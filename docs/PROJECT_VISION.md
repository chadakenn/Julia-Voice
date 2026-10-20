# Project vision: Julia Voice

**Project identity:** Julia Voice is named after **Julia**, the original local AI assistant. Tagline: **Your voice. Your home. Your AI.** Its first client is the Home Assistant Android TV voice app, but the brand intentionally extends beyond televisions. Current public repository and Android package identifiers remain unchanged to preserve signed updates and existing users.

## Product definition

Build a TV-native, voice-first Home Assistant assistant for Google TV / Android TV, starting with the **onn 4K Pro**. It is inspired by [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration) but is **not** a wall tablet, browser kiosk, permanent dashboard, or replacement launcher.

**North star:** The TV remains a TV. Use a configurable shortcut (ideally the onn ⭐ button) or an optional future wake word, converse naturally with Home Assistant, and see a tasteful transient overlay on top of the current TV app. The OEM voice button currently launches Google, so taking it over is **not** a current capability. On request, show a live camera, rich response, or Home Assistant dashboard. Dismiss and return to the same TV app.

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
- **Automated Frigate PiP:** on a configured, filtered person detection at the front door, show a small temporary live camera overlay plus an optional spoken announcement. Use event-ID deduplication, cooldown, quiet hours and on/off controls; never show video just for generic motion. Press OK to expand, Back to close. Overlays over streaming/DRM apps require actual onn testing.
- User-requested camera previews and opt-in proactive doorbell popups are both first-class capabilities. See [Media player / PiP specification](ANNOUNCEMENTS_AND_PIP.md).

### 4. Home Assistant media player, automation announcements, and proactive video

- Expose the onn 4K Pro as a **dedicated Home Assistant `media_player` entity**, with device availability and implemented playback/volume capabilities, not merely an Android TV/Google Cast generic player.
- Play spoken TTS and announcement chimes from HA scripts and automations, e.g. "The washer is done" or "Someone is at the front door". A native Android app and optional HA custom integration coordinate delivery and secure playback.
- Prefer ducking other app audio (with Android audio focus) and restoring it; some streaming apps may not comply. Respect announcement toggle, quiet hours, volume, and no-wake-on-standby defaults.
- Combine a Frigate doorbell announcement with a small live PiP camera popup that times out automatically. Provide controls to make it full screen, close it, or disable proactive video.
- Avoid stale delayed alerts on reconnect; dedupe Frigate events and queue only where appropriate.
- For full details and acceptance tests, see [Announcements and Frigate PiP](ANNOUNCEMENTS_AND_PIP.md).

### 5. Dashboard only when asked

- "Show my garage dashboard": open an allowlisted HA dashboard or view in a full-screen, TV-navigable screen; only when requested.
- Back / "close dashboard" restores the TV app.
- Dashboard rendering is a separate feature and must **not** be needed for the voice assistant to function.
- Prefer a dedicated WebView/activity; validate TV remote focus, token handling, and browser compatibility.

## Technical architecture (candidate)

- **Platform:** native Android TV Java app. The working hardware diagnostic has evolved into an **authenticated foreground Assist alpha** with TALK / STOP / SETTINGS. A background service and overlay are still to be built; consider Kotlin/Jetpack later only if useful.
- **Audio/button input:** On the actual onn 4K Pro, the remote's voice button launched Google Assistant. The box microphone successfully streamed audio to HA speech-to-text via `VOICE_RECOGNITION`. The third-party app has **not** gained Bluetooth remote mic access. Use box mic + D-pad TALK / STOP for the alpha; try the programmable ⭐ button later. No rooting or firmware changes in MVP.
- **Home Assistant link:** **Chosen for the alpha:** authenticated HA `/api/websocket` with `assist_pipeline/run`, 16 kHz PCM audio and TTS result handling; authentication and STT were confirmed on the real onn box. For a future HA media-player/satellite endpoint, plan a secure HA custom integration or reassess vetted ESPHome Native API architecture from [Ava research](AVA_RESEARCH.md). Never deploy Ava's original unauthenticated ESPHome server unchanged. See https://developers.home-assistant.io/docs/voice/pipelines/.
- **Credentials:** device-local secure storage; never hard-code tokens or add them to public GitHub; support safe pairing/rotation and certificate verification.
- **UI:** an ephemeral overlay (assess `TYPE_APPLICATION_OVERLAY` / `SYSTEM_ALERT_WINDOW` availability and Android TV OEM restrictions); dedicated transparent activity or assistant window where supported. Test overlays on streaming/DRM apps: some content/system dialogs may block or obscure them.
- **Camera playback:** native player or secure embedded view selected by stream type. Use Home Assistant camera pipeline or restricted Frigate/go2rtc path, ideally with short-lived authenticated URLs; test codec, latency, and audio.
- **Actions:** explicit allowlisted commands/actions (`show_camera`, `show_dashboard`, `close_overlay`, etc.), mapped from trusted HA intents/events. Do not execute arbitrary speech-generated URLs, shell commands, or unrestricted Home Assistant service calls.
- **Integrations:** Start with a native Assist WebSocket client, then implement a dedicated HA custom integration exposing a real `media_player` for announcements, availability and supported playback controls; make `assist_satellite` optional later. A plain Assist client does not automatically create these entities. Communicate over a secured, reconnecting HA-to-TV channel.
- **UX states:** idle -> listening -> processing -> responding -> idle, with camera/dashboard expanded states, Back/cancel/error handling, reconnection behavior.
- **Accessibility/privacy:** user-controlled microphone activation, obvious mic indicators, optional local wake word, minimal logging, mute control, configurable display duration and volume ducking.

