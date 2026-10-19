# Home Assistant Voice Remote for Google TV

A **TV-native, voice-first Home Assistant assistant** for Google TV / Android TV, starting with the **onn 4K Pro**.

Inspired by [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration), but designed to work **over normal TV apps** instead of replacing the launcher or showing a permanent Home Assistant kiosk/dashboard.

> **Project stage:** working Android TV diagnostic APK, **not yet** a functional Home Assistant Assist client. See [full project vision and milestones](docs/PROJECT_VISION.md).

## What we're building

- Press the Google TV remote's voice button, speak to the selected Home Assistant Assist pipeline, and hear the reply through TV speakers.
- A small translucent **voice/chat overlay**, with listening/processing animation, transcript, response, and multi-turn conversations.
- Ask **"Show the driveway camera"** for a Frigate/go2rtc or Home Assistant camera overlay, optionally full screen.
- Ask **"Show my garage dashboard"** to open a TV-friendly HA dashboard view **only on demand**.
- Dismiss with Back or by voice to return to the previously playing app.
- Eventually add timers, announcements, rich assistant answers, optional built-in-microphone wake word, and multi-pipeline support.

**Do not require:** a permanent dashboard, kiosk mode, launcher replacement, root access, or firmware changes for MVP.

**Unverified hardware constraints:** Whether the onn firmware routes remote microphone audio and its voice button to a third-party app, and whether overlays work over every streaming/DRM app. These need real-device testing first.

## Download and test current diagnostic APK

1. Open the [GitHub Actions builds](../../actions/workflows/build-apk.yml) or the [latest successful build](../../actions).
2. Download the `onn-tv-voice-probe-debug` artifact and extract `app-debug.apk`.
3. Install it on the onn 4K Pro using Android debugging / ADB (`adb install -r app-debug.apk`).
4. Open **TV Voice Probe** from Apps.
5. Test remote microphone key delivery, default assistant role, audio input list, and mic signal levels. Check if the input comes from the remote or the box.

The probe does **not** connect to Home Assistant, stream or store audio, display overlays, or show cameras yet.

## Development roadmap

1. Confirm voice remote / built-in mic behavior on actual hardware.
2. Build authenticated HA Assist WebSocket + STT/TTS + transient overlay.
3. Add multi-turn chat and assistant information panels.
4. Add Frigate/go2rtc camera overlays and full-screen view.
5. Add optional on-demand dashboard viewer.
6. Add advanced satellite features (wake word, dual pipelines, timers, media entities).

Details, acceptance criteria, architecture, privacy rules and tradeoffs: **[Project vision](docs/PROJECT_VISION.md)**.

## Security and attribution

Never commit HA tokens, camera credentials, internal URLs, private keys or test recordings. Store credentials securely on the TV and support revocation. The inspiration repo is AGPL-3.0; review its licensing requirements before reusing any source code.
