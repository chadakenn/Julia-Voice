<div align="center">
  <img src="docs/branding/julia-voice-header.svg" alt="Julia Voice | Your voice. Your home. Your AI." width="100%" />
</div>

# Julia Voice 🎙️💜

**Your voice. Your home. Your AI.**

**Julia Voice** is a local-first, open-source voice interface for Home Assistant. **Julia** is the assistant's name, and the project is growing from its first Android TV app into a platform-neutral voice interface.

**Native Home Assistant Assist on Google TV / Android TV**, initially tested on the **onn 4K Pro**. This project aims to let you talk to Home Assistant without replacing your TV launcher or permanently covering streaming apps.

> **Status: v0.8 ⭐ shortcut compatibility update in development (2026-10-20).** **Confirmed on real onn 4K Pro:** Home Assistant understood a spoken question, displayed the answer and played it through TV speakers; the v0.4 floating overlay appeared over Google TV Home. **v0.5:** Glass waveform and auto-listen (still needs field testing). **v0.6:** periodic GitHub Release checks, APK download, SHA-256 verification, and user-approved Android installation. **v0.7 branding builds and signs; v0.8 fixes additional onn ⭐ shortcut launcher behaviors. The shortcut fallback still requires user opt-in and on-device verification.** Frigate PiP, HA announcements and wake words are future work.

## Compatibility and updates

