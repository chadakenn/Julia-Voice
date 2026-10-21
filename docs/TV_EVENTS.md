# Home Assistant TV announcements and camera snapshot popups (development)

This is an opt-in Android TV feature. It does **not** require installing the
custom Home Assistant integration to show a **local announcement preview**.
Live Home Assistant events require HA configuration in a later step.

## TV configuration

1. Install the signed development APK over the existing app (keep your saved login).
2. In Kellie's Voice, open **Settings → TV announcements & cameras**.
3. Turn it on. Android may require **Display over other apps** permission.
4. Choose **Preview announcement** for a local Night Signal popup. The box does
   **not** enable the microphone, pause/mute YouTube, or steal DPAD focus.
5. To disable, turn the same switch off or use **Disable alerts** from the
   persistent notification.

The opt-in switch defaults to **OFF**. When enabled, the service connects
to the same Home Assistant WebSocket URL and encrypted access token already
configured for Assist, subscribes to **only two** HA event types, and
reconnects after an interruption. It can be stopped by Android TV and is **not**
a guaranteed 24/7 listener; it does not auto-start on boot.

The Home Assistant token needs enough permissions to subscribe to custom
events (usually an administrator token). Never expose the token in
GitHub files, screenshots, automation YAML, or logs.

## Event contract

The companion Home Assistant integration can implement the
`kellies_voice.announce` and `kellies_voice.show_camera` services. The
Android side listens for the following raw **Home Assistant event bus** events:

### Visual announcement (text only in phase 1)

- Event type: `kellies_voice_announcement`
- Data:
  - `title`: optional title, up to 65 characters
  - `text`: required non-empty message, up to 500 characters
  - `duration_seconds`: optional integer, clamped to 4–45 seconds (default 10)
  - `device_id`: optional persisted Kellie's Voice device ID; omit for all TVs

Sample Developer Tools → Events action:

```yaml
event_type: kellies_voice_announcement
event_data:
  title: Laundry
  text: The washer has finished.
  duration_seconds: 9
```

### Camera snapshot popup

- Event type: `kellies_voice_camera_popup`
- Data:
  - `entity_id`: required Home Assistant entity such as `camera.driveway`
  - `title`: optional, up to 65 characters
  - `duration_seconds`: optional 4–45 seconds (default 10)
  - `device_id`: optional, omit for all TVs

Sample:

```yaml
event_type: kellies_voice_camera_popup
event_data:
  entity_id: camera.driveway
  title: Person at the driveway
  duration_seconds: 12
```

The TV fetches a **single still image** through the authenticated Home
Assistant `/api/camera_proxy/camera.driveway` endpoint. Camera stream/video
and spoken TTS announcements are planned separately, not implemented in this
phase. Images only load from the configured Home Assistant host; event data
cannot provide a URL. The card is a touch- and focus-transparent Android
overlay, so video apps and remote controls remain usable.

**Note:** Some Frigate entities may not expose a usable Home Assistant
camera snapshot. Use an HA `camera.*` entity that supports `camera_proxy`,
or the popup will show its text without an image.

## Developer test checklist

- Enabling the receiver never requests recording permission or starts audio.
- With YouTube playing, use Preview: popup appears and auto-dismisses.
- Disabling receiver removes foreground notification and stops its socket.
- HA fires announcement event: intended TV shows a card; other target TVs ignore it.
- HA fires camera event: valid camera snapshot appears without a second hostname.
- Disable Wi-Fi / restart HA: the event receiver reconnects when available.
- Unsupported entity names and oversized text are rejected or safely bounded.
- Verify Android 14+ special-use foreground-service behavior on the onn 4K Pro.

This feature is intentionally separate from Home Assistant layout control.
The latter is still optional and keeps its own setting.
