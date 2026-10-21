# Kellie's Voice for Home Assistant

This integration changes overlay layout and can send announcement and camera events to configured TVs. It does not start the microphone, replace the TV login, or install an APK.

Home Assistant never connects to the TV for layout changes. The TV advertises itself, you click **Add**, and the TV reads those controls with the Home Assistant login it already has.

Announcement and camera actions stay inside Home Assistant too. They fire events. They do not open a port on the TV, and they do not put tokens or stream passwords in the event.

## Add it

1. Copy this folder to `config/custom_components/kellies_voice` on the Home Assistant machine. Include `services.yaml`, `services.py`, and the `blueprints` folder.
2. Restart Home Assistant.
3. On the TV, open **Kellie's Voice → Settings → Home Assistant layout** and turn it on. Leave the app open.
4. In Home Assistant, the TV shows up as a discovered device. Click **Add**.

The device page then has position, size, style, auto-hide, and auto-listen.

The first time the TV sees a new control, it copies the layout already saved on the TV into Home Assistant. After that, changes in Home Assistant win. The on-TV menu still works and updates Home Assistant too.

If the app is closed, a layout change waits until the next time Kellie's Voice connects. Turning the switch off on the TV stops discovery and ignores later Home Assistant layout changes.

The device id used by the services below is the id stored for that TV. It is the same id the TV advertised when you clicked **Add**. It is not a Home Assistant token.

## Announcements and camera popups

These actions fire Home Assistant events. The current signed Android development build includes an opt-in TV event receiver. On the TV, enable **Settings → TV announcements & cameras** to display them. You can also test the Home Assistant events using Developer Tools before turning on the receiver.

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

The TV displays the message or snapshot when its **TV announcements & cameras** receiver is enabled. This is a still-image popup, not live Frigate video. It does not start Kellie's microphone or speak the notification.

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
