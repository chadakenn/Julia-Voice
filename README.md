# HA TV Voice 🎙️📺

**Native Home Assistant Assist on Google TV / Android TV**, initially tested on the **onn 4K Pro**. This project aims to let you talk to Home Assistant without replacing your TV launcher or permanently covering streaming apps.

> **Status: v0.3 signed foreground alpha (2026-10-20).** The app **authenticated to Home Assistant and successfully transcribed an actual spoken question on the onn 4K Pro**. Its compact answer-first interface and TV speaker TTS are built but **still awaiting device verification**. Floating overlays, ⭐ shortcut activation, HA announcement/media-player integration and Frigate camera PiP are future work.

## What's working and what's next

| Feature | State |
| --- | --- |
| Launch on onn 4K Pro with D-pad and OK navigation | ✅ Tested on TV |
| Built-in box microphone, 16 kHz `VOICE_RECOGNITION` audio capture | ✅ Tested |
| Authenticate via HA WebSocket and long-lived access token | ✅ Tested on TV |
| Stream microphone audio to HA Assist; receive speech transcript | ✅ Tested: “What time is it?” |
| Show HA answer in compact, couch-readable TALK / STOP / SETTINGS screen | 🔶 Built, not tested on TV |
| Play HA TTS through TV speakers | 🔶 Implemented; needs real-world test |
| Permanently signed APK build | ✅ GitHub Actions run [#31](../../actions/runs/39397223494) successful |
| First debug-to-signed migration and in-place signed update | ⏳ Hardware confirmation pending |
| Program ⭐ shortcut; voice overlay over other TV apps | 📋 Planned |
| HA media_player announcements, Frigate person-triggered PiP, dashboard on request | 📋 Planned |

The **Google voice button currently opens Google's assistant**, not this app. A regular remote can control TALK / STOP, and the onn box microphone has produced working STT audio. We are **not claiming access to the Bluetooth remote microphone**.

## Download and install

**Use signed release APKs only.** The old debug builds had incompatible ephemeral signing certificates.

1. Open [the latest successful **signed APK** build](../../actions/workflows/build-apk.yml). [Build #31](../../actions/runs/39397223494) is the first signing-and-signature-verified release.
2. Download the **`onn-tv-ha-assist-signed`** artifact ZIP from **Artifacts**. Extract **`app-release.apk`** (not `app-debug.apk`).
3. Send the APK to the onn 4K Pro using **Send Files to TV**, USB, or ADB. On TV, allow your file manager to install unknown apps when asked.
4. **Only if you still have the original debug-signed app installed:** uninstall that old app **once**, then install `app-release.apk`. This clears the previous encrypted Home Assistant settings and you'll have to enter your token again. **Do not uninstall until you have the signed APK available.**
5. **All future signed updates** should install directly over the signed app and preserve the saved HA connection, provided the permanent signing key remains unchanged and `versionCode` increases when necessary.

📘 [Full installation / migration guide](docs/INSTALL.md) · [Signing and key backup](docs/SIGNING.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)

## First Home Assistant conversation

Prerequisites: a reachable Home Assistant server, a working **Assist pipeline** with STT/conversation agent (and TTS if you want spoken answers), and a **long-lived access token**.

1. Open **HA TV Voice Alpha**, then **SETTINGS → Configure Home Assistant**. Enter HA URL and token; choose **Save and connect**. Google TV's phone remote keyboard can help, but clipboard paste varies.
2. Wait for **Connected and authenticated**.
3. Select **TALK**, wait for **LISTENING**, speak toward the **onn 4K Pro box** (not the Google remote mic), then select **STOP**.
4. The **HOME ASSISTANT SAYS** answer should appear above **YOU SAID**. Check separately whether its spoken TTS reply plays through the TV speakers.
5. If speech-to-text works but no sound comes out, see [Troubleshooting](docs/TROUBLESHOOTING.md). Keep tokens out of screenshots.

The app currently runs **in the foreground**. It is *not* a voice overlay, system Assistant replacement, HA media player, wake-word listener or Frigate client yet.

## Architecture and privacy

- **Android Java**, Android TV-compatible D-pad UI, `AudioRecord` with `VOICE_RECOGNITION` at **16 kHz mono PCM16**, OkHttp authenticated WebSocket `/api/websocket`.
- Standard Home Assistant **`assist_pipeline/run`** events, per-run binary audio handler, transcripts, intent response and server-returned TTS URL; Android `MediaPlayer` attempts playback.
- The app stores the HA URL and access token encrypted with **Android Keystore** and local preferences; app backup is disabled. No microphone recording is saved or uploaded anywhere except audio intentionally **streamed to your configured HA server during TALK**.
- **HTTPS is recommended.** Plain local `http://` is allowed with a warning, but **exposes the token and microphone audio unencrypted to the network**. Do not use HTTP across untrusted networks.
- All installable APKs are now signed with a persistent private **GitHub Actions** keystore. Never commit signing files or tokens to this public repo.

More detail: [Field test results and checklist](docs/TESTING.md), [security notes](SECURITY.md), [changelog](CHANGELOG.md), and [developer / release checklist](docs/DEVELOPMENT.md).

## Roadmap

1. **Now:** verify the signed install, compact layout, HA answer, TTS sound, reconnect and safe upgrade from the next release.
2. **Next:** make voice available quickly via ⭐ shortcut and a nonintrusive floating overlay; improve audio focus and recovery.
3. **Afterward:** dedicated HA `media_player` with automation-driven chimes/TTS (e.g. “The washer is done”); secure companion channel.
4. **Then:** Frigate/go2rtc camera views, filtered doorbell-person PiP and optional spoken alerts; on-demand dashboards.
5. **Later:** optional wake word, timers, rich conversations, multi-pipeline selection and `assist_satellite` support.

See [project vision](docs/PROJECT_VISION.md), [announcements and PiP design](docs/ANNOUNCEMENTS_AND_PIP.md), [remote-key options](docs/REMOTE_BUTTON_FALLBACKS.md), and [Ava / protocol research](docs/AVA_RESEARCH.md). Those describe **goals**, not shipped features.

**Licensing:** This implementation was written independently; [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration) and [Ava](https://github.com/brownard/Ava) are research references. If code from those projects is incorporated later, their respective licenses must be followed.