## Priority and milestones

### Phase 0: hardware discovery (tested; diagnostic retained in app)

- Confirm Android version/build and ability to assign assistant role.
- Does pressing the voice key deliver `KEYCODE_ASSIST`, an Assist intent, or an OEM/Google-only event?
- Does `AudioRecord` see samples from the remote microphone or only built-in mic?
- If remote audio is unavailable, test a practical fallback without breaking Google TV.
- Test **native ⭐ customizable key** and **tvQuickActions / Button Mapper** to launch our assistant when firmware reserves the Google Assistant key. Button remapping does not itself give access to Bluetooth remote mic audio. See [Remote button fallback research and test matrix](REMOTE_BUTTON_FALLBACKS.md).
- Record test results on the actual onn 4K Pro. **Done for launch, D-pad navigation, microphone capture, Google mic-key routing, Home Assistant authentication and STT.** See [test matrix](TESTING.md).

### Phase 1: minimum useful TV assistant (foreground alpha in progress)

- **Implemented and tested:** authenticated Home Assistant WebSocket connection with a user-supplied token. The app currently also allows warned, unencrypted HTTP on trusted LANs; trusted TLS remains recommended. Secure phone-based pairing is a future improvement.
- **Implemented and tested:** D-pad press-to-talk using onn box microphone; user presses TALK and STOP in the foreground. No remote microphone-button interception.
- **Verified on real onn 4K Pro:** Home Assistant STT correctly understood “What time is it?”, the compact screen showed **“5:07 AM”**, and the user confirmed the answer was **spoken through the TV speakers**. Reliability across repeated sessions remains to be tested.
- Animated compact overlay with live state and transcripts.
- Dismissable, restores previous TV app and audio focus, reconnects after TV reboot.
- No dashboard needed.

### Phase 2: Home Assistant media player and proactive announcements

- Add a HA custom integration exposing a dedicated TV `media_player` entity and capability reporting.
- Play HA-generated TTS and chimes, starting with washer finished, with supported Android audio focus/ducking and quiet-hours options.
- Handle device online/offline state, command acknowledgements, safe authentication, queueing and reconnects.

### Phase 3: Frigate PiP and camera views

- Secure Frigate/go2rtc + Home Assistant camera discovery and authorized low-latency playback.
- Camera mini overlay and full-screen player for spoken requests.
- Filtered Frigate front-door person events -> optional audio notice + temporary corner PiP, configurable timeout/cooldown/dedupe and back/expand interaction.
- Test native PiP vs application overlay across video/DRM apps. Future multi-camera grid.

### Phase 4: rich conversation and on-demand dashboards

- Multi-turn chat panel, timers, home status, rich answers and agent pipeline selection.
- TV-friendly Home Assistant dashboard viewer only when requested.
- Safe allowlist and close/return behavior.

### Phase 5: optional hands-free and advanced satellite behavior

- Built-in omnidirectional mic / wake word (only if appropriate for normal TV audio).
- Dual pipelines, optional `assist_satellite` entities, media playback expansion, contextual visual results.
- Reliability and performance polishing.

## Definition of success

1. With a streaming app playing, press the voice key (or a tested alternate button), speak, and receive a correct HA Assist answer with minimal disturbance.
2. Confirm the command can control a permitted HA device.
3. Request a Frigate camera by friendly name and see a live preview, then full screen; close and return to the previous content.
4. Trigger a washer-finished HA automation and hear TTS from the TV's dedicated `media_player`, without losing previous TV content.
5. Trigger an allowed Frigate front-door person event and show a temporary live PiP plus optional TTS, with cooldown and Back/full-screen controls.
6. Request a named HA dashboard; show it only on demand; Back returns to TV.
7. Never put credentials in GitHub, never require a permanent kiosk/launcher, and always provide a reliable exit/recovery path.

## Explicit current limitations / decisions pending

- **Tested:** Remote's voice button launches Google Assistant; our app does not intercept it. Actual Bluetooth remote mic audio for third-party apps remains **unverified/unavailable through our current implementation**. The onn box microphone works for HA STT.
- **Unknown:** Does the system permit an overlay on top of all desired TV/DRM apps? Provide an activity-based fallback where necessary.
- **Unknown:** Which Frigate/go2rtc camera transport is fastest and reliably decoded by the onn box?
- **Implemented and hardware-verified:** foreground HA Assist connection, microphone audio streaming, speech transcript, answer-first TV UI and spoken TTS playback (first successful end-to-end question). Android Keystore credential storage and permanent signed release builds are implemented.
- **Not yet implemented / not verified:** dedicated HA media_player entity, automation announcements, app overlay, Frigate PiP, cameras, dashboards, button launcher shortcut, background voice, media focus and repeated-session stability. Signed update retaining credentials also needs field confirmation. See [field tests](TESTING.md).
- **Licensing:** This app is now **GPL-3.0-only**, with the full [project LICENSE](../LICENSE). Voice Satellite is AGPL-3.0 and Ava is Apache-2.0; both have been used as research references. Any future source incorporation needs deliberate license and attribution review.
- **No root / no firmware replacement** for initial work; do not promise every OEM limitation can be bypassed.

## Project links

- Inspiration: https://github.com/jxlarrea/voice-satellite-card-integration
- Assist pipeline docs: https://developers.home-assistant.io/docs/voice/pipelines/
- Frigate go2rtc docs: https://docs.frigate.video/configuration/go2rtc/
- Android assistant roles: https://developer.android.com/reference/android/app/role/RoleManager
- Android overlay window types: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
