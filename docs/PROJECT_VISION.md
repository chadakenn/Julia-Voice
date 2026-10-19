# Project vision: Home Assistant TV Voice

## Product definition

Build a TV-native, voice-first Home Assistant assistant for Google TV / Android TV, starting with the **onn 4K Pro**. It is inspired by [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration) but is **not** a wall tablet, browser kiosk, permanent dashboard, or replacement launcher.

**North star:** The TV remains a TV. Press the voice button (or use an optional wake word), converse naturally with Home Assistant, and see a tasteful transient overlay on top of the current TV app. On request, show a live camera, rich response, or Home Assistant dashboard. Dismiss and return to the same TV app.

Reference project features we want to evaluate: Assist pipeline routing, conversation history, TTS/STT, multiple pipelines, visual conversation, timers, announcements, media playback, and rich results. Implement a TV-native UX without requiring the reference project's browser/HA sidebar.

## User-facing experiences

### 1. TV stays playing

- In idle, **nothing** covers Netflix, Stremio, Nuvio, YouTube, or the launcher.
- Press voice key. An animated compact listening overlay appears; ideally the underlying video continues.
- Say: "Turn on the back garage lights." The user's transcript and assistant reply appear, with spoken TTS output through TV speakers.
- Audio focus can duck/pause normal playback if the OS allows, then restore it.
- After a configurable timeout, the overlay disappears. Back dismisses at once.
- No permanent dashboard and no intrusive persistent notification on screen.

### 2. Natural conversation and richer information

- Home Assistant Assist pipeline, using the configured conversation agent (including an LLM if the user chooses).
- Maintain conversation_id for relevant follow-up turns.
- Expand overlay to a scrollable chat panel when asked or when a response needs space.
- Rich result views for supported kinds of data: timers, weather, home status, answers with text, optional future images/video/cards.
- Two configurable Assist pipelines, optional wake-word routing later.
- Voice first; D-pad navigation and a manual launch icon as fallbacks.

### 3. Camera requests

- "Show the driveway camera": a live camera mini-view appears without replacing the current TV app where supported.
- "Make that full screen": switch to a full-screen low-latency camera player.
- "Show the front door and garage": future multi-camera grid.
- "Close the camera" / Back returns to the previous TV app.
- Use Home Assistant camera entities and/or **Frigate / go2rtc** streams (prefer WebRTC for low latency; consider HLS/other device-compatible fallback and codec support). Do not expose go2rtc or RTSP credentials to the public.
- Future: doorbell person detection can request a camera preview or announce, configurable and opt-in, with a cooldown.

### 4. Dashboard only when asked

- "Show my garage dashboard": open an allowlisted HA dashboard or view in a full-screen, TV-navigable screen; only when requested.
- Back / "close dashboard" restores the TV app.
- Dashboard rendering is a separate feature and must **not** be needed for the voice assistant to function.
- Prefer a dedicated WebView/activity; validate TV remote focus, token handling, and browser compatibility.

## Technical architecture (candidate)

- **Platform:** native Android TV application with a service/component for request handling, not a kiosk launcher. The current Java diagnostic probe will evolve into the TV app; consider Kotlin/Jetpack later if useful.
- **Audio/button input:** discover the firmware's assistant role routing and remote mic transport first. The **remote microphone is NOT yet verified** to be usable by third-party apps. We may need a default assistant implementation, remote-specific solution, or fallback to onn 4K Pro's built-in microphones. No rooting or firmware changes in MVP.
- **Home Assistant link:** authenticated WebSocket to Home Assistant, configured URL and selected Assist pipeline. Run `assist_pipeline/run`, send 16-bit PCM chunks, follow STT/intent/TTS events; maintain conversation_id. See https://developers.home-assistant.io/docs/voice/pipelines/.
- **Credentials:** device-local secure storage; never hard-code tokens or add them to public GitHub; support safe pairing/rotation and certificate verification.
- **UI:** an ephemeral overlay (assess `TYPE_APPLICATION_OVERLAY` / `SYSTEM_ALERT_WINDOW` availability and Android TV OEM restrictions); dedicated transparent activity or assistant window where supported. Test overlays on streaming/DRM apps: some content/system dialogs may block or obscure them.
- **Camera playback:** native player or secure embedded view selected by stream type. Use Home Assistant camera pipeline or restricted Frigate/go2rtc path, ideally with short-lived authenticated URLs; test codec, latency, and audio.
- **Actions:** explicit allowlisted commands/actions (`show_camera`, `show_dashboard`, `close_overlay`, etc.), mapped from trusted HA intents/events. Do not execute arbitrary speech-generated URLs, shell commands, or unrestricted Home Assistant service calls.
- **Integrations:** MVP may start as an Assist WebSocket client. Later consider an optional HA integration for `assist_satellite`, `media_player`, announcements, and device registration (not guaranteed by a plain client).
- **UX states:** idle -> listening -> processing -> responding -> idle, with camera/dashboard expanded states, Back/cancel/error handling, reconnection behavior.
- **Accessibility/privacy:** user-controlled microphone activation, obvious mic indicators, optional local wake word, minimal logging, mute control, configurable display duration and volume ducking.

