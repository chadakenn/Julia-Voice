# HA TV Voice 🎙️📺

**Native Home Assistant Assist on Google TV / Android TV**, initially tested on the **onn 4K Pro**. This project aims to let you talk to Home Assistant without replacing your TV launcher or permanently covering streaming apps.

> **Status: v0.6 in-app updater alpha (2026-10-20).** **Confirmed on real onn 4K Pro:** Home Assistant understood a spoken question, displayed the answer and played it through TV speakers; the v0.4 floating overlay appeared over Google TV Home. **v0.5:** Glass waveform and auto-listen (still needs field testing). **v0.6:** periodic GitHub Release checks, APK download, SHA-256 verification, and user-approved Android installation. **The updater is built and signed, but is not yet tested on the TV.** Frigate PiP, HA announcements and wake words are future work.

## What's working and what's next

| Feature | State |
| --- | --- |
| Launch on onn 4K Pro with D-pad and OK navigation | ✅ Tested on TV |
| Built-in box microphone, 16 kHz `VOICE_RECOGNITION` audio capture | ✅ Tested |
| Authenticate via HA WebSocket and long-lived access token | ✅ Tested on TV |
| Stream microphone audio to HA Assist; receive speech transcript | ✅ Tested: “What time is it?” |
| Show HA answer in compact TALK / STOP / SETTINGS screen | ✅ Tested on TV; “5:07 AM” displayed |
| Play HA TTS through TV speakers | ✅ Tested; spoken reply heard |
| Permanently signed APK build | ✅ GitHub Actions run [#31](../../actions/runs/39397223494) successful |
| First debug-to-signed migration and in-place signed update | ⏳ Hardware confirmation pending |
| Floating overlay on Google TV Home | ✅ v0.4 shown and authenticated on onn box |
| v0.5 Glass skin, animated waveform and Classic option | 🔶 Signed build passed; await onn field test |
| v0.5 auto-listen/auto-stop + reply auto-hide | 🔶 Signed build passed; await onn field test |
| Dedicated **HA Voice Overlay** launcher or main-app ⭐ fallback | 🔶 Built, mapping depends on onn firmware |
| GitHub Release update check, download, checksum verification and Android installer | 🔶 v0.6 builds successfully; device test pending |
| HA media_player announcements, Frigate person-triggered PiP, dashboard on request | 📋 Planned |

The **Google voice button currently opens Google's assistant**, not this app. A regular remote can control TALK / STOP, and the onn box microphone has produced working STT audio. We are **not claiming access to the Bluetooth remote microphone**.

## Download and install

**Use signed release APKs only.** The old debug builds had incompatible ephemeral signing certificates.

1. Open [the latest successful **signed APK** build](../../actions/workflows/build-apk.yml). [Build #31](../../actions/runs/39397223494) is the first signing-and-signature-verified release.
2. Download the **`onn-tv-ha-assist-signed`** artifact ZIP from **Artifacts**. Extract **`app-release.apk`** (not `app-debug.apk`).
3. Send the APK to the onn 4K Pro using **Send Files to TV**, USB, or ADB. On TV, allow your file manager to install unknown apps when asked.
4. **Only if you still have the original debug-signed app installed:** uninstall that old app **once**, then install `app-release.apk`. This clears the previous encrypted Home Assistant settings and you'll have to enter your token again. **Do not uninstall until you have the signed APK available.**
5. **All future signed updates** should install directly over the signed app and preserve the saved HA connection, provided the permanent signing key remains unchanged and `versionCode` increases when necessary.

📘 [In-app updates and first v0.6 install](docs/UPDATES.md) · [Full installation / migration guide](docs/INSTALL.md) · [Signing and key backup](docs/SIGNING.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)

## First Home Assistant conversation

Prerequisites: a reachable Home Assistant server, a working **Assist pipeline** with STT/conversation agent (and TTS if you want spoken answers), and a **long-lived access token**.

1. Open **HA TV Voice Alpha**, then **SETTINGS → Configure Home Assistant**. Enter HA URL and token; choose **Save and connect**. Google TV's phone remote keyboard can help, but clipboard paste varies.
2. Wait for **Connected and authenticated**.
3. Select **TALK**, wait for **LISTENING**, speak toward the **onn 4K Pro box** (not the Google remote mic), then select **STOP**.
4. The **HOME ASSISTANT SAYS** answer appears above **YOU SAID**. Both on-screen answer and spoken TTS have been confirmed on the onn box in a first end-to-end test.
5. If speech-to-text works but no sound comes out, see [Troubleshooting](docs/TROUBLESHOOTING.md). Keep tokens out of screenshots.

The **proven v0.3** full-screen conversation continues to work; **v0.4** proved the overlay displays over Google TV Home. **v0.5** brings auto-listening after the explicit ⭐/app launch, a Glass waveform skin, optional Classic skin, a best-effort silence detector, configurable automatic dismissal and a TV-app-picker fallback. Google TV real-world behavior and audio/video coexistence still require testing. This is not a system Assistant replacement, wake-word listener, HA media_player or Frigate client. See the [v0.5 home test checklist](docs/V0_5_TESTING.md).

## Architecture and privacy

- **Android Java**, Android TV-compatible D-pad UI, `AudioRecord` with `VOICE_RECOGNITION` at **16 kHz mono PCM16**, OkHttp authenticated WebSocket `/api/websocket`.
- Standard Home Assistant **`assist_pipeline/run`** events, per-run binary audio handler, transcripts, intent response and server-returned TTS URL; Android `MediaPlayer` **played TTS successfully** in the first verified on-device conversation.
- The app stores the HA URL and access token encrypted with **Android Keystore** and local preferences; app backup is disabled. No microphone recording is saved or uploaded anywhere except audio intentionally **streamed to your configured HA server during TALK**.
- **HTTPS is recommended.** Plain local `http://` is allowed with a warning, but **exposes the token and microphone audio unencrypted to the network**. Do not use HTTP across untrusted networks.
- All installable APKs are now signed with a persistent private **GitHub Actions** keystore. Never commit signing files or tokens to this public repo.

More detail: [Field test results and checklist](docs/TESTING.md), [security notes](SECURITY.md), [changelog](CHANGELOG.md), and [developer / release checklist](docs/DEVELOPMENT.md).

## Roadmap

1. **Now (v0.6):** install signed updater over v0.5 once; test GitHub Releases check and Android install permission. The first published v0.6 will report up to date; download/install testing requires a newer release. [Updater guide](docs/UPDATES.md). Continue v0.5 Glass/⭐ testing as desired.
2. **Next:** refine ⭐ shortcut selection where firmware permits, overlay placement, audio focus, ambient-TV-noise tolerance, skins and recovery based on actual results.
3. **Afterward:** dedicated HA `media_player` with automation-driven chimes/TTS (e.g. “The washer is done”); secure companion channel.
4. **Then:** Frigate/go2rtc camera views, filtered doorbell-person PiP and optional spoken alerts; on-demand dashboards.
5. **Later:** optional wake word, timers, rich conversations, multi-pipeline selection and `assist_satellite` support.

See [project vision](docs/PROJECT_VISION.md), [announcements and PiP design](docs/ANNOUNCEMENTS_AND_PIP.md), [remote-key options](docs/REMOTE_BUTTON_FALLBACKS.md), and [Ava / protocol research](docs/AVA_RESEARCH.md). Those describe **goals**, not shipped features.

## License

**GNU General Public License version 3 (GPL-3.0-only).** See the complete [LICENSE](LICENSE) file. You can use, modify and redistribute the code under GPLv3 terms. Distribution of derivative binaries generally requires making the corresponding source available under the license, including preserving notices. The software comes **without warranty**.

This implementation was written independently; [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration) (AGPL-3.0) and [Ava](https://github.com/brownard/Ava) (Apache-2.0) are **research references**, not incorporated source. Any future code reuse must comply with the original work's licensing and attribution. Third-party dependencies retain their respective licenses.
