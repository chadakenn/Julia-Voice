# Ava research: reuse and protocol options

Research date: 2026-10-19. **No Ava code has yet been incorporated into our app.** Our current APK is only the onn 4K Pro hardware probe.

## References reviewed

- Upstream native Android voice satellite: https://github.com/brownard/Ava (Apache License 2.0, `LICENSE.md`)
- Upstream Android microphone: https://github.com/brownard/Ava/blob/master/app/src/main/java/com/example/ava/esphome/android/microphone/AudioRecordMicrophone.kt
- Upstream voice run lifecycle: https://github.com/brownard/Ava/blob/master/app/src/main/java/com/example/ava/esphome/voiceassistant/VoicePipeline.kt
- Upstream media_player: https://github.com/brownard/Ava/blob/master/app/src/main/java/com/example/ava/esphome/entities/MediaPlayerEntity.kt
- Upstream foreground service: https://github.com/brownard/Ava/blob/master/app/src/main/java/com/example/ava/services/VoiceSatelliteService.kt
- Richer fork with floating windows / subtitles: https://github.com/pr0methevs/ava (read as design inspiration; verify license and bundled/proprietary elements before copying)
- HA WebSocket protocol: https://developers.home-assistant.io/docs/voice/pipelines/
- Voice Satellite browser/HA integration: https://github.com/jxlarrea/voice-satellite-card-integration (AGPL-3.0; do not directly copy without handling its obligations)
- HA community thread: https://community.home-assistant.io/t/ava-an-android-voice-assistant-using-the-esphome-protocol/975838

## What Ava already demonstrates

- ESPHome Native API server on Android. The Home Assistant ESPHome integration discovers it as a voice satellite with a media player, without a separate custom HA integration.
- Real Android `AudioRecord` capture with selectable audio source; upstream default is `MediaRecorder.AudioSource.VOICE_RECOGNITION` at 16 kHz, mono, PCM 16-bit. It does **not** prove that a Google TV remote's mic audio is exposed to third-party apps.
- Assistant states: listening, processing, responding, TTS, follow-up continuation, and handling pipeline events.
- Native media player, Home Assistant commands for playback/pause/stop/volume/mute, TTS and announcements.
- Foreground service and Wi-Fi wake lock for reliable local-network operation.
- User-controllable local wake words, timers and microphone mute.
- VoicePipeline waits for a start event before sending microphone buffers; it buffers temporarily until the server signals readiness, and ducks other audio.

## Security limitations before reuse

Original Ava README explicitly says **its ESPHome API port currently has no authentication**, so it should only be used on a trusted local LAN. Our publicly shareable Google TV app must NOT adopt this behavior without suitable protections. ESPHome Native API supports encrypted connections in appropriate configurations, but do not assume Ava currently implements this. Evaluate authenticated/encrypted ESPHome protocol implementation or restrictive LAN firewall rules as a temporary development-only measure.

Ava's source includes an optional trust-all-SSL-certificates setting. Do not adopt a certificate-validation bypass by default; use trusted certificates, secure pairing, and revocable credentials.

The simpler direct-HA-WebSocket approach provides standard HA bearer-token authentication over `wss://`, but a plain Assist client will **not** automatically create HA `assist_satellite` and `media_player` entities. Those need a companion HA integration or different supported registration.

**Do not combine both transports carelessly:** ESPHome API protobuf audio transport is **not** Home Assistant's WebSocket `assist_pipeline/run` binary framing. They are separate protocols with different roles.

## Correction to the suggested AudioRecord + WebSocket code

The following pseudocode is **not enough** for a working HA pipeline:

```kotlin
while (isListening) {
    val count = audioRecord.read(buffer, 0, buffer.size)
    if (count > 0) homeAssistantSocket.send(buffer.copyOf(count))
}
```

It captures PCM if Android exposes the input, but the HA **WebSocket** Assist protocol also requires all of the following:

1. Establish secure WebSocket at `/api/websocket`. Wait for `auth_required`, send `{ "type": "auth", "access_token": ... }`, confirm `auth_ok`.
2. Send `assist_pipeline/run` with a message `id`, `start_stage: "stt"`, `end_stage: "tts"`, and `input: { "sample_rate": 16000 }`. Include the chosen pipeline and conversation ID if relevant.
3. Follow result/events, including `run-start` with a per-run `stt_binary_handler_id` and `stt-start` before sending audio.
4. Every WebSocket **binary** PCM chunk must begin with the **one-byte `stt_binary_handler_id`** from `run-start`. The rest is the raw signed 16-bit mono PCM bytes in the expected sample rate. Don't send just the audio buffer without this framing byte. Preserve the actual bytes read and sample format.
5. End capture with **one binary byte containing only the handler ID**. This signals end-of-audio to the server.
6. Handle `stt-end` (recognized text), `intent-end` (result, continuation/conversation ID), `tts-end` (URL, content type/token), `run-end` and errors. Fetch playable TTS securely and use Media3/ExoPlayer with Android AudioFocus; keep the transcript and UX states in sync.
7. Stop/cancel, close/reconnect and heartbeat logic; bound in-memory buffers, not infinite loops on the UI thread; audio permission and privacy indicators.

If choosing the ESPHome transport instead, use ESPHome Native API message framing and appropriate protobuf events, not the above WebSocket messages.

## Architecture choice to make **after hardware tests**

| Candidate | Main advantage | Tradeoff |
| --- | --- | --- |
| **Ava-style ESPHome Native API** | Existing HA satellite **and media player** entities, automations, announcements and timers without our own HA integration. License-friendly original upstream. | Original Ava implementation lacks authentication; adding encryption/security is required. Some Google TV firmware limitations remain. |
| **Direct HA WebSocket Assist + custom HA integration** | Standard HA authentication/TLS for voice; explicit command channel for Frigate overlay, on-demand dashboard, and TV events. | Need to create/register our own HA media-player entity and announcement routing. More backend code. |
| **Hybrid** | Potentially ESPHome for satellite/media and separately authenticated endpoint for overlay controls. | Two transport stacks, duplicate lifecycle/authentication work and increased maintenance. Avoid unless clearly justified. |

**Provisional recommendation:** finish the real onn hardware probe first. Then prototype voice + media on the **least-complex secure** transport. Evaluate reusing vetted portions of original Apache-2.0 Ava under its license, but avoid importing it unmodified, and do not use unverified fork/proprietary features.

## Still custom TV work

- Remapping/handling onn voice remote mic key and Bluetooth microphone, or fallback to built-in mic.
- Non-disruptive overlay visible over streaming apps (hardware/DRM restrictions unknown).
- Speech/chat popup and on-demand Home Assistant dashboard.
- Low-latency **Frigate/go2rtc inbound camera PiP** and events. Ava fork's on-device **camera** functionality generally captures *the Android device camera*, which is not the same as receiving Frigate camera feeds.
- Better audio focus around streaming applications, quiet-hours announcements and popup permissions.
- Secure, TV-friendly pairing and headless background behavior.

## Licensing note

Original `brownard/Ava` is Apache-2.0; retain notices/copyright and review third-party dependencies for any copied code. The Voice Satellite project is AGPL-3.0, which has materially different terms. Attribution to a reference alone is not sufficient if its code is incorporated.
