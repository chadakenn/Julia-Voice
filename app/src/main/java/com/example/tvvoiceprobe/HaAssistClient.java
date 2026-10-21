package com.example.tvvoiceprobe;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Foreground push-to-talk client for Home Assistant Assist's authenticated WebSocket API.
 * Streams 16 kHz/mono/PCM16 only while actively listening, with the server's handler byte.
 * Not an ESPHome API client and not yet an Assist Satellite / background TV service.
 */
public final class HaAssistClient {
    public interface Events {
        void status(String message);
        void transcript(String text);
        void answer(String text);
        void ttsUrl(String url);
        // Optional typed lifecycle callbacks; existing full-screen client
        // continues to work without implementing these methods.
        default void ready() { }
        default void listening() { }
        default void processing() { }
        default void localSilence() { }
        default void runEnded() { }
        /** Unexpected socket loss; in-progress Assist run has been reset. */
        default void connectionLost() { }
    }

    private final Context context;
    private final Events events;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .pingInterval(25, TimeUnit.SECONDS)
            .build();
    private volatile WebSocket socket;
    private volatile boolean authenticated;
    private volatile int currentRunId = -1;
    private volatile int handlerId = -1;
    private volatile boolean endRequested;
    private volatile boolean captureStarted;
    private volatile boolean autoEndSpeech;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private final AtomicBoolean ended = new AtomicBoolean(false);
    private volatile String conversationId;
    private volatile int commandId = 1;
    private volatile boolean disposed = true;

    // Keep one authenticated WebSocket alive only for the lifetime of the
    // explicit activity/overlay. Android can suspend or kill the app, so this
    // is self-healing, not an always-running background assistant service.
    private String savedBaseUrl;
    private String savedToken;
    private boolean reconnectEnabled;
    private boolean reconnectScheduled;
    private boolean connecting;
    private int failures;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Runnable reconnectTask = () -> {
        synchronized (HaAssistClient.this) {
            reconnectScheduled = false;
            if (reconnectEnabled && !disposed && !authenticated && !connecting) {
                openConnection();
            }
        }
    };

    public HaAssistClient(Context context, Events events) {
        this.context = context.getApplicationContext();
        this.events = events;
    }

    public boolean isConnected() { return authenticated; }

    /** Explicit start/reconfigure. Tokens never enter logs or retry messages. */
    public synchronized void connect(String baseUrl, String token) {
        disconnect();
        if (baseUrl == null || token == null || token.trim().isEmpty()) {
            events.status("Missing Home Assistant address or access token.");
            return;
        }
        try {
            okhttp3.HttpUrl parsed = okhttp3.HttpUrl.parse(baseUrl);
            if (parsed == null ||
                    (!"https".equals(parsed.scheme()) && !"http".equals(parsed.scheme()))) {
                throw new IllegalArgumentException("Use a full Home Assistant http:// or https:// address.");
            }
            // Reject a URL with credentials in it. Use only the saved HA access token.
            if (!parsed.username().isEmpty() || !parsed.password().isEmpty()) {
                throw new IllegalArgumentException("Do not put credentials in the Home Assistant URL.");
            }
            savedBaseUrl = baseUrl;
            savedToken = token;
            disposed = false;
            reconnectEnabled = true;
            failures = 0;
            openConnection();
        } catch (Exception error) {
            events.status("Invalid Home Assistant address. Check settings.");
        }
    }

    /**
     * When the app returns to the foreground, reconnect if Android interrupted
     * its socket. Never start a microphone or a voice request here.
     */
    public synchronized void ensureConnected() {
        if (disposed || !reconnectEnabled || authenticated || connecting
                || savedToken == null) return;
        // Foreground user activity should reconnect immediately instead of
        // waiting out a previous 30-second background backoff timer.
        if (reconnectScheduled) {
            reconnectHandler.removeCallbacks(reconnectTask);
            reconnectScheduled = false;
        }
        openConnection();
    }