## Priority and milestones

### Phase 0: hardware discovery (current working diagnostic APK)

- Confirm Android version/build and ability to assign assistant role.
- Does pressing the voice key deliver `KEYCODE_ASSIST`, an Assist intent, or an OEM/Google-only event?
- Does `AudioRecord` see samples from the remote microphone or only built-in mic?
- If remote audio is unavailable, test a practical fallback without breaking Google TV.
- Record test results on the actual onn 4K Pro.

### Phase 1: minimum useful TV assistant

- Pair locally to Home Assistant via secure authenticated WebSocket.
- Hold-to-talk / press-to-talk using available input.
- STT -> configured HA conversation agent -> TTS.
- Animated compact overlay with live state and transcripts.
- Dismissable, restores previous TV app and audio focus, reconnects after TV reboot.
- No dashboard needed.

### Phase 2: rich conversation and on-demand information

- Multi-turn conversational chat panel.
- Timer, notifications, home statuses, rich answer display, agent pipeline selection.
- Optional spoken announcements, opt-in.

### Phase 3: camera views

- Secure Frigate/go2rtc + Home Assistant camera discovery.
- Small camera overlay, full-screen camera player, voice controls, latency/error handling.
- Future doorbell-triggered preview and multi-camera grid.

### Phase 4: on-demand dashboards

- TV-friendly Home Assistant dashboard viewer when requested.
- Safe allowlist and close/return behavior.

### Phase 5: optional hands-free and advanced satellite behavior

- Built-in omnidirectional mic / wake word (only if appropriate for normal TV audio).
- Dual pipelines, satellite entities, media player, automations, contextual visual results.
- Reliability and performance polishing.

## Definition of success

1. With a streaming app playing, press the voice key (or a tested alternate button), speak, and receive a correct HA Assist answer with minimal disturbance.
2. Confirm the command can control a permitted HA device.
3. Request a Frigate camera by friendly name and see a live preview, then full screen; close and return to the previous content.
4. Request a named HA dashboard; show it only on demand; Back returns to TV.
5. Never put credentials in GitHub, never require a permanent kiosk/launcher, and always provide a reliable exit/recovery path.

## Explicit current limitations / decisions pending

- **Unknown:** Can this firmware route the actual onn voice remote button *and remote mic audio* to an app we control?
- **Unknown:** Does the system permit an overlay on top of all desired TV/DRM apps? Provide an activity-based fallback where necessary.
- **Unknown:** Which Frigate/go2rtc camera transport is fastest and reliably decoded by the onn box?
- **Not yet implemented:** Assist connection, audio streaming, overlay, cameras, dashboards, media focus. The current APK is ONLY a microphone/key diagnostic.
- **Licensing:** Voice Satellite is AGPL-3.0. We can learn from documented behavior and APIs, but must review licensing obligations before copying or incorporating any of its source.
- **No root / no firmware replacement** for initial work; do not promise every OEM limitation can be bypassed.

## Project links

- Inspiration: https://github.com/jxlarrea/voice-satellite-card-integration
- Assist pipeline docs: https://developers.home-assistant.io/docs/voice/pipelines/
- Frigate go2rtc docs: https://docs.frigate.video/configuration/go2rtc/
- Android assistant roles: https://developer.android.com/reference/android/app/role/RoleManager
- Android overlay window types: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