**Julia Voice** now lives at [**chadakenn/Julia-Voice**](https://github.com/chadakenn/Julia-Voice). The Android `applicationId` (`com.example.tvvoiceprobe`) and release-signing key remain unchanged to preserve installations and settings. The public v0.6 APK predates the visual rebrand. The v0.7 development APK includes Julia Voice launcher labels, waveform icon, TV banner and welcome screen. **The revised v0.7 updater successfully built and passed signature verification in [CI run #77](https://github.com/chadakenn/Julia-Voice/actions/runs/39425244245), but is not yet a public stable Release or tested on the TV.** Since the repository has already been renamed, **installed v0.6 users may need a one-time manual signed APK upgrade**. See [upgrade instructions](docs/UPDATES.md#repository-rename-completed-chadakennjulia-voice).

A preliminary public search found other uses of "Julia" in AI and "Julia Voice" in music; no comprehensive trademark clearance or domain check has been completed. This README reflects our development branding rather than a claim of exclusive rights.

## onn ⭐ button shortcut

**Actual onn 4K Pro observation:** The firmware's ⭐ app picker lists only **Julia Voice**, not the separate **Julia Voice Overlay** entry. To make it open the floating voice assistant, open Julia Voice → **SETTINGS → Overlay style and voice settings → ⭐ picker fallback (main app opens overlay)**, and enable it. This is **OFF by default** to prevent unexpected microphone launches. Make sure microphone and *Display over other apps* permissions are granted by first testing **SETTINGS → Open floating voice overlay**.

The v0.8 development update also accepts ⭐ launch intents that omit the launcher category and reacts when Android reuses a running activity. If an installed v0.7 APK still opens the full app with fallback enabled, try the latest signed v0.8 development build over the existing install; do **not** uninstall and lose saved HA credentials. See [star-button test steps](docs/V0_5_TESTING.md).

## Julia Voice look and feel

The GitHub header uses a scalable [Julia Voice branded SVG](docs/branding/julia-voice-header.svg) with the approved midnight-navy, electric-cyan and violet-wave styling. It is a vector recreation inspired by the separately generated design concepts, not the original raster render.

For the Android TV app, v0.7 source includes:

- a matching, scalable **Julia Voice waveform launcher icon**
- an updated **Android TV launcher banner** with Julia Voice wordmark
- an approximately one-second **startup splash** on normal full-screen launches
- Julia Voice names in the **app launcher, ⭐ overlay shortcut, overlay panel, and notifications**

The overlay shortcut continues launching directly without the splash so the remote-button experience stays quick. No permissions, application ID, Home Assistant credentials, or APK signing settings were changed. **Test these visuals on actual onn 4K Pro hardware before distributing as stable.**

## What this project does

Your TV is the **voice front end**, not the AI server. Press the programmable ⭐ button (where the TV firmware allows app shortcuts), speak toward the onn box, and receive Home Assistant's answer as text and speech. The app sends requests through the Home Assistant Assist pipeline, so you can change the conversation agent or install new tools **without rebuilding the TV APK**.

**Already demonstrated on the onn 4K Pro:** authenticated HA Assist, recognized speech, a spoken answer through TV speakers, and a translucent overlay on Google TV Home. **Not yet verified on device:** v0.5 Glass/automatic voice flow and v0.6 automatic installation over other video apps.

## Our local AI and Home Assistant setup 🧠

We use a **self-hosted Ollama language-model server on our home network**, separate from the onn Android TV app and the Home Assistant host. Our earlier local Ollama setup used an **NVIDIA RTX 3060 (12 GB)** and tested the **Qwen3 8B** family; the exact currently selected model and Assist pipeline should be checked in Home Assistant rather than assumed.

On the **Home Assistant server**, we installed [**Tools for Assist (`llm_intents`)**](https://github.com/skye-harris/llm_intents) as a companion integration for **online search and weather**. The search and weather providers/tools must still be configured and made available to the selected conversation agent; installing `llm_intents` alone does not prove that every tool is working.

```text
Onn 4K Pro / Julia Voice TV app (mic, overlay, TTS playback)
                     |
          Home Assistant Assist pipeline
                     |
     Conversation agent + approved HA tools
               /                  \
  Local Ollama LLM server     Tools for Assist
      (RTX 3060)             (weather / web search)
                     |
      Home Assistant entities / automations
```

**Julia Voice's TV app does not run Ollama**, query web-search providers directly, or store Ollama configuration. That separation makes it possible to reuse the same Home Assistant intelligence from other microphones, phones, and voice satellites.

**Permissions and expectations:** Expose only approved HA entities and actions to a conversational agent, avoid unrestricted shell/network access, and use confirmation for sensitive actions. Web search sends search queries to the configured provider. Ollama can stay local even though weather/search retrieval uses external services.

## Screenshots and appearance

The first device tests used a compact blue full-screen UI and a blue floating overlay. **Glass** (dark, animated waveform) and **Classic** are selectable as of v0.5, but actual Glass skin device photos haven't been captured yet.

- **Working voice conversation:** the TV successfully showed “What time is it?” and Home Assistant's “5:07 AM” reply; voice playback through TV speakers was confirmed.
- **Floating overlay:** the TV showed the Julia Voice panel above Google TV Home, with TALK / STOP / CLOSE and an authenticated connection.

The image previews below are **illustrative reconstructions based on those actual TV tests**, not photographs of v0.5/v0.6. Real cropped device photos are available to replace the previews when added as repository image assets.

| Assist conversation preview | Floating overlay preview |
| --- | --- |
| ![Illustrated Julia Voice full-screen conversation showing question and answer](docs/screenshots/conversation-preview.svg) | ![Illustrated original floating Julia Voice panel on the Google TV launcher](docs/screenshots/overlay-preview.svg) |

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
| Dedicated **Julia Voice Overlay** launcher or main-app ⭐ fallback | 🔶 Built, mapping depends on onn firmware |
| GitHub Release update check, download, checksum verification and Android installer | 🔶 v0.6 builds successfully; device test pending |
| HA media_player announcements, Frigate person-triggered PiP, dashboard on request | 📋 Planned |

The **Google voice button currently opens Google's assistant**, not this app. A regular remote can control TALK / STOP, and the onn box microphone has produced working STT audio. We are **not claiming access to the Bluetooth remote microphone**.

## Download and install

**Get the current public signed APK:** [**Julia Voice v0.6**](https://github.com/chadakenn/Julia-Voice/releases/tag/v0.6) · [**Direct APK download**](https://github.com/chadakenn/Julia-Voice/releases/download/v0.6/app-release.apk).

1. Download `app-release.apk` and install it using **Send Files to TV**, USB, or ADB (one-time migration to the update-enabled build). Allow Android's installation prompt.
2. If you're **already using an APK signed by our permanent release key (v0.3 or newer)**, **install over it; do not uninstall**. The HA URL and encrypted token should remain saved.
3. **Only for an original debug-signed diagnostic APK** with a different certificate: uninstall the debug version once and then install the signed release; you'll need to re-enter the HA token.
4. From v0.6 onward, use **SETTINGS → Check for app updates**. The app can download a newer public signed release and open the standard Android installer. Android installation approval may still be required.

GitHub Releases are intentionally published only after an owner-approved workflow run. New builds and updater behavior should be tested on actual Google TV hardware; don't interpret a green CI build as proof of a completed on-device update.

📘 [In-app updates and first v0.6 install](docs/UPDATES.md) · [Full installation / migration guide](docs/INSTALL.md) · [Signing and key backup](docs/SIGNING.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)

## First Home Assistant conversation

Prerequisites: a reachable Home Assistant server, a working **Assist pipeline** with STT/conversation agent (and TTS if you want spoken answers), and a **long-lived access token**.

1. Open **Julia Voice**, then **SETTINGS → Configure Home Assistant**. Enter HA URL and token; choose **Save and connect**. Google TV's phone remote keyboard can help, but clipboard paste varies.
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

1. **Now (v0.7 development / v0.6 published):** test the signed v0.6 updater, then verify Julia Voice launcher icon, banner and splash on v0.7. The public v0.6 Release is available; on-device update installation still needs testing and requires a newer release. [Updater guide](docs/UPDATES.md). Continue v0.5 Glass/⭐ testing as desired.
2. **Next:** refine ⭐ shortcut selection where firmware permits, overlay placement, audio focus, ambient-TV-noise tolerance, skins and recovery based on actual results.
3. **Afterward:** dedicated HA `media_player` with automation-driven chimes/TTS (e.g. “The washer is done”); secure companion channel.
4. **Then:** Frigate/go2rtc camera views, filtered doorbell-person PiP and optional spoken alerts; on-demand dashboards.
5. **Later:** optional wake word, timers, rich conversations, multi-pipeline selection and `assist_satellite` support.

See [project vision](docs/PROJECT_VISION.md), [announcements and PiP design](docs/ANNOUNCEMENTS_AND_PIP.md), [remote-key options](docs/REMOTE_BUTTON_FALLBACKS.md), and [Ava / protocol research](docs/AVA_RESEARCH.md). Those describe **goals**, not shipped features.

## License

**GNU General Public License version 3 (GPL-3.0-only).** See the complete [LICENSE](LICENSE) file. You can use, modify and redistribute the code under GPLv3 terms. Distribution of derivative binaries generally requires making the corresponding source available under the license, including preserving notices. The software comes **without warranty**.

This implementation was written independently; [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration) (AGPL-3.0) and [Ava](https://github.com/brownard/Ava) (Apache-2.0) are **research references**, not incorporated source. Any future code reuse must comply with the original work's licensing and attribution. Third-party dependencies retain their respective licenses.
