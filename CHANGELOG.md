# Changelog

This is an experimental Android TV / Google TV app. Each entry distinguishes **shipped code** from **tested on the onn 4K Pro**.

## v0.6-in-app-updates (2026-10-20) — TV-native signed updater

- Added native Android TV **Check for app updates** and **Install downloaded update** menu actions, plus a roughly daily check when the full-screen app opens.
- Fetches **public stable GitHub Releases**; a newer `versionCode` prompts before downloading.
- Downloads APK directly over approved GitHub HTTPS hosts into app-private cache, verifies advertised size and **SHA-256 checksum**, and uses a scoped AndroidX FileProvider URI.
- Opens Android's standard user-approved APK installer; requests **Install unknown apps** permission if required. No root, silent background installation, HA-token upload or sideloading from arbitrary hosts.
- Added owner-controlled GitHub Actions `publish_release` checkbox. If selected in a manual run, signs and verifies the APK, then publishes a GitHub Release containing `app-release.apk` and `update.json`. Ordinary pushes do **not** publish releases.
- Increased Android `versionCode` to **6**, `versionName` to `0.6-in-app-updates`. Existing keystore and application ID are unchanged.
- **[Signed build #58](https://github.com/chadakenn/onn-tv-voice-remote./actions/runs/39411724076) passed** build and APK signature verification. **No public release has been published yet and the in-app install path is not hardware-verified.**
- [First-install and release publishing instructions](docs/UPDATES.md).

## v0.5-glass-voice (2026-10-20) — Glass UI and one-press voice prototype

- ✅ Added original animated cyan/purple waveform and wide translucent dark Glass skin, plus optional compact Classic styling.
- ✅ Added on-device preferences for Glass/Classic, auto-listen (ON by default) and auto-hide after speech (ON by default).
- ✅ Overlay launches a speech session **after Home Assistant authenticates**, eliminating a second TALK press. Added optional local voice-pause detection and 23-second recording safety timeout; manual STOP still works.
- ✅ Added optional main-app launcher fallback for the onn ⭐ app picker; overlay SETTINGS button keeps the configuration screen reachable.
- ✅ Incremented signed Android `versionCode` from 4 to 5; built with same permanent signing key. **[GitHub Actions build #50](https://github.com/chadakenn/onn-tv-voice-remote./actions/runs/39409512785) passed all signing and artifact steps**.
- ⚠️ **Awaiting actual onn box testing:** auto-listen, waveform, auto-stop with background video, auto-hide, ⭐ shortcut mapping and preserving credentials during signed upgrade. Previous v0.4 already displayed the overlay on Google TV Home. See [v0.5 testing](docs/V0_5_TESTING.md).

## v0.4-overlay-prototype (2026-10-20) — Opt-in floating TV voice

- Added an **experimental small, translucent floating voice panel** with D-pad TALK / STOP / CLOSE, recognized speech, HA text answer and speech output.
- Implemented `VoiceOverlayService` as an **explicitly user-started, microphone-typed Android foreground service**. It is not an always-on microphone; recording starts only during TALK, and stopping the overlay closes the service.
- Added consent flow for **Display over other apps** and microphone permission. No overlay is started without permission.
- Added a separate launcher entry **HA Voice Overlay**, which may be selectable as the onn ⭐ programmable remote shortcut **if the TV firmware allows app launch selection**.
- Version upgraded to `0.4-overlay-prototype` / Android `versionCode 4`. The package ID and **permanent release signing key** are unchanged, so signed-v0.3 users should be able to **update in place** without losing tokens.
- Signed GitHub Actions **[build #39](https://github.com/chadakenn/onn-tv-voice-remote./actions/runs/39400677478)** passed compilation, private key restoration and APK signature verification, and published `onn-tv-ha-assist-signed`.
- **Hardware testing still required:** overlay permission availability on onn Google TV, floating window display over launcher/streaming apps, D-pad focus and TTS during a video, shortcut remapping, task return behavior and in-place credential retention. See [overlay test instructions](docs/OVERLAY_TESTING.md).

## 2026-10-20 — Full voice conversation confirmed; GPLv3 selected

- ✅ **Real TV end-to-end success:** the onn 4K Pro understood “What time is it?”, displayed Home Assistant's reply **“5:07 AM”**, and **played TTS through the TV speakers**.
- ✅ The compact **TALK / STOP / SETTINGS** screen displays HA's answer and the recognized question without cropping the reply.
- ✅ Adopted **GNU GPL version 3, GPL-3.0-only**. Full license at [LICENSE](LICENSE); dependencies and research references retain their own licenses.
- ⏳ Longer-term repeated conversations, foreground audio behavior and the in-place upgrade of a second permanently signed APK remain to be verified.


## v0.3-assist-alpha (2026-10-20) — Signed release

- ✅ Added **persistent private release signing** using encrypted GitHub Actions secrets `TV_SIGNING_KEYSTORE_B64` and `TV_SIGNING_PASSWORD`. First successful signed build: [#31](https://github.com/chadakenn/onn-tv-voice-remote./actions/runs/39397223494).
- ✅ Changed downloadable artifact from old `onn-tv-ha-assist-alpha` debug ZIP to `onn-tv-ha-assist-signed` containing `app-release.apk`. Signature verification runs before publishing.
- ✅ Increased Android `versionCode` to 3 and set `versionName` to `0.3-assist-alpha`.
- ✅ Replaced the seven oversized action buttons with a **TALK / STOP / SETTINGS** layout. **The new layout subsequently passed an on-device test**, see the milestone above.
- ✅ Corrected parsing of the Assist TTS event URL from `tts_output.url`. **TV-speaker playback subsequently passed the real-device test.**
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
