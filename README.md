<div align="center">
  <img src="docs/branding/kellies-voice-approved-banner.webp" alt="Kellie's Voice logo and branding banner" width="100%" />
</div>

# Kellie's Voice 🎙️📺

**Turn your Android TV into a Home Assistant voice companion, without turning your TV into a dashboard.**

Kellie's Voice is a native Android TV / Google TV app with a companion **Home Assistant custom integration**. Talk to your Home Assistant Assist agent, see useful answers in a floating overlay, receive quiet smart-home announcements, and pop up security-camera views **over what you're already watching** when Android allows it.

No replacement launcher, always-open browser tab, or separate AI server on the TV. Your existing Home Assistant setup does the thinking; Kellie's Voice handles the microphone, on-screen experience, and TV audio.

[**Get the Android TV app**](https://github.com/chadakenn/Kellies-Voice/releases/latest) · [**Install the HACS integration**](https://my.home-assistant.io/redirect/hacs_repository/?owner=chadakenn&repository=Kellies-Voice&category=integration) · [**Setup guide**](docs/INSTALL.md) · [**Report a problem**](https://github.com/chadakenn/Kellies-Voice/issues)

## Why would I use this?

Imagine you're watching YouTube and want to turn on the garage lights. Instead of finding your phone or opening a dashboard, launch the Kellie's Voice overlay, ask Home Assistant, and keep watching. A few other examples:

| You want to… | Kellie's Voice can… |
| --- | --- |
| Ask "Turn off the kitchen lights" or "What's the weather?" | Send your question to your configured Home Assistant Assist pipeline, then show and speak the response. Actual answers and device control depend on that pipeline and its tools. |
| Know when laundry finishes | Show a brief text announcement from a Home Assistant automation. |
| See who's approaching the front door | Display a short **camera snapshot popup** when a Frigate person sensor triggers. |
| Check the driveway on demand | Request an **experimental muted live camera overlay**, without leaving the current TV app. Moving live video **verified with driveway and doorbell cameras on onn 4K Pro using v0.16.1**. Other devices still need testing. |
| Move the assistant out of the way | Set voice-overlay position, size, appearance, auto-hide, and auto-listen from Home Assistant. |
| Stay current | Check for a signed Android app update right inside Kellie's Voice. HACS updates its *separate* Home Assistant component. |

**A key distinction:** automatic Frigate alerts currently show **still snapshots**, not live video. Live HLS streaming is a separate, **on-demand** feature, confirmed playing moving driveway **and doorbell** video on the onn 4K Pro in Android v0.16.1 (HA integration v0.16.0). Other cameras/devices remain unverified. Camera windows are Android overlays, not native system picture-in-picture. Announcements are **on-screen text**, not spoken TTS broadcasts.

## See it in action 📸

The project's brand banner above is promotional artwork, **not a screenshot**. We're assembling a gallery of **actual onn 4K Pro captures** showing:

- The Kellie's Voice settings screen and the floating voice overlay.
- An on-screen Home Assistant announcement while video plays behind it.
- A Frigate driveway or doorbell snapshot popup.
- The **real live driveway and doorbell camera overlays** (moving video confirmed with v0.16.1). Screenshot captures pending.
- The Kellie's Voice device controls in Home Assistant.

**Real-device captures are being collected.** We won't label design mockups as actual app screenshots. See the [real screenshot capture checklist](docs/screenshots/README.md) if you'd like to contribute photos or ADB captures. The older [illustrative concept previews](docs/screenshots/conversation-preview.svg) remain available as design references, not proof of real-device appearance.

## How it works

```text
Android TV / Google TV
  Kellie's Voice (microphone, popup overlays, on-TV speech)
           │ authenticated Home Assistant connection
           ▼
     Home Assistant
  Assist pipeline + your chosen conversation agent
  Devices, automations, Frigate cameras
           ▲
  Kellie's Voice HACS integration
  TV settings + announce / snapshot / experimental live actions
```

Kellie's Voice uses **your existing Home Assistant Assist pipeline**. It works with the conversation agent and speech components you configure in Home Assistant. Local Ollama/LLM agents are optional, not required, and aren't installed by the Android APK.

## Get started: install both parts

The Android app and the HACS integration are **two different downloads**. You need both for Home Assistant-controlled overlay settings and camera announcements.

### 1. Install the Android TV app

1. On a supported Android TV / Google TV device, install the **signed APK** from [the latest GitHub Release](https://github.com/chadakenn/Kellies-Voice/releases/latest).
2. Open Kellie's Voice and configure its Home Assistant URL, access token, microphone and **Display over other apps** permissions.
3. Test a question through your configured Home Assistant Assist pipeline. Use the on-screen controls or the TV shortcut supported by your device.
4. In **Settings → Home Assistant layout**, turn remote layout management **ON**. To get announcements and camera popups, independently enable **Settings → TV announcements & cameras**.
5. Later updates: use **Settings → Check for app updates** and confirm Android's installer. Never uninstall the app just to update, or you may lose your saved connection.

### 2. Install the Home Assistant integration via HACS

[![Open Kellie's Voice in HACS](https://my.home-assistant.io/badges/hacs_repository.svg)](https://my.home-assistant.io/redirect/hacs_repository/?owner=chadakenn&repository=Kellies-Voice&category=integration)

1. If this is your first installation: **HACS → ⋮ → Custom repositories → Add**, paste `https://github.com/chadakenn/Kellies-Voice` and select **Integration**.
2. Open Kellie's Voice in HACS and download **v0.16.0 or later**. Restart Home Assistant.
3. Go to **Settings → Devices & services**, choose the discovered Kellie's Voice TV and add it. If mDNS discovery doesn't work, select **Add Integration → Kellie's Voice** and enter the ID shown under the TV app's **Settings → Home Assistant layout → Show this TV's pairing ID**.
4. Open the registered TV device in Home Assistant. Configure voice-overlay position, size, style, auto-hide and auto-listen. You can now call the actions below.

**HACS does not install or update the Android APK.** The in-app updater does not update your HACS integration. Install matching versions of both for new features. [Full integration documentation](custom_components/kellies_voice/README.md).

## What can Home Assistant send to the TV?

<details>
<summary><strong>Example: show an announcement</strong></summary>

```yaml
action: kellies_voice.announce
data:
  title: Laundry
  text: The washer has finished.
  duration_seconds: 10
```

</details>

<details>
<summary><strong>Example: show a Frigate snapshot</strong></summary>

```yaml
action: kellies_voice.show_camera
data:
  entity_id: camera.doorbell
  title: Someone is at the door
  duration_seconds: 15
```

Use an existing Home Assistant `camera.*` entity. A Frigate person-detection automation or [included blueprint](custom_components/kellies_voice/blueprints/frigate_person_popup.yaml) can trigger this for you. The popup is a still image; camera setup and person-detection sensors come from your HA/Frigate configuration.

</details>

<details>
<summary><strong>Example: request live video (experimental)</strong></summary>

```yaml
action: kellies_voice.show_live_camera
data:
  entity_id: camera.driveway
  title: Driveway live
  duration_seconds: 90
```

Requires a streaming-capable HA camera and matching v0.16.0 or newer Android/HA components. This uses a muted, temporary HLS player and **has been verified with moving driveway and doorbell video on the onn 4K Pro using Android v0.16.1**; this doesn't yet establish support for every camera or Android TV model. For Assist/Ollama voice requests, expose separate scripts for authorized cameras and ensure your conversation agent can call Home Assistant scripts. Voice invocation still requires a speaking test.

</details>

Leaving **TV device ID** blank sends an announcement or camera action to all configured Kellie's Voice TVs. Set that field when you have multiple TVs and want just one.

See [TV event setup](docs/TV_EVENTS.md), [HA service details](custom_components/kellies_voice/README.md), and [camera/announcement design notes](docs/ANNOUNCEMENTS_AND_PIP.md).

## Compatibility and important limits

| Capability | Status |
| --- | --- |
| Home Assistant voice conversation and TV speech | **Tested** on onn 4K Pro |
| Floating overlay, remote scrolling, YouTube audio behavior | **Tested** on onn 4K Pro |
| HA changing the assistant overlay position | **Tested** on onn 4K Pro |
| Targeted text announcements over TV | **Tested** on onn 4K Pro |
| Frigate still-image camera popups | **Tested** with driveway camera on onn 4K Pro |
| Person-detection automations for driveway and doorbell | **Configured** in HA; natural detection end-to-end test pending |
| Live HLS camera overlay | **Verified** moving driveway and doorbell video on onn 4K Pro with Android v0.16.1; other cameras and TV devices still need testing |
| Other Google TV and Android TV brands | **Not comprehensively tested** |

Kellie's Voice uses a user-controlled microphone; it **does not intercept the Google Assistant voice button** or promise access to the remote's Bluetooth microphone. Some streaming apps or protected content may limit overlays. The TV event listener is opt-in and Android may stop it while suspended; delivery is not guaranteed 24/7. No external ports need to be opened on the TV for Home Assistant events.

For problems, see [Troubleshooting](docs/TROUBLESHOOTING.md), [Update guide](docs/UPDATES.md), [Known testing procedures](docs/TESTING.md), [Privacy and security guidance](SECURITY.md) and [project vision](docs/PROJECT_VISION.md).

## Project status and contributing

**Current Android version: v0.16.2 (Home Assistant integration v0.16.2).** Voice, announcement cards, Frigate snapshots and moving live driveway and doorbell video have been confirmed on the onn 4K Pro. Other camera streams and wider TV compatibility remain under test. Issues and pull requests are welcome, especially **real, redacted screenshots**, hardware results and reproducible problems.

Kellie's Voice is a community-built Home Assistant companion, **not an official Home Assistant or HACS product**.

## Close a camera by voice (v0.16.2)

Kellie's Voice adds a dedicated `kellies_voice.hide_camera` action to **dismiss a live or snapshot camera popup immediately**. It does not turn off video playback from your other TV apps, dismiss text announcements, or operate the microphone. Configure an Assist-exposed script named **Close camera on TV**, then say **“Julia, close the camera on the TV.”** You can also call the HA action directly:

```yaml
action: kellies_voice.hide_camera
data:
  # Leave empty for all configured TVs, or select one paired TV:
  device_id: "your-kellies-voice-tv-id"
```

`show_live_camera` already supports **15–180 second** durations. Expose camera scripts with a `duration_seconds` field if your Assist agent can pass script arguments; a named 30-second camera script is a reliable alternative for simple speech commands. **Voice recognition of these new commands must be checked on your chosen conversation agent**. Both the v0.16.2 Android app **and** v0.16.2 HACS integration are required for the hide action.

### Copy-and-paste Home Assistant script setup

The Android app alone cannot make Julia understand new camera phrases. **Create and expose the corresponding Home Assistant scripts**. Detailed full YAML for driveway, doorbell, 30-second requests, instant close, multi-TV targeting, and Frigate automations is in the **[Home Assistant voice-camera script setup guide](docs/VOICE_CAMERA_SCRIPTS.md)**.

**Example: Close camera on TV** (Home Assistant → Settings → Automations & scenes → Scripts → Create script → Edit in YAML):

```yaml
alias: Close camera on TV
description: >
  When asked to close or hide the camera, dismiss the current
  Kellie's Voice camera window without stopping recordings.
icon: mdi:close-box
mode: single
sequence:
  - action: kellies_voice.hide_camera
    data: {}
```

**Example: Show doorbell for 30 seconds** (create as a second script):

```yaml
alias: Show doorbell camera for 30 seconds on TV
description: >
  Open the live doorbell camera on the TV for 30 seconds
  when the user asks for a 30-second view.
icon: mdi:cctv
mode: single
sequence:
  - action: kellies_voice.show_live_camera
    data:
      entity_id: camera.doorbell
      title: Live doorbell
      duration_seconds: 30
```

Substitute your own `camera.*` entity IDs. On Home Assistant's **Settings → Voice assistants → Expose** screen, expose these scripts to Assist. If multiple TVs are paired, add `device_id` inside each action's `data` to target just one. Both the TV app and HACS integration must be at **v0.16.2 or later**, followed by a Home Assistant restart and reopening the TV app so its receiver reconnects. Voice phrases depend on the selected Assist conversation agent.
