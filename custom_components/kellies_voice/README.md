# Kellie's Voice for Home Assistant

**Give your TV a voice, useful popups, and quick access to security-camera views without replacing your TV launcher.** Kellie's Voice consists of a native Android TV/Google TV app and this Home Assistant integration. The TV handles speech and on-screen overlays; the integration lets HA automations and controls talk to it.

**Why it matters:** ask your Assist agent about the house while a show plays, get a text alert when the washing machine finishes, or see a Frigate driveway/doorbell snapshot when a person approaches. On-demand live video is available as an **experimental** option; moving driveway **and doorbell** video is now confirmed on the onn 4K Pro with Android v0.16.1, though other cameras/devices need verification.

| User-facing feature | Home Assistant feature |
| --- | --- |
| Move the floating voice overlay | Per-TV position, size, style, auto-hide and auto-listen entities |
| Show a quiet on-TV notification | `kellies_voice.announce` |
| See a Frigate camera still when someone appears | `kellies_voice.show_camera`, plus included person-detection blueprint |
| Request a temporary live camera feed | `kellies_voice.show_live_camera` (experimental HLS, muted) |

**You also need the [signed Android TV app](https://github.com/chadakenn/Kellies-Voice/releases/latest).** HACS installs only this integration. Enable **Home Assistant layout** in the TV settings and separately enable **TV announcements & cameras** for notifications and camera popups.

**Actual screenshots:** we're collecting real onn 4K Pro captures of the current app and overlays. Promotional artwork and legacy previews are not actual captures. See the [screenshot checklist](../../docs/screenshots/README.md).

This integration changes overlay layout and can send announcement and camera events to configured TVs. It does not start the microphone, replace the TV login, or install an APK.

Home Assistant never connects directly to the TV for layout changes. The TV can advertise through mDNS or you can enter its stable pairing ID manually. Either way, the TV reads those controls using its existing Home Assistant login.

Announcement and camera actions stay inside Home Assistant too. They fire events. They do not open a port on the TV, and they do not put tokens or stream passwords in the event.

## Install with HACS (recommended)

[![Open Kellie's Voice in HACS](https://my.home-assistant.io/badges/hacs_repository.svg)](https://my.home-assistant.io/redirect/hacs_repository/?owner=chadakenn&repository=Kellies-Voice&category=integration)

1. Make sure HACS is installed in your Home Assistant instance.
2. **First-time only:** in **HACS → ⋮ → Custom repositories**, add `https://github.com/chadakenn/Kellies-Voice` with category **Integration**. The shortcut badge does not register an unknown repository automatically.
3. Click **[Open in HACS](https://my.home-assistant.io/redirect/hacs_repository/?owner=chadakenn&repository=Kellies-Voice&category=integration)**. Download **v0.16.0** (or a newer release), not the older `v0.6`.
4. Restart Home Assistant, then turn **Home Assistant layout ON** inside the Kellie's Voice TV app. Either add the TV under **Settings → Devices & services → Discovered** or use **Add Integration → Kellie's Voice** for manual pairing with its device ID (shown on the TV under Settings → Home Assistant layout → Show this TV's pairing ID). This works if mDNS is blocked across VLANs.
5. Optionally turn on **TV announcements & cameras** on the TV to receive HA announcements and Frigate camera snapshots.

After custom repository registration, the badge opens Kellie's Voice for future HACS downloads/updates. The first registration and HA restart are still required. HACS installs only the Home Assistant component, not the TV APK.

## Manual installation

1. Copy this folder to `config/custom_components/kellies_voice` on the Home Assistant machine. Include `services.yaml`, `services.py`, and the `blueprints` folder.
2. Restart Home Assistant.
3. On the TV, open **Kellie's Voice → Settings → Home Assistant layout** and turn it on. Leave the app open.
4. In Home Assistant, add the TV under **Discovered**. If it doesn't appear, choose **Add Integration → Kellie's Voice**, then enter the TV's stable ID from **Settings → Home Assistant layout → Show this TV's pairing ID**. Use the exact ID; it is not your HA token.

The device page then has position, size, style, auto-hide, and auto-listen.

The first time the TV sees a new control, it copies the layout already saved on the TV into Home Assistant. After that, changes in Home Assistant win. The on-TV menu still works and updates Home Assistant too.

If the app is closed, a layout change waits until the next time Kellie's Voice connects. Turning the switch off on the TV stops discovery and ignores later Home Assistant layout changes.

The device id used by the services below is the id stored for that TV. It is the same id the TV advertised when you clicked **Add**. It is not a Home Assistant token.

## Announcements and camera popups

These actions fire Home Assistant events. The signed Android app includes an opt-in TV event receiver. On the TV, enable **Settings → TV announcements & cameras** to display them. You can also test the Home Assistant events using Developer Tools before turning on the receiver.

Leaving **TV device id** empty sends one event to every TV you have added. Filling it in sends the event only to that TV. A TV that is not added here is not a target.

Limits, on every call:

- Title: 1 to 65 characters
- Announcement text: 1 to 500 characters
- Duration: 4 to 45 seconds
- Camera: a real `camera` entity already in Home Assistant, never a URL

Nothing in these actions is written to the log except the event name and how many TVs were targeted.

### Announce

Action: `kellies_voice.announce`

Event: `kellies_voice_announcement`

```yaml
action: kellies_voice.announce
data:
  title: Washer
  text: The washer is done.
  duration_seconds: 10
```

The event data is exactly:

```json
{
  "title": "Washer",
  "text": "The washer is done.",
  "duration_seconds": 10,
  "device_id": "the-configured-tv-id"
}
```

### Show a camera

Action: `kellies_voice.show_camera`

Event: `kellies_voice_camera_popup`

```yaml
action: kellies_voice.show_camera
data:
  entity_id: camera.front_door
  title: Front door
  duration_seconds: 20
```

The event data is exactly:

```json
{
  "entity_id": "camera.front_door",
  "title": "Front door",
  "duration_seconds": 20,
  "device_id": "the-configured-tv-id"
}
```

`entity_id` is a Home Assistant camera entity. The event does not include a snapshot URL, an RTSP address, or a token.

### Test both

1. Add at least one TV with the steps above.
2. Go to **Developer tools → Events**. Start listening for `kellies_voice_announcement`.
3. Go to **Developer tools → Actions**. Run `kellies_voice.announce` with a short title and text.
4. Confirm one event per configured TV. Each event has `title`, `text`, `duration_seconds`, and that TV's `device_id`.
5. Listen for `kellies_voice_camera_popup` and run `kellies_voice.show_camera` with a camera that already exists, such as your Frigate camera.
6. Run them again with a **TV device id** and confirm only that TV is in the event.
7. Try a duration of `120`, an empty title, or `entity_id: light.kitchen`. Home Assistant should reject the call and fire no event.

The TV displays the message or snapshot when its **TV announcements & cameras** receiver is enabled. The `show_camera` action displays a **still-image snapshot**, not a live stream. The separate experimental `show_live_camera` action requests HLS video on demand. Neither action starts Kellie's microphone or speaks the notification.

### Frigate person detection

Frigate's Home Assistant integration exposes a person sensor and a camera, for example `binary_sensor.front_door_person_occupancy` and `camera.front_door`. Use your own entity ids.

Copy [`blueprints/frigate_person_popup.yaml`](blueprints/frigate_person_popup.yaml) to `config/blueprints/automation/kellies_voice/frigate_person_popup.yaml`, then reload automations. Create an automation from **Kellie's Voice Frigate person popup** and choose the person sensor, the camera, the title, and how long the popup stays up. Leave the TV device id blank for every configured TV.

The blueprint waits after each popup, so a person standing at the door does not open the camera again on every detection. The camera entity is an input, not a hard-coded camera.

The same action without the blueprint:

```yaml
alias: Front door person
mode: single
triggers:
  - trigger: state
    entity_id: binary_sensor.front_door_person_occupancy
    to: "on"
actions:
  - action: kellies_voice.show_camera
    data:
      entity_id: camera.front_door
      title: Someone is at the front door
      duration_seconds: 20
  - delay:
      seconds: 90
```

Replace both entity ids with yours. Add `device_id` only when you want one TV.

## On-demand live camera viewing (experimental)

The `kellies_voice.show_live_camera` action requests a **live HLS stream** from Home Assistant and displays a **muted, non-focusable overlay** on the TV. It does not cast away your movie or steal focus from YouTube. Default viewing time is 90 seconds (maximum 180). Person-detection alerts continue to use snapshots.

```yaml
action: kellies_voice.show_live_camera
data:
  entity_id: camera.driveway
  title: Live driveway
  duration_seconds: 90
```

For voice, expose an appropriately named script to your Assist/Ollama agent that calls the service with a fixed camera entity. Adding a new HA action alone does not guarantee your custom Julia agent recognizes "show driveway camera". A sentence-triggered HA automation may be used as a fallback.

The TV receiver requests a temporary HLS URL through Home Assistant's authenticated WebSocket `camera/stream` command. Events never carry stream URLs, tokens, or RTSP credentials. Playback is muted and released on timeout, a new popup, or receiver shutdown. HLS has more delay than WebRTC. Both the v0.16.0 or newer HACS integration and the matching signed Android APK with Media3 HLS support are needed. Moving live video has been confirmed on the onn 4K Pro with `camera.driveway` and `camera.doorbell` using Android v0.16.1. Other TV models and cameras have not been field-tested.
