package com.example.tvvoiceprobe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Opt-in, microphone-free foreground listener for two authenticated Home
 * Assistant events. Popup windows are non-focusable and never intercept the
 * remote or take audio focus away from a video.
 *
 * Android can still stop this service, so it reconnects while permitted but
 * does not promise true 24/7 delivery or auto-start itself at boot.
 */
public final class TvEventsService extends Service {
    public static final String ACTION_START = "com.example.tvvoiceprobe.TV_EVENTS_START";
    public static final String ACTION_STOP = "com.example.tvvoiceprobe.TV_EVENTS_STOP";
    public static final String ACTION_PREVIEW = "com.example.tvvoiceprobe.TV_EVENTS_PREVIEW";

    private static final String CHANNEL = "kellie_tv_events";
    private static final int NOTICE_ID = 2242;
    private static final int ANNOUNCE_ID = 1;
    private static final int CAMERA_ID = 2;
    private static final int MAX_SNAPSHOT_BYTES = 5 * 1024 * 1024;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .pingInterval(25, TimeUnit.SECONDS)
            .build();
    private final Runnable reconnect = this::connect;
    private final Runnable dismiss = this::hideCard;

    private WindowManager windowManager;
    private View card;
    private ImageView photo;
    private WebSocket socket;
    private CredentialStore.Connection credentials;
    private boolean stopping;
    private int failedAttempts;
    private int generation;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            new OverlayPreferences(this).setTvEvents(false);
            stopSelf();
            return START_NOT_STICKY;
        }
        // A user must first enable this in the app, and Android requires the
        // system "Display over other apps" permission for passive popups.
        if (!new OverlayPreferences(this).tvEvents() || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (windowManager == null) windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);
        if (credentials == null) {
            try { credentials = new CredentialStore(this).load(); }
            catch (Exception ignored) { credentials = null; }
        }
        if (credentials == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            createChannel();
            Notification notification = buildNotification("TV alerts enabled");
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTICE_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTICE_ID, notification);
            }
        } catch (RuntimeException ignored) {
            stopSelf();
            return START_NOT_STICKY;
        }
        stopping = false;
        if (ACTION_PREVIEW.equals(action)) {
            showCard("Kellie's Voice", "This is how announcements will appear over your TV.", null, 10);
        }
        if (socket == null) connect();
        return START_STICKY;
    }

    private void createChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Kellie's Voice TV alerts", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Opt-in Home Assistant announcements and camera popups");
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification(String message) {
        Intent open = new Intent(this, VoiceActivity.class)
                .setAction("com.example.tvvoiceprobe.OPEN_SETTINGS");
        PendingIntent openPending = PendingIntent.getActivity(this, 30, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, TvEventsService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 31, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Kellie's Voice TV notifications")
                .setContentText(message)
                .setContentIntent(openPending)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Disable alerts", stopPending).build())
                .setOngoing(true)
                .build();
    }

    private void connectionNotice(String message) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && !stopping) manager.notify(NOTICE_ID, buildNotification(message));
    }

    private void connect() {
        main.removeCallbacks(reconnect);
        if (stopping || socket != null || !new OverlayPreferences(this).tvEvents()
                || credentials == null) return;
        HttpUrl base = HttpUrl.parse(credentials.url);
        if (base == null || !(base.scheme().equals("http") || base.scheme().equals("https"))
                || !base.username().isEmpty() || !base.password().isEmpty()) {
            connectionNotice("Check the Home Assistant connection in Settings");
            return;
        }
        // OkHttp upgrades an HTTP(S) URL to WebSocket internally. HttpUrl
        // itself only accepts the http/https schemes.
        HttpUrl address = base.newBuilder()
                .addPathSegments("api/websocket").build();
        socket = http.newWebSocket(new Request.Builder().url(address).build(),
                new WebSocketListener() {
                    @Override public void onMessage(WebSocket ws, String text) {
                        main.post(() -> handleMessage(ws, text));
                    }
                    @Override public void onFailure(WebSocket ws, Throwable problem,
                            Response response) {
                        main.post(() -> lostConnection(ws));
                    }
                    @Override public void onClosed(WebSocket ws, int code, String reason) {
                        main.post(() -> lostConnection(ws));
                    }
                });
    }

    private void lostConnection(WebSocket ws) {
        if (ws != socket) return;
        socket = null;
        if (stopping) return;
        connectionNotice("Reconnecting to Home Assistant");
        long delay = Math.min(30000L, 2000L << Math.min(4, failedAttempts++));
        main.removeCallbacks(reconnect);
        main.postDelayed(reconnect, delay);
    }

    private void handleMessage(WebSocket ws, String json) {
        if (stopping || ws != socket) return;
        try {
            JSONObject message = new JSONObject(json);
            String type = message.optString("type");
            if ("auth_required".equals(type)) {
                ws.send(new JSONObject().put("type", "auth")
                        .put("access_token", credentials.token).toString());
                return;
            }
            if ("auth_invalid".equals(type)) {
                connectionNotice("Home Assistant rejected the saved login");
                ws.close(1000, "Authentication rejected");
                return;
            }
            if ("auth_ok".equals(type)) {
                failedAttempts = 0;
                ws.send(new JSONObject().put("id", ANNOUNCE_ID)
                        .put("type", "subscribe_events")
                        .put("event_type", TvEventPayload.ANNOUNCEMENT).toString());
                ws.send(new JSONObject().put("id", CAMERA_ID)
                        .put("type", "subscribe_events")
                        .put("event_type", TvEventPayload.CAMERA_POPUP).toString());
                connectionNotice("Connected, waiting for TV alerts");
                return;
            }
            if ("result".equals(type) && !message.optBoolean("success", false)
                    && (message.optInt("id") == ANNOUNCE_ID
                            || message.optInt("id") == CAMERA_ID)) {
                connectionNotice("Event subscriptions rejected; check HA permissions");
                return;
            }
            if (!"event".equals(type)) return;
            int subscription = message.optInt("id", -1);
            JSONObject event = message.optJSONObject("event");
            if (event == null) return;
            String eventType = event.optString("event_type");
            JSONObject data = event.optJSONObject("data");
            if (data == null) return;
            if (!TvEventPayload.addressedTo(data.optString("device_id", ""),
                    new OverlayPreferences(this).deviceId())) return;
            int duration = TvEventPayload.duration(data.optInt("duration_seconds", 10));
            if (subscription == ANNOUNCE_ID
                    && TvEventPayload.ANNOUNCEMENT.equals(eventType)) {
                String body = TvEventPayload.cleanText(data.optString("text"), 500);
                if (!body.isEmpty()) showCard(
                        TvEventPayload.cleanText(data.optString("title", "Kellie's Voice"), 65),
                        body, null, duration);
            } else if (subscription == CAMERA_ID
                    && TvEventPayload.CAMERA_POPUP.equals(eventType)) {
                String entity = TvEventPayload.cameraEntity(data.optString("entity_id"));
                if (entity != null) showCard(
                        TvEventPayload.cleanText(data.optString("title", "Camera alert"), 65),
                        "Home Assistant camera snapshot", entity, duration);
            }
        } catch (Exception ignored) {
            // Never print auth tokens, URLs, or untrusted event payloads.
        }
    }

    private int dp(int pixels) {
        return Math.round(pixels * getResources().getDisplayMetrics().density);
    }

    private TextView label(String value, int points, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(points);
        v.setTextColor(color);
        v.setPadding(dp(3), dp(5), dp(3), dp(5));
        v.setMaxLines(8);
        return v;
    }

    private void showCard(String title, String text, String entity, int seconds) {
        if (stopping || windowManager == null || !Settings.canDrawOverlays(this)) return;
        hideCard();
        int current = ++generation;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(13), dp(18), dp(14));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {0xf51B1512, 0xf52B1D17});
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), 0xffAD835D);
        panel.setBackground(background);
        panel.setElevation(dp(12));

        View goldRule = new View(this);
        goldRule.setBackgroundColor(0xffE1B27B);
        panel.addView(goldRule, new LinearLayout.LayoutParams(-1, dp(3)));
        panel.addView(label(title.isEmpty() ? "Kellie's Voice" : title, 19, 0xffEAC296));
        panel.addView(label(text, 16, 0xffFAE8D5));
        photo = null;
        if (entity != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xff2D211C);
            panel.addView(image, new LinearLayout.LayoutParams(-1, dp(220)));
            photo = image;
        }

        int screen = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(dp(entity == null ? 430 : 505), screen - dp(48));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(28);
        params.y = dp(40);
        params.setTitle("Kellie's Voice TV alert");
        try {
            windowManager.addView(panel, params);
            card = panel;
            if (entity != null) fetchSnapshot(entity, photo, current);
            main.postDelayed(dismiss, TvEventPayload.duration(seconds) * 1000L);
        } catch (RuntimeException ignored) {
            card = null;
            photo = null;
        }
    }

    private void fetchSnapshot(String entity, ImageView target, int current) {
        if (credentials == null) return;
        HttpUrl base = HttpUrl.parse(credentials.url);
        if (base == null) return;
        // The event may only select a validated camera entity. It may NOT
        // supply a URL, hostname, protocol, file path, or bearer token.
        HttpUrl url = base.newBuilder().addPathSegment("api")
                .addPathSegment("camera_proxy").addPathSegment(entity).build();
        Request request = new Request.Builder().url(url)
                .header("Authorization", "Bearer " + credentials.token)
                .header("Cache-Control", "no-cache").build();
        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { }
            @Override public void onResponse(Call call, Response response) {
                try (Response result = response) {
                    if (!result.isSuccessful() || result.body() == null) return;
                    long declared = result.body().contentLength();
                    if (declared > MAX_SNAPSHOT_BYTES) return;
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (InputStream input = result.body().byteStream()) {
                        byte[] buf = new byte[8192];
                        int read;
                        while ((read = input.read(buf)) >= 0) {
                            if (bytes.size() + read > MAX_SNAPSHOT_BYTES) return;
                            bytes.write(buf, 0, read);
                        }
                    }
                    byte[] raw = bytes.toByteArray();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
                    int largest = Math.max(options.outWidth, options.outHeight);
                    options.inSampleSize = largest > 1920 ? 4 : largest > 960 ? 2 : 1;
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
                    if (bitmap != null) main.post(() -> {
                        if (!stopping && generation == current && photo == target) {
                            target.setImageBitmap(bitmap);
                        } else bitmap.recycle();
                    });
                } catch (Exception ignored) { }
            }
        });
    }

    private void hideCard() {
        main.removeCallbacks(dismiss);
        ++generation;
        if (windowManager != null && card != null) {
            try { windowManager.removeView(card); } catch (RuntimeException ignored) { }
        }
        card = null;
        photo = null;
    }

    @Override public void onDestroy() {
        stopping = true;
        main.removeCallbacksAndMessages(null);
        hideCard();
        if (socket != null) {
            socket.close(1000, "Receiver stopped");
            socket = null;
        }
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
        credentials = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
