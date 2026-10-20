# Home Assistant TV Voice for onn 4K Pro

A **native Google TV / Android TV voice assistant** that eventually overlays normal streaming apps, supports Home Assistant announcements and shows Frigate camera picture-in-picture.

> **Current alpha:** The first Home Assistant **push-to-talk client is implemented and compiles**, but it is **NOT yet tested against the user's Home Assistant instance**. The current build is **foreground-only**, not the future floating overlay, media_player or doorbell PiP. The original diagnostics remain accessible from the main menu.

## Install the alpha APK

1. Go to [GitHub Actions](../../actions/workflows/build-apk.yml), open the latest **successful** run.
2. Under **Artifacts**, download `onn-tv-voice-probe-debug` (the artifact name is inherited from the original probe).
3. Extract `app-debug.apk` from the ZIP and transfer it to the onn 4K Pro (e.g. Send Files to TV).
4. Install over the previous test APK; both share the same app ID. Open **HA TV Voice Alpha**.
5. Use the remote's up/down and OK buttons. The previous microphone tests are under **Hardware microphone diagnostics**.

## First voice test on the TV

Prerequisites: an existing Home Assistant Assist pipeline with STT, conversation agent and TTS configured, a reachable Home Assistant URL, and a user-created **long-lived access token**.

1. On the TV, choose **Configure Home Assistant**. Paste the full Home Assistant URL (prefer a trusted `https://` URL, including a Nabu Casa remote URL if that is what works) and the access token. The Google TV phone remote may help type the token.
2. Press **Save and connect**, then look for **Connected and authenticated**. If a certificate or login is rejected, check the URL/token and error shown in the app.
3. Choose **TALK**. Grant microphone permission if prompted. Wait until it says **LISTENING**, then speak toward the **onn 4K Pro box**, not the Google remote.
4. Choose **STOP** when finished. The app sends the end-of-audio packet and waits for a transcript and Home Assistant response.
5. Verify the spoken answer plays through the TV speakers and the text appears on screen. Try a simple command like **"What time is it?"**, then a harmless HA device command if desired.
6. Report the exact on-screen status/transcript and whether TTS played. Don't share your actual HA token or private URLs.

**Token safety:** The configured token is encrypted locally using Android Keystore, app backup is disabled, and it is **never** stored in the GitHub repo. You can remove it via **Forget saved connection**. HTTPS is the recommended transport. The app warns before allowing plain HTTP because audio and credentials will be unencrypted in transit.

**Known alpha constraints:** Works in the **foreground** with a D-pad TALK/STOP action. No wake word, system-level remote mic capture, automatic ⭐ button mapping, background voice control, overlay, Frigate streaming or HA media_player entity yet. Your earlier hardware tests suggested usable built-in microphone input, but actual STT quality is unverified.

## How it works

- Native Android Java app with OkHttp WebSocket client to HA `/api/websocket` and standard HA token auth.
- Starts `assist_pipeline/run` (`stt` to `tts`, preferred pipeline, 16 kHz PCM).
- Waits for the server's `stt_binary_handler_id` and `stt-start` before recording; binary chunks include handler byte. STOP sends the one-byte end-of-audio marker.
- Processes speech transcript, intent response, conversation ID and TTS URL; plays authorized HA TTS audio using Android `MediaPlayer`.
- Android KeyStore encrypts saved connection details; URLs are validated and only HA-related playback sources can receive the token.
- Separate **TV Voice Probe** diagnostic screen remains available for keycodes, mic source tests, assistant role check and input routing.

## Future development

- Translucent voice/chat overlay on current streaming app; robust audio-focus controls.
- Programmable ⭐ button integration (remote mic button still invokes Google and isn't proven routable).
- Real Home Assistant `media_player` entity for announcements, TTS and availability.
- Proactive Frigate person-event doorbell PiP, with quiet hours and cooldown; request cameras by voice.
- On-demand dashboard and richer conversations.

Details: [Project vision](docs/PROJECT_VISION.md), [Ava research](docs/AVA_RESEARCH.md), [announcements + Frigate PiP](docs/ANNOUNCEMENTS_AND_PIP.md), [remote key fallback testing](docs/REMOTE_BUTTON_FALLBACKS.md).

## Security and licensing

Do not commit access tokens, camera passwords, device secrets or recordings. This repo was written independently with Voice Satellite and Ava as design references; copying their source later requires compliance with their respective AGPL-3.0 or Apache-2.0 licenses.
