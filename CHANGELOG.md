# Changelog

This is an experimental Android TV / Google TV app. Each entry distinguishes **shipped code** from **tested on the onn 4K Pro**.

## v0.3-assist-alpha (2026-10-20) — Signed release

- ✅ Added **persistent private release signing** using encrypted GitHub Actions secrets `TV_SIGNING_KEYSTORE_B64` and `TV_SIGNING_PASSWORD`. First successful signed build: [#31](https://github.com/chadakenn/onn-tv-voice-remote./actions/runs/39397223494).
- ✅ Changed downloadable artifact from old `onn-tv-ha-assist-alpha` debug ZIP to `onn-tv-ha-assist-signed` containing `app-release.apk`. Signature verification runs before publishing.
- ✅ Increased Android `versionCode` to 3 and set `versionName` to `0.3-assist-alpha`.
- ✅ Replaced the seven oversized action buttons with a **TALK / STOP / SETTINGS** layout; the Home Assistant reply should appear above the recognized question. **New layout has not yet been verified on the TV.**
- ✅ Corrected parsing of the Assist TTS event URL from `tts_output.url`. **Spoken playback still awaiting TV validation.**
- ✅ Added one-time signing migration instructions and permanently ignored private keystores / local recordings.
- ✅ Documentation-only updates no longer automatically start APK builds.

**Migration:** Original debug-signed Android apps cannot be upgraded in place to this signed version. Uninstall the debug app **once** after obtaining a successful signed APK, re-enter the saved HA token, and thereafter install newer releases over the existing signed app. Preserve the signing-key backup privately.

## v0.2-assist-alpha (2026-10-20) — Assist conversation prototype

- Implemented HA `/api/websocket` token authentication, `assist_pipeline/run`, 16 kHz PCM capture and binary framing, answer/transcript display and initial Android `MediaPlayer` TTS.
- Android Keystore-encrypted local connection settings; settings dialog and microphone permission handling.
- **Verified on actual onn 4K Pro:** successfully authenticated and transcribed “What time is it?” through HA Assist.
- Initial app UI had too many buttons and pushed the answer below the screen. TTS audio outcome was not yet confirmed.

## v0.1-probe (2026-10-19 through 2026-10-20) — Hardware diagnostics

- Built and installed diagnostic APK using Send Files to TV.
- Fixed Android TV D-pad focus/navigation; verified arrow keys and OK worked on the real device.
- Enumerated four Android audio inputs. Nonzero built-in box mic levels were recorded through `MIC` and `VOICE_RECOGNITION`.
- Verified Google Assistant captures the physical voice button; third-party Bluetooth remote mic capture remains unsupported/unproven.
- **Known problem:** per-run GitHub debug signing keys caused Android package-update conflicts. Replaced by stable signed releases in v0.3.

See [Field testing](docs/TESTING.md) for precise verification status, and [project vision](docs/PROJECT_VISION.md) for features not yet built.
