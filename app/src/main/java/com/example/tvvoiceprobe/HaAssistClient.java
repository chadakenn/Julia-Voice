package com.example.tvvoiceprobe;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

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
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private final AtomicBoolean ended = new AtomicBoolean(false);
    private volatile String conversationId;
    private volatile int commandId = 1;
    private volatile boolean disposed;

    public HaAssistClient(Context context, Events events) {
        this.context = context.getApplicationContext();
        this.events = events;
    }

    public boolean isConnected() { return authenticated; }

    public synchronized void connect(String baseUrl, String token) {
        disconnect();
        disposed = false;
        if (token == null || token.trim().isEmpty()) {
            events.status("Missing Home Assistant access token");
            return;
        }
        try {
            okhttp3.HttpUrl parsed = okhttp3.HttpUrl.parse(baseUrl);
            if (parsed == null || (!"https".equals(parsed.scheme()) && !"http".equals(parsed.scheme()))) {
                throw new IllegalArgumentException("Use a full http:// or https:// Home Assistant address");
            }
            // User supplies the HA base address, not a manually entered WebSocket endpoint.
            okhttp3.HttpUrl url = parsed.newBuilder()
                    .addPathSegments("api/websocket")
                    .build(); // OkHttp newWebSocket() accepts HTTP(S) URLs and upgrades internally.
            events.status("Connecting to Home Assistant...");
            socket = http.newWebSocket(new Request.Builder().url(url).build(), new WebSocketListener() {
                @Override public void onOpen(WebSocket ws, Response response) {
                    events.status("Connected to server, waiting for authentication...");
                }

                @Override public void onMessage(WebSocket ws, String message) {
                    try { receive(ws, new JSONObject(message), token); }
                    catch (Exception e) { events.status("Unexpected Home Assistant response"); }
                }

                @Override public void onFailure(WebSocket ws, Throwable error, Response response) {
                    authenticated = false;
                    recording.set(false);
                    events.status("Connection failed. Check HA address, network and certificate.");
                }

                @Override public void onClosed(WebSocket ws, int code, String reason) {
                    authenticated = false;
                    recording.set(false);
                    if (!disposed) events.status("Disconnected from Home Assistant");
                }
            });
        } catch (Exception e) {
            events.status("Connection setup error: " + e.getMessage());
        }
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
            authenticated = true;
            events.status("Connected and authenticated. Select TALK.");
            return;
        }
        if ("auth_invalid".equals(type)) {
            authenticated = false;
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
                break;
            case "stt-end":
                endRequested = true;
                recording.set(false);
                JSONObject stt = data.optJSONObject("stt_output");
                if (stt != null) events.transcript(stt.optString("text", ""));
                events.status("Thinking...");
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
                break;
            default:
                break;
        }
    }

    public synchronized void talk() {
        if (!authenticated || socket == null) {
            events.status("Connect to Home Assistant first.");
            return;
        }
        if (currentRunId != -1) {
            events.status("Finish the current conversation first.");
            return;
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            events.status("Microphone permission is required.");
            return;
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
            ended.set(false);
            currentRunId = id;
            if (!socket.send(request.toString())) {
                currentRunId = -1;
                events.status("Couldn't send Assist request.");
            } else {
                events.status("Preparing to listen...");
            }
        } catch (Exception e) {
            currentRunId = -1;
            events.status("Couldn't start Assist pipeline.");
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
        if (captureStarted || runId != currentRunId || handlerId < 0) return;
        captureStarted = true;
        recording.set(true);
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
                byte[] pcm = new byte[3200]; // 100 ms at 16kHz, mono, 16-bit
                while (recording.get() && runId == currentRunId) {
                    int read = recorder.read(pcm, 0, pcm.length);
                    if (!recording.get() || runId != currentRunId) break;
                    if (read <= 0) throw new IllegalStateException("Microphone read failed (" + read + ")");
                    if (read % 2 != 0) read--; // PCM16 samples must be complete
                    if (read == 0) continue;
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
