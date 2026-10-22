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
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import org.json.JSONArray;
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
 * Opt-in, microphone-free foreground listener for authenticated Home
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
    private static final int LIVE_EVENT_ID = 3;
    private static final int LAYOUT_EVENT_ID = 4;
    private static final int LAYOUT_STATES_ID = 5;
    private static final int HIDE_CAMERA_EVENT_ID = 6;
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
    private TextureView liveSurface;
    private boolean liveFirstFrame;
    private TextView liveStatus;
    private ExoPlayer livePlayer;
    private int requestCounter = 100;
    private int liveRequestId = -1;
    private int liveRequestGeneration = -1;
    private int layoutStatesId = -1;
    private WebSocket socket;
    private CredentialStore.Connection credentials;
    private boolean stopping;
    private boolean cameraVisible;
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
        CredentialStore.Connection saved;
        try { saved = new CredentialStore(this).load(); }
        catch (Exception ignored) { saved = null; }
        if (saved == null) {
            credentials = null;
            stopSelf();
            return START_NOT_STICKY;
        }
        if (credentials != null
                && (!credentials.url.equals(saved.url) || !credentials.token.equals(saved.token))) {
            main.removeCallbacks(reconnect);
            if (socket != null) {
                socket.close(1000, "Home Assistant login changed");
                socket = null;
            }
            failedAttempts = 0;
        }
        credentials = saved;
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
        layoutStatesId = -1;
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
                ws.send(new JSONObject().put("id", LIVE_EVENT_ID)
                        .put("type", "subscribe_events")
                        .put("event_type", TvEventPayload.LIVE_CAMERA).toString());
                ws.send(new JSONObject().put("id", HIDE_CAMERA_EVENT_ID)
                        .put("type", "subscribe_events")
                        .put("event_type", TvEventPayload.HIDE_CAMERA).toString());
                if (new OverlayPreferences(this).haManage()) {
                    layoutStatesId = LAYOUT_STATES_ID;
                    ws.send(new JSONObject().put("id", LAYOUT_EVENT_ID)
                            .put("type", "subscribe_events")
                            .put("event_type", "state_changed").toString());
                    ws.send(new JSONObject().put("id", LAYOUT_STATES_ID)
                            .put("type", "get_states").toString());
                }
                connectionNotice("Connected, waiting for TV alerts");
                return;
            }
            if ("result".equals(type)) {
                int resultId = message.optInt("id", -1);
                if (resultId == layoutStatesId) {
                    layoutStatesId = -1;
                    if (message.optBoolean("success", false)) {
                        JSONArray states = message.optJSONArray("result");
                        if (states != null) {
                            for (int i = 0; i < states.length(); i++) {
                                applyAlertState(states.optJSONObject(i));
                            }
                        }
                    }
                    return;
                }
                if (resultId == liveRequestId && liveRequestGeneration == generation) {
                    liveRequestId = -1;
                    if (message.optBoolean("success", false)) {
                        JSONObject result = message.optJSONObject("result");
                        String path = result == null ? "" : result.optString("url");
                        playLiveStream(path);
                    } else if (liveStatus != null) {
                        JSONObject err = message.optJSONObject("error");
                        String code = err == null ? "" : err.optString("code");
                        // Error codes only; never expose free-form HA errors, credentials, or URLs.
                        if (!code.matches("[a-z_]{1,40}")) code = "unknown";
                        liveStatus.setText("HA stream request failed (" + code
                                + "); showing camera snapshot.");
                    }
                    return;
                }
                if (!message.optBoolean("success", false)
                        && (resultId == ANNOUNCE_ID || resultId == CAMERA_ID
                                || resultId == LIVE_EVENT_ID
                                || resultId == HIDE_CAMERA_EVENT_ID)) {
                    connectionNotice("Event subscriptions rejected; check HA permissions");
                }
                return;
            }
            if (!"event".equals(type)) return;
            int subscription = message.optInt("id", -1);
            JSONObject event = message.optJSONObject("event");
            if (event == null) return;
            String eventType = event.optString("event_type");
            if (subscription == LAYOUT_EVENT_ID && "state_changed".equals(eventType)) {
                JSONObject changed = event.optJSONObject("data");
                if (changed != null) applyAlertState(changed.optJSONObject("new_state"));
                return;
            }
            JSONObject data = event.optJSONObject("data");
            if (data == null) return;
            if (!TvEventPayload.addressedTo(data.optString("device_id", ""),
                    new OverlayPreferences(this).deviceId())) return;
            // A close command never starts a new overlay or microphone.
            // Ignore it when a text announcement (or nothing) is visible.
            if (subscription == HIDE_CAMERA_EVENT_ID
                    && TvEventPayload.HIDE_CAMERA.equals(eventType)) {
                if (cameraVisible) hideCard();
                return;
            }
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
            } else if (subscription == LIVE_EVENT_ID
                    && TvEventPayload.LIVE_CAMERA.equals(eventType)) {
                String entity = TvEventPayload.cameraEntity(data.optString("entity_id"));
                if (entity != null) showCard(
                        TvEventPayload.cleanText(data.optString("title", "Live camera"), 65),
                        "LIVE · Starting muted video…", entity,
                        TvEventPayload.liveDuration(data.optInt("duration_seconds", 90)), true);
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
        showCard(title, text, entity, seconds, false);
    }

    private void showCard(String title, String text, String entity, int seconds, boolean live) {
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
        TextView subtitle = label(text, 16, 0xffFAE8D5);
        panel.addView(subtitle);
        photo = null;
        OverlayPreferences placement = new OverlayPreferences(this);
        String alertSize = placement.alertSize();
        if (entity != null && live) {
            liveStatus = subtitle;
            // SurfaceView can show black in TYPE_APPLICATION_OVERLAY on some TVs.
            // TextureView is composited with the rest of this popup window.
            // Keep a real camera still behind the player until the first video frame.
            FrameLayout preview = new FrameLayout(this);
            ImageView still = new ImageView(this);
            still.setScaleType(ImageView.ScaleType.CENTER_CROP);
            still.setBackgroundColor(0xff2D211C);
            preview.addView(still, new FrameLayout.LayoutParams(-1, -1));
            photo = still;
            TextureView surface = new TextureView(this);
            surface.setOpaque(false);
            surface.setAlpha(0f);
            surface.setKeepScreenOn(true);
            preview.addView(surface, new FrameLayout.LayoutParams(-1, -1));
            liveSurface = surface;
            liveFirstFrame = false;
            panel.addView(preview, new LinearLayout.LayoutParams(-1,
                    dp(TvAlertLayout.mediaHeightDp(alertSize, true))));
        } else if (entity != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xff2D211C);
            panel.addView(image, new LinearLayout.LayoutParams(-1,
                    dp(TvAlertLayout.mediaHeightDp(alertSize, false))));
            photo = image;
        }

        int screen = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(dp(TvAlertLayout.widthDp(alertSize, entity != null, live)), screen - dp(48));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        String alertPosition = placement.alertPosition();
        params.gravity = alertGravity(alertPosition);
        params.x = dp(TvAlertLayout.marginXDp(alertPosition));
        params.y = dp(TvAlertLayout.marginYDp(alertPosition));
        params.setTitle("Kellie's Voice TV alert");
        try {
            windowManager.addView(panel, params);
            card = panel;
            cameraVisible = entity != null;
            if (entity != null) {
                if (live) {
                    // This is explicitly a still-image fallback, never label it "LIVE".
                    fetchSnapshot(entity, photo, current);
                    requestLiveStream(entity, current);
                    main.postDelayed(() -> {
                        if (generation == current && !liveFirstFrame && liveStatus != null) {
                            liveStatus.setText("Live video taking longer than expected; "
                                    + "showing camera snapshot.");
                        }
                    }, 18000L);
                } else fetchSnapshot(entity, photo, current);
            }
            main.postDelayed(dismiss,
                    (live ? TvEventPayload.liveDuration(seconds)
                            : TvEventPayload.duration(seconds)) * 1000L);
        } catch (RuntimeException ignored) {
            card = null;
            photo = null;
        }
    }

    private static int alertGravity(String position) {
        switch (TvAlertLayout.position(position)) {
            case TvAlertLayout.TOP_LEFT:
                return Gravity.TOP | Gravity.START;
            case TvAlertLayout.BOTTOM_LEFT:
                return Gravity.BOTTOM | Gravity.START;
            case TvAlertLayout.BOTTOM_RIGHT:
                return Gravity.BOTTOM | Gravity.END;
            case TvAlertLayout.CENTER:
                return Gravity.CENTER;
            default:
                return Gravity.TOP | Gravity.END;
        }
    }

    /** Saves a Home Assistant alert placement. Ignores voice-overlay settings. */
    private void applyAlertState(JSONObject state) {
        if (state == null || !new OverlayPreferences(this).haManage()) return;
        JSONObject attributes = state.optJSONObject("attributes");
        if (attributes == null) return;
        OverlayPreferences prefs = new OverlayPreferences(this);
        if (!prefs.deviceId().equals(attributes.optString("kellie_device_id"))) return;
        String setting = attributes.optString("kellie_setting");
        if (!HaLayout.ALERT_POSITION.equals(setting) && !HaLayout.ALERT_SIZE.equals(setting)) return;
        if (attributes.optInt("kellie_generation", 0) <= 0) return;
        String raw = state.optString("state");
        if ("unavailable".equals(raw) || "unknown".equals(raw) || raw.isEmpty()) return;
        HaLayout.apply(prefs, setting, raw);
    }

    private void requestLiveStream(String entity, int current) {
        if (socket == null || liveSurface == null) {
            if (liveStatus != null) liveStatus.setText("Waiting for Home Assistant connection.");
            return;
        }
        int id = ++requestCounter;
        liveRequestId = id;
        liveRequestGeneration = current;
        try {
            boolean sent = socket.send(new JSONObject().put("id", id)
                    .put("type", "camera/stream").put("entity_id", entity)
                    .put("format", "hls").toString());
            if (!sent && liveStatus != null) liveStatus.setText("Could not request live camera.");
        } catch (Exception ignored) {
            if (liveStatus != null) liveStatus.setText("Could not request live camera.");
        }
    }

    private void playLiveStream(String path) {
        // The stream URL is generated by HA over our authenticated socket, never
        // supplied by a TV event. Reject other hosts or protocols completely.
        if (liveSurface == null || credentials == null || !TvEventPayload.hlsPathAllowed(path)) {
            if (liveStatus != null) liveStatus.setText(
                    "HA returned an unsupported HLS address; showing camera snapshot.");
            return;
        }
        HttpUrl base = HttpUrl.parse(credentials.url);
        HttpUrl url = base == null ? null : base.resolve(path);
        if (base == null || url == null || !base.scheme().equals(url.scheme())
                || !base.host().equals(url.host()) || base.port() != url.port()
                || !url.encodedPath().startsWith("/api/hls/")) {
            if (liveStatus != null) liveStatus.setText("Untrusted HLS stream address rejected.");
            return;
        }
        try {
            ExoPlayer player = new ExoPlayer.Builder(this).build();
            player.setVolume(0f); // Do not steal focus or sound from the TV program.
            player.addListener(new Player.Listener() {
                @Override public void onRenderedFirstFrame() {
                    if (player != livePlayer || liveSurface == null || liveStatus == null) return;
                    liveFirstFrame = true;
                    liveSurface.setAlpha(1f);
                    liveStatus.setText("LIVE · muted");
                }
                @Override public void onPlayerError(PlaybackException error) {
                    if (player == livePlayer && liveStatus != null) {
                        // The numeric Media3 code distinguishes HTTP errors,
                        // missing media segments, and decoder incompatibility.
                        // Never include exception details containing the signed URL.
                        liveStatus.setText("Live playback error "
                                + error.errorCode + "; showing camera snapshot.");
                        if (liveSurface != null) liveSurface.setAlpha(0f);
                    }
                }
            });
            livePlayer = player;
            player.setVideoTextureView(liveSurface);
            player.setMediaItem(new MediaItem.Builder().setUri(url.toString())
                    .setMimeType(MimeTypes.APPLICATION_M3U8).build());
            player.prepare();
            player.play();
        } catch (RuntimeException ignored) {
            if (liveStatus != null) liveStatus.setText(
                    "Cannot start video player; showing camera snapshot.");
            if (livePlayer != null) {
                livePlayer.release();
                livePlayer = null;
            }
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
        liveRequestId = -1;
        liveRequestGeneration = -1;
        liveFirstFrame = false;
        if (livePlayer != null && liveSurface != null) {
            livePlayer.clearVideoTextureView(liveSurface);
        }
        if (livePlayer != null) {
            livePlayer.release();
            livePlayer = null;
        }
        liveSurface = null;
        liveStatus = null;
        cameraVisible = false;
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