    private synchronized void openConnection() {
        if (disposed || !reconnectEnabled || connecting || authenticated
                || savedBaseUrl == null || savedToken == null) return;
        try {
            okhttp3.HttpUrl base = okhttp3.HttpUrl.parse(savedBaseUrl);
            if (base == null) throw new IllegalStateException("Missing HA URL");
            okhttp3.HttpUrl url = base.newBuilder()
                    .addPathSegments("api/websocket").build();
            final String authToken = savedToken;
            connecting = true;
            events.status(failures == 0 ? "Connecting to Home Assistant..."
                    : "Reconnecting to Home Assistant...");
            socket = http.newWebSocket(new Request.Builder().url(url).build(),
                    new WebSocketListener() {
                @Override public void onOpen(WebSocket ws, Response response) {
                    if (isCurrent(ws)) {
                        events.status("Connected to server, waiting for authentication...");
                    }
                }

                @Override public void onMessage(WebSocket ws, String message) {
                    if (!isCurrent(ws)) return;
                    try { receive(ws, new JSONObject(message), authToken); }
                    catch (Exception e) {
                        if (isCurrent(ws)) events.status("Unexpected Home Assistant response.");
                    }
                }

                @Override public void onFailure(WebSocket ws, Throwable error,
                        Response response) {
                    connectionLost(ws);
                }

                @Override public void onClosed(WebSocket ws, int code, String reason) {
                    connectionLost(ws);
                }
            });
        } catch (Exception error) {
            connecting = false;
            scheduleReconnect();
        }
    }

    private synchronized boolean isCurrent(WebSocket ws) {
        return !disposed && reconnectEnabled && ws == socket;
    }

    private void connectionLost(WebSocket ws) {
        synchronized (this) {
            // The old socket can close AFTER a new one authenticated. Never
            // allow its callback to mark the replacement disconnected.
            if (ws != socket || disposed) return;
            socket = null;
            authenticated = false;
            connecting = false;
            stopForRunEnd();
            if (reconnectEnabled) scheduleReconnect();
        }
        events.connectionLost();
    }

    private synchronized void scheduleReconnect() {
        if (disposed || !reconnectEnabled || reconnectScheduled) return;
        // 2, 4, 8, 16, 30 seconds with a cap; prevents rapid connection loops
        // when HA restarts or the TV's Wi-Fi is down.
        int attempt = Math.min(failures++, 4);
        long delay = Math.min(30000L, 2000L * (1L << attempt));
        reconnectScheduled = true;
        events.status("Home Assistant disconnected. Retrying automatically in "
                + (delay / 1000L) + " seconds...");
        reconnectHandler.postDelayed(reconnectTask, delay);
    }

