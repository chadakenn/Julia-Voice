# Home Assistant Voice Remote for Google TV

A **TV-native, voice-first Home Assistant assistant** for Google TV / Android TV, starting with the **onn 4K Pro**.

Inspired by [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration), but designed to work **over normal TV apps** instead of replacing the launcher or showing a permanent Home Assistant kiosk/dashboard.

> **Project stage:** working Android TV diagnostic APK, **not yet** a functional Home Assistant Assist client. See [full project vision and milestones](docs/PROJECT_VISION.md).

## What we're building

- Press the Google TV remote's voice button, speak to the selected Home Assistant Assist pipeline, and hear the reply through TV speakers.
- A small translucent **voice/chat overlay**, with listening/processing animation, transcript, response, and multi-turn conversations.
- Ask **"Show the driveway camera"** for a Frigate/go2rtc or Home Assistant camera overlay, optionally full screen.
- Announce **"The washer is done"** or similar automation events through a dedicated Home Assistant **media_player** entity with optional chime, spoken TTS, audio ducking and quiet-hours preferences.
- Pop up a **temporary Frigate front-door live camera PiP** when a filtered person detection occurs, with optional spoken "Someone is at the front door", cooldown and expand/close controls.
- Ask **"Show my garage dashboard"** to open a TV-friendly HA dashboard view **only on demand**.
- Dismiss with Back or by voice to return to the previously playing app.
- Eventually add timers, rich assistant answers, optional built-in-microphone wake word, and multi-pipeline support.

**Do not require:** a permanent dashboard, kiosk mode, launcher replacement, root access, or firmware changes for MVP.

**Unverified hardware constraints:** Whether the onn firmware routes remote microphone audio and its voice button to a third-party app, and whether overlays work over every streaming/DRM app. These need real-device testing first.

## Download and test current diagnostic APK

1. Open the [GitHub Actions builds](../../actions/workflows/build-apk.yml) or the [latest successful build](../../actions).
2. Download the `onn-tv-voice-probe-debug` artifact and extract `app-debug.apk`.
3. Install it on the onn 4K Pro using Android debugging / ADB (`adb install -r app-debug.apk`).
4. Open **TV Voice Probe** from Apps.
5. Test remote microphone key delivery and the default assistant role. Use **List audio inputs**, then compare the new **MIC** and **VOICE_RECOGNITION** 10-second audio-level tests:
   - With the microphone button **not pressed**, speak near the box and note signal levels / reported route.
   - Repeat while holding the remote microphone button, speaking only into the remote.
   - The remote may launch Google Assistant and our recorder may capture only silence, even with permission.
   - Compare the capture-route name/type and RMS/peak values. A generic mic route is not proof that the Bluetooth remote audio is available.

The probe does **not** connect to Home Assistant, provide media player or announcements, stream or store audio, display overlays, or show cameras yet.

## Development roadmap

1. Confirm voice remote / built-in mic behavior on actual hardware.
2. Build authenticated HA Assist WebSocket + STT/TTS + transient overlay.
3. Add multi-turn chat and assistant information panels.
4. Add Home Assistant **media_player** entity and automation-driven TTS/chime announcements.
5. Add Frigate/go2rtc camera previews, filtered person-triggered PiP alerts, and full-screen view.
6. Add optional on-demand dashboard viewer and richer multi-turn chat.
7. Add advanced satellite features (wake word, dual pipelines, timers, Assist Satellite entity).

Details, acceptance criteria, architecture, privacy rules and tradeoffs: **[Project vision](docs/PROJECT_VISION.md)** and **[Media player, announcements & Frigate PiP design](docs/ANNOUNCEMENTS_AND_PIP.md)**.

## Security and attribution

Never commit HA tokens, camera credentials, internal URLs, private keys or test recordings. Store credentials securely on the TV and support revocation. The inspiration repo is AGPL-3.0; review its licensing requirements before reusing any source code.
