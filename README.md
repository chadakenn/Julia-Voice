<div align="center">
  <img src="docs/branding/kellies-voice-header.svg" alt="Kellie's Voice | Your home. Your voice. Your way." width="100%" />
</div>

# Kellie's Voice 🎙️🧡

**Your home. Your voice. Your way.**

Kellie's Voice is a local-first, open-source voice companion for Home Assistant, starting with Google TV and Android TV. Ask a question or control a room using the floating overlay, without replacing the TV launcher.

**Current status:** v0.10 copper-and-charcoal branding committed to the app source; its TV appearance still requires on-device verification. The earlier Kellie's Voice build successfully handled Home Assistant questions and the onn 4K Pro ⭐ floating overlay. Public v0.6 remains the last explicitly published GitHub Release. Normal pushes generate signed development APKs only.

## Download and upgrade safely

The signed Android package ID stays `com.example.tvvoiceprobe` and the signing key stays the same. Install the latest signed `kellies-voice-signed` GitHub Actions artifact **over the installed Kellie's Voice app**. **Do not uninstall** or you may lose the Home Assistant token and device settings.

**Repository URL:** Source currently lives at [chadakenn/Julia-Voice](https://github.com/chadakenn/Julia-Voice). The proposed `chadakenn/kellies-voice` GitHub repository rename is a separate owner action, to be completed after the upgraded TV devices have the new updater. See [the safe migration guide](docs/UPDATES.md).

## Appearance

- Charcoal, copper, warm ivory and rose-gold instead of blue or purple.
- New scalable GitHub header, Android launcher icon, TV launcher banner, animated waveform and startup splash.
- The welcome splash includes a **single small portrait of Kellie** ([brand portrait](docs/branding/kellie-portrait.webp)), while the floating TV overlay remains icon-only and unobtrusive. The photo-rich marketing comps are also available separately from the app.
- Built-in Home Assistant/Ollama conversation routing and saved voice preferences remain intact.

## onn ⭐ TV remote shortcut

If your TV's ⭐ app picker only lists Kellie's Voice, enable **SETTINGS → Overlay style and voice settings → ⭐ picker fallback**. The existing v0.9 transparent launch path should skip the full-screen app when permissions and the Home Assistant connection are ready. You must first grant microphone and Display over other apps permissions.

## What this project does

Your TV is the **voice front end**, not the AI server. Press the programmable ⭐ button (where the TV firmware allows app shortcuts), speak toward the onn box, and receive Home Assistant's answer as text and speech. The app sends requests through the Home Assistant Assist pipeline, so you can change the conversation agent or install new tools **without rebuilding the TV APK**.

**Already demonstrated on the onn 4K Pro:** authenticated HA Assist, recognized speech, a spoken answer through TV speakers, and a translucent overlay on Google TV Home. **Not yet verified on device:** v0.5 Glass/automatic voice flow and v0.6 automatic installation over other video apps.

## Our local AI and Home Assistant setup 🧠

We use a **self-hosted Ollama language-model server on our home network**, separate from the onn Android TV app and the Home Assistant host. Our earlier local Ollama setup used an **NVIDIA RTX 3060 (12 GB)** and tested the **Qwen3 8B** family; the exact currently selected model and Assist pipeline should be checked in Home Assistant rather than assumed.

On the **Home Assistant server**, we installed [**Tools for Assist (`llm_intents`)**](https://github.com/skye-harris/llm_intents) as a companion integration for **online search and weather**. The search and weather providers/tools must still be configured and made available to the selected conversation agent; installing `llm_intents` alone does not prove that every tool is working.

```text
Onn 4K Pro / Kellie's Voice TV app (mic, overlay, TTS playback)
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

**Kellie's Voice's TV app does not run Ollama**, query web-search providers directly, or store Ollama configuration. That separation makes it possible to reuse the same Home Assistant intelligence from other microphones, phones, and voice satellites.

**Permissions and expectations:** Expose only approved HA entities and actions to a conversational agent, avoid unrestricted shell/network access, and use confirmation for sensitive actions. Web search sends search queries to the configured provider. Ollama can stay local even though weather/search retrieval uses external services.

## Screenshots and appearance

The first device tests used a compact blue full-screen UI and a blue floating overlay. **Glass** (dark, animated waveform) and **Classic** are selectable as of v0.5, but actual Glass skin device photos haven't been captured yet.

- **Working voice conversation:** the TV successfully showed “What time is it?” and Home Assistant's “5:07 AM” reply; voice playback through TV speakers was confirmed.
- **Floating overlay:** the TV showed the Kellie's Voice panel above Google TV Home, with TALK / STOP / CLOSE and an authenticated connection.

The image previews below are **illustrative reconstructions based on those actual TV tests**, not photographs of v0.5/v0.6. Real cropped device photos are available to replace the previews when added as repository image assets.

| Assist conversation preview | Floating overlay preview |
| --- | --- |
| ![Illustrated Kellie's Voice full-screen conversation showing question and answer](docs/screenshots/conversation-preview.svg) | ![Illustrated original floating Kellie's Voice panel on the Google TV launcher](docs/screenshots/overlay-preview.svg) |

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
| Dedicated **Kellie's Voice Overlay** launcher or main-app ⭐ fallback | 🔶 Built, mapping depends on onn firmware |
| GitHub Release update check, download, checksum verification and Android installer | 🔶 v0.6 builds successfully; device test pending |
| HA media_player announcements, Frigate person-triggered PiP, dashboard on request | 📋 Planned |

The **Google voice button currently opens Google's assistant**, not this app. A regular remote can control TALK / STOP, and the onn box microphone has produced working STT audio. We are **not claiming access to the Bluetooth remote microphone**.

## Download and install

**Get the current public signed APK:** [**Kellie's Voice v0.6**](https://github.com/chadakenn/Julia-Voice/releases/tag/v0.6) · [**Direct APK download**](https://github.com/chadakenn/Julia-Voice/releases/download/v0.6/app-release.apk).

1. Download `app-release.apk` and install it using **Send Files to TV**, USB, or ADB (one-time migration to the update-enabled build). Allow Android's installation prompt.
2. If you're **already using an APK signed by our permanent release key (v0.3 or newer)**, **install over it; do not uninstall**. The HA URL and encrypted token should remain saved.
3. **Only for an original debug-signed diagnostic APK** with a different certificate: uninstall the debug version once and then install the signed release; you'll need to re-enter the HA token.
4. From v0.6 onward, use **SETTINGS → Check for app updates**. The app can download a newer public signed release and open the standard Android installer. Android installation approval may still be required.

GitHub Releases are intentionally published only after an owner-approved workflow run. New builds and updater behavior should be tested on actual Google TV hardware; don't interpret a green CI build as proof of a completed on-device update.

📘 [In-app updates and first v0.6 install](docs/UPDATES.md) · [Full installation / migration guide](docs/INSTALL.md) · [Signing and key backup](docs/SIGNING.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)

## First Home Assistant conversation

Prerequisites: a reachable Home Assistant server, a working **Assist pipeline** with STT/conversation agent (and TTS if you want spoken answers), and a **long-lived access token**.

1. Open **Kellie's Voice**, then **SETTINGS → Configure Home Assistant**. Enter HA URL and token; choose **Save and connect**. Google TV's phone remote keyboard can help, but clipboard paste varies.
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

1. **Now (v0.7 development / v0.6 published):** test v0.10's warm branding and signed update bridge. The public v0.6 Release is available; on-device update installation still needs testing and requires a newer release. [Updater guide](docs/UPDATES.md). Continue v0.5 Glass/⭐ testing as desired.
2. **Next:** refine ⭐ shortcut selection where firmware permits, overlay placement, audio focus, ambient-TV-noise tolerance, skins and recovery based on actual results.
3. **Afterward:** dedicated HA `media_player` with automation-driven chimes/TTS (e.g. “The washer is done”); secure companion channel.
4. **Then:** Frigate/go2rtc camera views, filtered doorbell-person PiP and optional spoken alerts; on-demand dashboards.
5. **Later:** optional wake word, timers, rich conversations, multi-pipeline selection and `assist_satellite` support.

See [project vision](docs/PROJECT_VISION.md), [announcements and PiP design](docs/ANNOUNCEMENTS_AND_PIP.md), [remote-key options](docs/REMOTE_BUTTON_FALLBACKS.md), and [Ava / protocol research](docs/AVA_RESEARCH.md). Those describe **goals**, not shipped features.

## License

**GNU General Public License version 3 (GPL-3.0-only).** See the complete [LICENSE](LICENSE) file. You can use, modify and redistribute the code under GPLv3 terms. Distribution of derivative binaries generally requires making the corresponding source available under the license, including preserving notices. The software comes **without warranty**.

This implementation was written independently; [Voice Satellite](https://github.com/jxlarrea/voice-satellite-card-integration) (AGPL-3.0) and [Ava](https://github.com/brownard/Ava) (Apache-2.0) are **research references**, not incorporated source. Any future code reuse must comply with the original work's licensing and attribution. Third-party dependencies retain their respective licenses.