    private void receive(WebSocket ws, JSONObject msg, String token) throws Exception {
        if (ws != socket || disposed) return;
        String type = msg.optString("type");
        if ("auth_required".equals(type)) {
            JSONObject auth = new JSONObject();
            auth.put("type", "auth");
            auth.put("access_token", token);
            ws.send(auth.toString());
            return;
        }
        if ("auth_ok".equals(type)) {
            synchronized (this) {
                if (!isCurrent(ws)) return;
                authenticated = true;
                connecting = false;
                failures = 0;
                reconnectHandler.removeCallbacks(reconnectTask);
                reconnectScheduled = false;
            }
            events.status("Connected and authenticated. Ready.");
            events.ready();
            return;
        }
        if ("auth_invalid".equals(type)) {
            synchronized (this) {
                if (!isCurrent(ws)) return;
                authenticated = false;
                connecting = false;
                reconnectEnabled = false; // Invalid credentials cannot heal by retrying.
                recording.set(false);
                stopForRunEnd();
                reconnectHandler.removeCallbacks(reconnectTask);
                reconnectScheduled = false;
            }
            events.status("Authentication rejected. Check the long-lived access token.");
            ws.close(1000, "invalid auth");
            return;
        }
        if ("result".equals(type)) {
            if (msg.optInt("id", -1) == currentRunId && !msg.optBoolean("success", false)) {
                JSONObject error = msg.optJSONObject("error");
                events.status("Assist request rejected: " +
                        (error == null ? "Check the selected pipeline" : error.optString("message", "Unknown error")));
                stopForRunEnd();
            }
            return;
        }
        if (!"event".equals(type) || msg.optInt("id", -1) != currentRunId) return;
        JSONObject event = msg.optJSONObject("event");
        if (event == null) return;
        JSONObject data = event.optJSONObject("data");
        if (data == null) data = new JSONObject();
        String eventType = event.optString("type");
        switch (eventType) {
            case "run-start":
                JSONObject runner = data.optJSONObject("runner_data");
                int id = runner == null ? -1 : runner.optInt("stt_binary_handler_id", -1);
                if (id < 0 || id > 255) {
                    events.status("Assist did not provide a valid audio handler ID");
                    stopForRunEnd();
                    break;
                }
                handlerId = id;
                events.status("Audio handler ready. Preparing microphone...");
                break;
            case "stt-start":
                if (endRequested) sendEndOfAudio(currentRunId);
                else beginCapture(currentRunId);
                break;
            case "stt-vad-end":
                endRequested = true;
                recording.set(false);
                events.status("Speech detected. Processing...");
                events.processing();
                break;
            case "stt-end":
                endRequested = true;
                recording.set(false);
                JSONObject stt = data.optJSONObject("stt_output");
                if (stt != null) events.transcript(stt.optString("text", ""));
                events.status("Thinking...");
                events.processing();
                break;
            case "intent-start":
                events.status("Home Assistant is responding...");
                break;
            case "intent-end":
                JSONObject out = data.optJSONObject("intent_output");
                if (out != null) {
                    String nextConversationId = out.optString("conversation_id", "");
                    if (!nextConversationId.isEmpty()) conversationId = nextConversationId;
                    JSONObject response = out.optJSONObject("response");
                    if (response != null) {
                        JSONObject speech = response.optJSONObject("speech");
                        JSONObject plain = speech == null ? null : speech.optJSONObject("plain");
                        if (plain != null) events.answer(plain.optString("speech", ""));
                    }
                }
                break;
            case "tts-start":
                events.status("Generating spoken response...");
                break;
            case "tts-end":
                // HA supplies the playback URL INSIDE tts_output, not at the
                // top level of the event. Earlier builds silently missed TTS.
                JSONObject tts = data.optJSONObject("tts_output");
                String audio = tts == null ? data.optString("url", "")
                        : tts.optString("url", "");
                if (!audio.isEmpty()) events.ttsUrl(audio);
                else events.status("Home Assistant didn't return a TTS audio URL.");
                break;
            case "error":
                events.status("Assist error: " + data.optString("code", "unknown") +
                        " (" + data.optString("message", "no details") + ")");
                stopForRunEnd();
                break;
            case "run-end":
                stopForRunEnd();
                events.status("Ready for another question.");
                events.runEnded();
                break;
            default:
                break;
        }
    }

    public synchronized boolean talk() {
        return talk(false);
    }

    /**
     * Auto mode begins listening on command and uses conservative local
     * silence detection to finish after an utterance. Manual STOP remains
     * available if TV playback sound confuses the detector.
     */
    public synchronized boolean talk(boolean autoStopOnSilence) {
        if (!authenticated || socket == null) {
            events.status("Connect to Home Assistant first.");
            return false;
        }
        if (currentRunId != -1) {
            events.status("Finish the current conversation first.");
            return false;
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            events.status("Microphone permission is required.");
            return false;
        }
        try {
            JSONObject request = new JSONObject();
            int id = commandId++;
            request.put("id", id);
            request.put("type", "assist_pipeline/run");
            request.put("start_stage", "stt");
            request.put("end_stage", "tts");
            JSONObject input = new JSONObject();
            input.put("sample_rate", 16000);
            request.put("input", input);
            if (conversationId != null) request.put("conversation_id", conversationId);
            handlerId = -1;
            endRequested = false;
            captureStarted = false;
            autoEndSpeech = autoStopOnSilence;
            ended.set(false);
            currentRunId = id;
            if (!socket.send(request.toString())) {
                currentRunId = -1;
                events.status("Couldn't send Assist request.");
                return false;
            }
            events.status("Preparing to listen...");
            return true;
        } catch (Exception e) {
            currentRunId = -1;
            events.status("Couldn't start Assist pipeline.");
            return false;
        }
    }

    public void finishTalking() {
        if (currentRunId < 0) {
            events.status("Press TALK first.");
            return;
        }
        endRequested = true;
        if (!captureStarted && handlerId >= 0) {
            sendEndOfAudio(currentRunId);
        }
        recording.set(false);
        events.status("Finishing audio. Waiting for Home Assistant...");
    }

