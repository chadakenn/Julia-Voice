package com.example.tvvoiceprobe;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.homeassistant.companion.android.microwakeword.MicroWakeWord;

/**
 * Opt-in on-device "Okay Nabu" listener. It starts only from the settings
 * screen, never from boot or an app update, and it releases the microphone
 * before the voice overlay opens. Audio stays on the TV until that overlay
 * talks to Home Assistant.
 */
public final class WakeWordService extends Service {
    public static final String ACTION_START = WakeWordSession.ACTION_START;
    public static final String ACTION_STOP = WakeWordSession.ACTION_STOP;
    private static final String CHANNEL_ID = "ha_tv_wake_word";
    private static final int NOTICE_ID = 2251;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHUNK = 160;

    private final Object micLock = new Object();
    private volatile boolean running;
    private Thread worker;
    private AudioRecord recorder;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action) || !allowed(action)) {
            running = false;
            releaseRecorder();
            stopSelf();
            return START_NOT_STICKY;
        }
        Notification notice = buildNotice();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTICE_ID, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTICE_ID, notice);
        }
        if (worker == null || !worker.isAlive()) {
            running = true;
            worker = new Thread(this::listen, "kellie-wake-word");
            worker.start();
        }
        return START_NOT_STICKY;
    }

    private boolean allowed(String action) {
        OverlayPreferences prefs = new OverlayPreferences(this);
        return WakeWordSession.mayStart(prefs.wakeWord(), action,
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED,
                android.provider.Settings.canDrawOverlays(this));
    }

    private void listen() {
        MicroWakeWord engine = null;
        try {
            WakeWordConfig config = WakeWordConfig.parse(readAsset("wakeword/okay_nabu.json"));
            engine = new MicroWakeWord(loadModel("wakeword/" + config.model),
                    config.featureStepSizeMs, config.probabilityCutoff, config.slidingWindowSize);
            WakeWordSession session = new WakeWordSession();
            short[] chunk = new short[CHUNK];
            while (running && new OverlayPreferences(this).wakeWord()) {
                if (!WakeWordSession.shouldCapture(true, VoiceOverlayService.isOpen())) {
                    releaseRecorder();
                    sleep(200);
                    continue;
                }
                int read;
                synchronized (micLock) {
                    if (!ensureRecorder()) {
                        notifyFailure("The microphone could not open at 16 kHz. Wake word is off.");
                        new OverlayPreferences(this).setWakeWord(false);
                        break;
                    }
                    read = recorder.read(chunk, 0, CHUNK);
                }
                if (read <= 0) continue;
                short[] samples = read == CHUNK ? chunk : java.util.Arrays.copyOf(chunk, read);
                if (!session.onChunk(engine.processAudio(samples))) continue;
                engine.reset();
                releaseRecorder();
                if (!openOverlay()) {
                    sleep(2000);
                    continue;
                }
                long deadline = android.os.SystemClock.elapsedRealtime() + 5000L;
                while (running && !VoiceOverlayService.isOpen()
                        && android.os.SystemClock.elapsedRealtime() < deadline) {
                    sleep(100);
                }
                while (running && VoiceOverlayService.isOpen()
                        && new OverlayPreferences(this).wakeWord()) {
                    sleep(200);
                }
            }
        } catch (Throwable ignored) {
            notifyFailure("Wake word stopped. Turn it on again from Settings.");
            new OverlayPreferences(this).setWakeWord(false);
        } finally {
            running = false;
            releaseRecorder();
            if (engine != null) engine.close();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private boolean openOverlay() {
        if (!new OverlayPreferences(this).wakeWord()) return false;
        if (VoiceOverlayService.isOpen()) return true;
        try {
            // The wake recorder was released immediately before this call.
            // Deliver a one-shot explicit detection, not a normal idle launch.
            startForegroundService(new Intent(this, VoiceOverlayService.class)
                    .setAction(WakeWordSession.ACTION_DETECTED));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean ensureRecorder() {
        synchronized (micLock) {
            if (!running) return false;
            if (recorder != null) return true;
            int minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) return false;
            AudioRecord created = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(minimum, CHUNK * 4));
            if (created.getState() != AudioRecord.STATE_INITIALIZED) {
                created.release();
                return false;
            }
            try {
                created.startRecording();
            } catch (RuntimeException error) {
                created.release();
                return false;
            }
            if (!running || created.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                created.release();
                return false;
            }
            recorder = created;
            return true;
        }
    }

    private void releaseRecorder() {
        AudioRecord held;
        synchronized (micLock) {
            held = recorder;
            recorder = null;
        }
        if (held == null) return;
        try {
            held.stop();
        } catch (RuntimeException ignored) {
        }
        held.release();
    }

    private String readAsset(String path) throws java.io.IOException {
        try (InputStream in = getAssets().open(path)) {
            return new String(readBounded(in, 16_384), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private ByteBuffer loadModel(String path) throws java.io.IOException {
        try (InputStream in = getAssets().open(path)) {
            byte[] bytes = readBounded(in, 2_000_000);
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.order(ByteOrder.nativeOrder());
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        }
    }

    private static byte[] readBounded(InputStream in, int cap) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > cap) throw new java.io.IOException("Wake word asset is too large");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private Notification buildNotice() {
        createChannel();
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Kellie's Voice wake word")
                .setContentText("Listening for Okay Nabu. The microphone is in use.")
                .setOngoing(true)
                .build();
    }

    private void notifyFailure(String message) {
        createChannel();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        manager.notify(NOTICE_ID, new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Kellie's Voice wake word")
                .setContentText(message)
                .setOngoing(false)
                .build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                "Kellie's Voice wake word", NotificationManager.IMPORTANCE_LOW));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override public void onDestroy() {
        running = false;
        releaseRecorder();
        super.onDestroy();
    }
}
