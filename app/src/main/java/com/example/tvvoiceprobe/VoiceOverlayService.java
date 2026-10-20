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
 * Explicitly started, on-screen Google TV voice overlay prototype.
 *
 * A foreground microphone service is required by recent Android versions when
 * recording continues while another app is visible. It starts ONLY after the
 * user chooses the overlay from our visible Activity, and lives ONLY while the
 * floating panel is open. No wake word or idle microphone capture.
 */
public final class VoiceOverlayService extends Service implements HaAssistClient.Events {
    public static final String ACTION_CLOSE = "com.example.tvvoiceprobe.CLOSE_OVERLAY";
    private static final String CHANNEL_ID = "ha_tv_voice_overlay";
    private static final int NOTIFICATION_ID = 2217;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Button> actions = new ArrayList<>();
    private WindowManager windowManager;
    private VoicePanel panel;
    private TextView statusView;
    private TextView answerView;
    private TextView transcriptView;
    private HaAssistClient assist;
    private CredentialStore.Connection connection;
    private MediaPlayer player;
    private int selected = 0;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CLOSE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (panel != null) {
            panel.post(() -> { focus(0); panel.requestFocus(); });
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Promote while the app is foreground, before the Activity returns to
        // the previous TV app. This is NOT an always-on microphone service.
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
                    .setContentTitle("Home Assistant voice overlay")
                    .setContentText("Overlay active. Microphone captures audio only after TALK.")
                    .setContentIntent(openPending)
                    .addAction(new Notification.Action.Builder(
                            android.R.drawable.ic_menu_close_clear_cancel,
                            "Close overlay", closePending).build())
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
        } catch (Exception | SecurityException exception) {
            // No user secrets or internal connection details in logs.
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        "Home Assistant TV voice overlay", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Visible only while the voice overlay is enabled");
                manager.createNotificationChannel(channel);
            }
        }
    }

    private int dp(int amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setPadding(dp(2), dp(3), dp(2), dp(3));
        view.setFocusable(false);
        return view;
    }

    private void showPanel() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) throw new IllegalStateException("No overlay window manager");
        actions.clear();
        selected = 0;

        panel = new VoicePanel();
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        panel.setFocusable(true);
        panel.setFocusableInTouchMode(true);
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(0xee152638);
        surface.setCornerRadius(dp(15));
        surface.setStroke(dp(1), 0xff25b5db);
        panel.setBackground(surface);
        panel.setElevation(dp(12));

        panel.addView(text("🎙  HOME ASSISTANT TV VOICE", 19, 0xff5de1ec));
        statusView = text("Connecting to Home Assistant...", 15, 0xffecf1f9);
        panel.addView(statusView);
        panel.addView(text("HOME ASSISTANT SAYS", 12, 0xff8bb0ca));
        answerView = text("Ready for your question", 19, 0xffffffff);
        answerView.setMaxLines(4);
        panel.addView(answerView);
        panel.addView(text("YOU SAID", 12, 0xff8bb0ca));
        transcriptView = text("Nothing yet", 16, 0xffc6e8fc);
        transcriptView.setMaxLines(3);
        panel.addView(transcriptView);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        addAction(toolbar, "TALK", () -> {
            if (assist == null || !assist.isConnected()) {
                status("Still connecting. Try again shortly.");
                return;
            }
            transcriptView.setText("Listening...");
            answerView.setText("Waiting for Home Assistant...");
            assist.talk();
        });
        addAction(toolbar, "STOP", () -> {
            if (assist != null) assist.finishTalking();
        });
        addAction(toolbar, "CLOSE", this::stopSelf);
        panel.addView(toolbar);

        // A focusable TYPE_APPLICATION_OVERLAY is deliberate: while open, the
        // panel receives D-pad input. CLOSE/BACK returns control to the TV app.
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                Math.min(dp(540), getResources().getDisplayMetrics().widthPixels - dp(20)),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(18);
        params.y = dp(48);
        params.setTitle("HA TV Voice Overlay");
        windowManager.addView(panel, params);
        panel.post(() -> focus(0));
    }

    private void addAction(LinearLayout parent, String label, Runnable click) {
        Button button = new Button(this);
        int index = actions.size();
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(0xffffffff);
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setBackgroundTintList(ColorStateList.valueOf(0xff3f596f));
        button.setOnClickListener(v -> click.run());
        button.setOnFocusChangeListener((v, focused) -> {
            if (focused) selected = index;
            button.setBackgroundTintList(ColorStateList.valueOf(
                    focused ? 0xff058db7 : 0xff3f596f));
        });
        actions.add(button);
        parent.addView(button, new LinearLayout.LayoutParams(0, dp(54), 1f));
    }

    private void focus(int index) {
        if (actions.isEmpty()) return;
        selected = Math.max(0, Math.min(index, actions.size() - 1));
        actions.get(selected).requestFocus();
    }

    private final class VoicePanel extends LinearLayout {
        VoicePanel() { super(VoiceOverlayService.this); }

        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            int key = event.getKeyCode();
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN &&
                    event.getRepeatCount() == 0;
            switch (key) {
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

    @Override public void status(String message) {
        main.post(() -> { if (statusView != null) statusView.setText(message); });
    }

    @Override public void transcript(String text) {
        main.post(() -> { if (transcriptView != null) transcriptView.setText(text); });
    }

    @Override public void answer(String text) {
        main.post(() -> { if (answerView != null) answerView.setText(text); });
    }

    @Override public void ttsUrl(String url) {
        main.post(() -> playTts(url));
    }

    private void playTts(String address) {
        if (connection == null) return;
        try {
            HttpUrl base = HttpUrl.parse(connection.url);
            if (base == null) throw new IllegalStateException("Invalid Home Assistant URL");
            HttpUrl playback = base.resolve(address);
            if (playback == null) throw new IllegalArgumentException("Invalid TTS path");
            if (!playback.host().equals(base.host()) || playback.port() != base.port()
                    || !playback.scheme().equals(base.scheme())) {
                if (!playback.encodedPath().startsWith("/api/tts_proxy/"))
                    throw new SecurityException("Unexpected TTS source");
                playback = base.newBuilder().encodedPath(playback.encodedPath())
                        .encodedQuery(playback.encodedQuery()).build();
            }
            if (player != null) { player.release(); player = null; }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + connection.token);
            player.setDataSource(this, Uri.parse(playback.toString()), headers);
            player.setOnPreparedListener(p -> { status("Speaking..."); p.start(); });
            player.setOnCompletionListener(p -> status("Finished. Press TALK for another question."));
            player.setOnErrorListener((p, what, extra) -> {
                status("TTS playback failed. You can still read the answer.");
                return true;
            });
            player.prepareAsync();
        } catch (Exception error) {
            status("Unable to play TTS. Text answer is shown above.");
        }
    }

    @Override public void onDestroy() {
        if (assist != null) { assist.shutdown(); assist = null; }
        if (player != null) { player.release(); player = null; }
        if (windowManager != null && panel != null) {
            try { windowManager.removeViewImmediate(panel); }
            catch (Exception ignored) { }
        }
        panel = null;
        statusView = null;
        answerView = null;
        transcriptView = null;
        connection = null;
        actions.clear();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