    private void beginCapture(int runId) {
        synchronized (this) {
            if (captureStarted || runId != currentRunId || handlerId < 0 || disposed) return;
            captureStarted = true;
            recording.set(true);
        }
        new Thread(() -> {
            AudioRecord recorder = null;
            try {
                int minimum = AudioRecord.getMinBufferSize(16000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (minimum <= 0) throw new IllegalStateException("16kHz microphone audio is unsupported");
                recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(minimum * 2, 6400));
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED)
                    throw new IllegalStateException("AudioRecord initialization failed");
                recorder.startRecording();
                if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
                    throw new IllegalStateException("Microphone recording was not started");
                events.status("LISTENING. Speak toward the onn box; select STOP when done.");
                events.listening();
                byte[] pcm = new byte[3200]; // 100 ms at 16kHz, mono, 16-bit
                long observedMs = 0L;
                long voiceMs = 0L;
                long quietMs = 0L;
                boolean heardUtterance = false;
                double backgroundRms = 0.002;
                while (recording.get() && runId == currentRunId) {
                    int read = recorder.read(pcm, 0, pcm.length);
                    if (!recording.get() || runId != currentRunId) break;
                    if (read <= 0) throw new IllegalStateException("Microphone read failed (" + read + ")");
                    if (read % 2 != 0) read--; // PCM16 samples must be complete
                    if (read == 0) continue;

                    if (autoEndSpeech) {
                        long chunkMs = Math.max(1, read / 32);
                        observedMs += chunkMs;
                        double power = 0d;
                        for (int sample = 0; sample + 1 < read; sample += 2) {
                            short value = (short) ((pcm[sample] & 255) |
                                    (pcm[sample + 1] << 8));
                            double normalized = value / 32768.0;
                            power += normalized * normalized;
                        }
                        double rms = Math.sqrt(power / Math.max(1, read / 2));
                        // Adapt a quiet noise-floor estimate, but don't chase
                        // loud speech or a TV program's temporary peaks.
                        if (!heardUtterance && rms < backgroundRms * 2.2) {
                            backgroundRms = backgroundRms * 0.96 + rms * 0.04;
                        }
                        boolean voice = rms > Math.max(0.0045, backgroundRms * 2.4);
                        if (!heardUtterance) {
                            voiceMs = voice ? voiceMs + chunkMs : Math.max(0, voiceMs - chunkMs);
                            if (voiceMs >= 180) heardUtterance = true;
                        } else {
                            quietMs = voice ? 0 : quietMs + chunkMs;
                        }
                        // Don't finish before sending the final chunk.
                        if (heardUtterance && quietMs >= 1550 && observedMs >= 1100) {
                            events.status("Voice pause detected. Processing your question...");
                            events.localSilence();
                            endRequested = true;
                            recording.set(false);
                        }
                    }
                    WebSocket ws = socket;
                    int handler = handlerId;
                    if (ws == null || handler < 0) break;
                    byte[] packet = new byte[read + 1];
                    packet[0] = (byte) handler;
                    System.arraycopy(pcm, 0, packet, 1, read);
                    if (!ws.send(ByteString.of(packet))) throw new IllegalStateException("Audio socket closed");
                }
            } catch (Exception e) {
                events.status("Microphone streaming failed: " + e.getMessage());
            } finally {
                recording.set(false);
                if (recorder != null) {
                    try { recorder.stop(); } catch (Exception ignored) { }
                    recorder.release();
                }
                sendEndOfAudio(runId);
            }
        }, "ha-assist-audio").start();
    }

    private void sendEndOfAudio(int runId) {
        if (runId != currentRunId || handlerId < 0 || !ended.compareAndSet(false, true)) return;
        WebSocket ws = socket;
        if (ws != null) ws.send(ByteString.of(new byte[] { (byte)handlerId }));
    }

    private void stopForRunEnd() {
        recording.set(false);
        handlerId = -1;
        currentRunId = -1;
        captureStarted = false;
        endRequested = false;
    }

    public synchronized void disconnect() {
        disposed = true;
        reconnectEnabled = false;
        reconnectHandler.removeCallbacks(reconnectTask);
        reconnectScheduled = false;
        connecting = false;
        failures = 0;
        savedBaseUrl = null;
        savedToken = null;
        authenticated = false;
        conversationId = null;
        stopForRunEnd();
        WebSocket ws = socket;
        socket = null;
        if (ws != null) ws.close(1000, "leaving");
    }

    public void shutdown() {
        disconnect();
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}
