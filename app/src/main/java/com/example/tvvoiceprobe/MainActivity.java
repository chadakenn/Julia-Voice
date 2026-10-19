package com.example.tvvoiceprobe;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

/** Diagnostic only: key events, default-assistant eligibility, audio inputs, audio level. */
public class MainActivity extends Activity {
    private static final int REQUEST_AUDIO = 41;
    private TextView output;
    private ScrollView scroll;
    private volatile boolean probing = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);
        layout.setBackgroundColor(0xff142031);
        TextView heading = new TextView(this);
        heading.setText("TV Voice Probe  •  onn / Google TV");
        heading.setTextSize(23);
        heading.setTextColor(0xffffffff);
        layout.addView(heading);
        addButton(layout, "List audio inputs", v -> listInputs());
        addButton(layout, "Check / request default assistant", v -> requestAssistant());
        addButton(layout, "Record microphone LEVEL for 10 seconds", v -> requestAudio());
        addButton(layout, "Clear log", v -> output.setText(""));
        scroll = new ScrollView(this);
        output = new TextView(this);
        output.setTextColor(0xffd9f1ff);
        output.setTextSize(16);
        output.setTextIsSelectable(true);
        scroll.addView(output);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        log("Press remote keys while this screen is focused. Try mic key; it may be intercepted by firmware.");
        handleAssistIntent(getIntent());
        listInputs();
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(click);
        parent.addView(b);
    }

    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAssistIntent(intent);
    }

    private void handleAssistIntent(Intent intent) {
        if (intent != null && Intent.ACTION_ASSIST.equals(intent.getAction())) {
            log("ASSIST intent received! The OS routed assistant invocation to this app.");
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            log("Key: " + KeyEvent.keyCodeToString(event.getKeyCode()) +
                " (" + event.getKeyCode() + ") inputDevice=" + event.getDeviceId());
        }
        return super.dispatchKeyEvent(event);
    }

    private void requestAssistant() {
        if (Build.VERSION.SDK_INT < 29) {
            log("Assistant RoleManager needs Android 10+.");
            return;
        }
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
            log("Default-assistant role is NOT available on this firmware.");
            return;
        }
        if (rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            log("This app already holds the assistant role.");
        } else {
            log("Requesting assistant role. Android must approve; Google TV may block or ignore it.");
            try { startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), 42); }
            catch (Exception e) { log("Role request failed: " + e.getMessage()); }
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 42) log("Assistant role request result = " + resultCode + " (-1 = accepted)");
    }

    private void listInputs() {
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio == null) { log("AudioManager unavailable"); return; }
        AudioDeviceInfo[] inputs = audio.getDevices(AudioManager.GET_DEVICES_INPUTS);
        log("Audio input devices = " + inputs.length);
        for (AudioDeviceInfo d : inputs) {
            log("  id=" + d.getId() + " type=" + d.getType() + " name=" + d.getProductName() +
                " sampleRates=" + Arrays.toString(d.getSampleRates()));
        }
        log("A Bluetooth remote may use vendor GATT instead of appearing in this list.");
    }

    private void requestAudio() {
        if (probing) { log("Mic test already running"); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        } else { micTest(); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) micTest();
        else if (requestCode == REQUEST_AUDIO) log("Microphone permission denied.");
    }

    private void micTest() {
        probing = true;
        new Thread(() -> {
            AudioRecord rec = null;
            try {
                int rate = 16000;
                int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (min <= 0) {
                    rate = 48000;
                    min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                }
                if (min <= 0) throw new IllegalStateException("No usable mono PCM capture format");
                rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min * 2, 8192));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("AudioRecord initialization failed");
                rec.startRecording();
                log("Mic level test running (" + rate + " Hz). Press and HOLD remote mic button, then speak.");
                AudioDeviceInfo routed = rec.getRoutedDevice();
                log("AudioRecord routed input=" + (routed == null ? "unknown" : routed.getProductName() + " (type " + routed.getType() + ")"));
                short[] samples = new short[2048];
                long until = SystemClock.elapsedRealtime() + 10000;
                long lastLog = 0;
                while (SystemClock.elapsedRealtime() < until) {
                    int read = rec.read(samples, 0, samples.length);
                    if (read <= 0) { log("Capture read error: " + read); break; }
                    long squareSum = 0;
                    int peak = 0;
                    for (int i = 0; i < read; i++) {
                        int x = samples[i];
                        peak = Math.max(peak, Math.abs(x));
                        squareSum += (long)x * x;
                    }
                    if (SystemClock.elapsedRealtime() - lastLog > 400) {
                        lastLog = SystemClock.elapsedRealtime();
                        double rms = Math.sqrt(squareSum / (double)read) / 32768;
                        log(String.format(Locale.US, "Mic peak=%d /32768  RMS=%.3f", peak, rms));
                    }
                }
            } catch (Exception e) { log("Mic test failed: " + e); }
            finally {
                if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} rec.release(); }
                probing = false;
                log("Mic test ended. Audio was NOT stored or sent anywhere.");
            }
        }, "tv-voice-probe").start();
    }

    private void log(String message) {
        runOnUiThread(() -> {
            if (output == null) return;
            String old = output.getText().toString();
            if (old.length() > 10000) old = old.substring(old.length() - 7000);
            output.setText(old + "\n" + message);
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
    }
}
