package com.example.tvvoiceprobe;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

/**
 * v0.5 opt-in Home Assistant voice overlay. Starts from a visible activity;
 * no always-on microphone or wake word. Recording is explicitly tied to a
 * user-initiated overlay launch / TALK, with STOP and watchdog escape hatches.
 */
public final class VoiceOverlayService extends Service implements HaAssistClient.Events {
    public static final String ACTION_CLOSE = "com.example.tvvoiceprobe.CLOSE_OVERLAY";
    private static final String CHANNEL_ID = "ha_tv_voice_overlay";
    private static final int NOTIFICATION_ID = 2217;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Button> actions = new ArrayList<>();
    private final Runnable endListeningTimeout = () -> {
        if (listening && assist != null) {
            listening = false;
            assist.finishTalking();
            if (wave != null) wave.setMode(VoiceWaveView.THINKING);
            status("Listening time limit reached. Processing...");
        }
    };
    private final Runnable dismissAfterReply = this::stopSelf;
    private WindowManager windowManager;
    private VoicePanel panel;
    private TextView statusView;
    private TextView answerView;
    private TextView transcriptView;
    private VoiceWaveView wave;
    private HaAssistClient assist;
    private CredentialStore.Connection connection;
    private OverlayPreferences preferences;
    private MediaPlayer player;
    private int selected = 0;
    private boolean listening = false;
    private boolean firstAutoListenPending = false;
    private boolean firstAutoListenStarted = false;
    private boolean closing = false;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CLOSE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (panel != null) {
            main.removeCallbacks(dismissAfterReply);
            panel.post(() -> {
                panel.requestFocus();
                focus(0);
                // Pressing the shortcut again may start the next question,
                // but never restarts the mic while an utterance is underway.
                if (!listening && assist != null && assist.isConnected()
                        && preferences != null && preferences.autoListen()) {
                    startListening();
                }
            });
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this) ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        closing = false;
        preferences = new OverlayPreferences(this);
        firstAutoListenPending = preferences.autoListen();
        firstAutoListenStarted = false;
        try {
            createNotificationChannel();
            Intent openApp = new Intent(this, VoiceActivity.class);
            openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPending = PendingIntent.getActivity(this, 0, openApp,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Intent close = new Intent(this, VoiceOverlayService.class);
            close.setAction(ACTION_CLOSE);
            PendingIntent closePending = PendingIntent.getService(this, 1, close,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentTitle("HA TV Voice")
                    .setContentText("Overlay open. Microphone is active only during a voice request.")
                    .setContentIntent(openPending)
                    .addAction(new Notification.Action.Builder(
                            android.R.drawable.ic_menu_close_clear_cancel, "Close", closePending)
                            .build())
                    .setOngoing(true)
                    .build();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

            connection = new CredentialStore(this).load();
            if (connection == null) {
                stopSelf();
                return START_NOT_STICKY;
            }
            showPanel();
            assist = new HaAssistClient(this, this);
            assist.connect(connection.url, connection.token);
        } catch (Exception ignored) {
            // Do not print private tokens or HA URL into logs.
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Home Assistant TV Voice", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible only while the voice overlay is open");
            manager.createNotificationChannel(channel);
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView label(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(dp(2), dp(3), dp(2), dp(3));
        t.setFocusable(false);
        return t;
    }

    private void showPanel() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) throw new IllegalStateException("No window manager");
        actions.clear();
        selected = 0;
        boolean glass = OverlayPreferences.SKIN_GLASS.equals(preferences.skin());

        panel = new VoicePanel();
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(14), dp(22), dp(14));
        panel.setFocusable(true);
        panel.setFocusableInTouchMode(true);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                glass ? new int[] {0xe81c1739, 0xf0141c37, 0xed0a1b30}
                        : new int[] {0xf01b3046, 0xf0122639});
        background.setCornerRadius(dp(glass ? 22 : 14));
        background.setStroke(dp(1), glass ? 0x887965ff : 0xff25b5db);
        panel.setBackground(background);
        panel.setElevation(dp(14));

        TextView top = label(glass ? "✦  HOME ASSISTANT" : "🎙  HOME ASSISTANT TV VOICE",
                glass ? 15 : 19, glass ? 0xffc9ccff : 0xff5de1ec);
        top.setLetterSpacing(0.12f);
        panel.addView(top);

        statusView = label("CONNECTING...", glass ? 14 : 15, 0xffe6e9ff);
        panel.addView(statusView);

        wave = new VoiceWaveView(this);
        wave.setMode(VoiceWaveView.CONNECTING);
        panel.addView(wave, new LinearLayout.LayoutParams(-1, dp(glass ? 88 : 68)));

        if (glass) panel.addView(label("YOU SAID", 11, 0xffaeb7d9));
        transcriptView = label("Say something when listening starts...",
                glass ? 17 : 16, glass ? 0xffe0e0fc : 0xffc6e8fc);
        transcriptView.setMaxLines(2);
        panel.addView(transcriptView);

        panel.addView(label("HOME ASSISTANT SAYS", 11, glass ? 0xffaeb7d9 : 0xff8bb0ca));
        answerView = label("Ready for your question",
                glass ? 23 : 19, 0xffffffff);
        answerView.setMaxLines(4);
        panel.addView(answerView);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        addAction(toolbar, "TALK", this::startListening);
        addAction(toolbar, "STOP", this::stopListening);
        addAction(toolbar, "CLOSE", this::stopSelf);
        panel.addView(toolbar);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int width = Math.max(dp(300), Math.min(dp(glass ? 720 : 550), screenWidth - dp(28)));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = glass ? (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)
                : (Gravity.TOP | Gravity.END);
        params.y = dp(glass ? 42 : 45);
        if (!glass) params.x = dp(18);
        params.setTitle("Home Assistant TV Voice");
        windowManager.addView(panel, params);
        panel.post(() -> focus(0));
    }

    private void addAction(LinearLayout parent, String name, Runnable runnable) {
        final int index = actions.size();
        Button button = new Button(this);
        button.setText(name);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(0xffffffff);
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setBackgroundTintList(ColorStateList.valueOf(0xff3a3e61));
        button.setOnClickListener(v -> runnable.run());
        button.setOnFocusChangeListener((v, focused) -> {
            if (focused) selected = index;
            button.setBackgroundTintList(ColorStateList.valueOf(
                    focused ? 0xff5b51b5 : 0xff3a3e61));
        });
        actions.add(button);
        parent.addView(button, new LinearLayout.LayoutParams(0, dp(42), 1f));
    }

    private void focus(int index) {
        if (actions.isEmpty()) return;
        selected = Math.max(0, Math.min(index, actions.size() - 1));
        actions.get(selected).requestFocus();
    }

    private void startListening() {
        if (closing) return;
        if (assist == null || !assist.isConnected()) {
            status("Connecting to Home Assistant. Try again shortly.");
            return;
        }
        if (listening) return;
        firstAutoListenPending = false;
        main.removeCallbacks(dismissAfterReply);
        main.removeCallbacks(endListeningTimeout);
        if (player != null) {
            player.release();
            player = null;
        }
        listening = true;
        transcriptView.setText("Listening to your voice...");
        answerView.setText("...");
        if (wave != null) wave.setMode(VoiceWaveView.LISTENING);
        status("LISTENING • Speak toward the onn box");
        assist.talk(preferences.autoListen());
    }

    private void stopListening() {
        if (assist == null) return;
        listening = false;
        main.removeCallbacks(endListeningTimeout);
        if (wave != null) wave.setMode(VoiceWaveView.THINKING);
        assist.finishTalking();
    }

    private final class VoicePanel extends LinearLayout {
        VoicePanel() { super(VoiceOverlayService.this); }
        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            int code = event.getKeyCode();
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN &&
                    event.getRepeatCount() == 0;
            switch (code) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (down) focus(selected - 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (down) focus(selected + 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (down && !actions.isEmpty()) actions.get(selected).performClick();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    if (down) stopSelf();
                    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }
        }
    }

    @Override public void ready() {
        main.post(() -> {
            if (closing) return;
            if (wave != null) wave.setMode(VoiceWaveView.IDLE);
            if (firstAutoListenPending && !firstAutoListenStarted) {
                firstAutoListenStarted = true;
                startListening();
            } else {
                status("Connected. Select TALK.");
            }
        });
    }

    @Override public void listening() {
        main.post(() -> {
            if (closing) return;
            listening = true;
            if (wave != null) wave.setMode(VoiceWaveView.LISTENING);
            main.removeCallbacks(endListeningTimeout);
            main.postDelayed(endListeningTimeout, 23000);
        });
    }

    @Override public void processing() {
        main.post(() -> {
            listening = false;
            main.removeCallbacks(endListeningTimeout);
            if (wave != null) wave.setMode(VoiceWaveView.THINKING);
        });
    }

    @Override public void status(String message) {
        main.post(() -> {
            if (closing || statusView == null) return;
            statusView.setText(message);
            if (message.contains("Ready for another question")) {
                if (wave != null) wave.setMode(VoiceWaveView.IDLE);
                scheduleFallbackDismiss();
            } else if (message.startsWith("Speaking...")) {
                main.removeCallbacks(dismissAfterReply);
                if (wave != null) wave.setMode(VoiceWaveView.SPEAKING);
            } else if (message.contains("Voice pause detected")) {
                listening = false;
                main.removeCallbacks(endListeningTimeout);
                if (wave != null) wave.setMode(VoiceWaveView.THINKING);
            }
        });
    }

    @Override public void transcript(String value) {
        main.post(() -> {
            if (!closing && transcriptView != null) transcriptView.setText(value);
        });
    }

    @Override public void answer(String value) {
        main.post(() -> {
            if (!closing && answerView != null) answerView.setText(value);
        });
    }

    @Override public void ttsUrl(String url) {
        main.post(() -> {
            main.removeCallbacks(dismissAfterReply);
            playTts(url);
        });
    }

    private void scheduleFallbackDismiss() {
        if (preferences != null && preferences.autoHide()) {
            main.removeCallbacks(dismissAfterReply);
            main.postDelayed(dismissAfterReply, 25000);
        }
    }

    private void playTts(String address) {
        if (connection == null || closing) return;
        try {
            HttpUrl base = HttpUrl.parse(connection.url);
            if (base == null) throw new IllegalStateException("Bad HA address");
            HttpUrl playback = base.resolve(address);
            if (playback == null) throw new IllegalArgumentException("Invalid speech media path");
            if (!playback.host().equals(base.host()) || playback.port() != base.port()
                    || !playback.scheme().equals(base.scheme())) {
                if (!playback.encodedPath().startsWith("/api/tts_proxy/")) {
                    throw new SecurityException("Unexpected TTS server");
                }
                playback = base.newBuilder().encodedPath(playback.encodedPath())
                        .encodedQuery(playback.encodedQuery()).build();
            }
            if (player != null) player.release();
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            Map<String,String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + connection.token);
            player.setDataSource(this, Uri.parse(playback.toString()), headers);
            player.setOnPreparedListener(p -> {
                status("SPEAKING • Home Assistant is answering");
                if (wave != null) wave.setMode(VoiceWaveView.SPEAKING);
                p.start();
            });
            player.setOnCompletionListener(p -> {
                status("Done. Press ⭐ again or choose TALK.");
                if (wave != null) wave.setMode(VoiceWaveView.IDLE);
                if (preferences.autoHide()) {
                    main.removeCallbacks(dismissAfterReply);
                    main.postDelayed(dismissAfterReply, 6500);
                }
            });
            player.setOnErrorListener((p, what, extra) -> {
                status("Speech playback failed; answer is still shown.");
                scheduleFallbackDismiss();
                return true;
            });
            player.prepareAsync();
        } catch (Exception ignored) {
            status("Could not play speech. The answer is still shown.");
            scheduleFallbackDismiss();
        }
    }

    @Override public void onDestroy() {
        closing = true;
        main.removeCallbacksAndMessages(null);
        if (assist != null) { assist.shutdown(); assist = null; }
        if (player != null) { player.release(); player = null; }
        if (windowManager != null && panel != null) {
            try { windowManager.removeViewImmediate(panel); }
            catch (Exception ignored) { }
        }
        panel = null;
        wave = null;
        answerView = null;
        transcriptView = null;
        statusView = null;
        connection = null;
        actions.clear();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
