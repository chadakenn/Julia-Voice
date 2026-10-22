# Real-device screenshot gallery 📸

**This folder does not yet contain confirmed captures of Kellie's Voice v0.16.0 announcements, Frigate camera windows or live video.** The existing SVG previews and `night-signal.png` are **illustrative/design assets**, not unaltered captures of the currently shipped Android application. Please do not label them "actual screenshot" in README, HACS, release notes, or marketing.

We want people browsing HACS to understand what Kellie's Voice looks like on a **real Android TV**, not a concept rendering. As soon as real captures are supplied, the main README and HACS description can show them with accurate captions.

## Requested captures

| Screenshot / photo | What it should show | Proposed file |
| --- | --- | --- |
| TV app home/settings | Real on-TV app showing name, connection status and settings, no secrets | `actual-tv-settings.png` |
| Voice overlay | Kellie's Voice overlay on Google TV / YouTube with a harmless Assist request and reply | `actual-voice-overlay.png` |
| Announcement popup | The opt-in on-screen text announcement above a TV app | `actual-announcement.png` |
| Frigate popup | A real camera **still snapshot** overlay, preferably driveway or doorbell | `actual-frigate-snapshot.png` |
| HA device settings | HA device page with overlay position, size, style, auto-hide and auto-listen | `actual-ha-device-controls.png` |
| Live camera, **moving playback verified with driveway and doorbell on onn 4K Pro v0.16.1** | Capture actual HLS video overlay (not a mockup); on-device screenshot has not yet been supplied | `actual-live-camera.png` |

## Easy way to capture and contribute

**Phone photos are okay.** Pause on each screen or use a 20–30 second test popup, photograph the TV square-on with minimal glare, and upload the originals in a GitHub issue or chat. A direct Android screenshot is even better if you already have ADB working. No rooting or ADB setup is required just to help with documentation.

If using an existing ADB connection, you can use `adb shell screencap -p /sdcard/kellies-voice.png`, then `adb pull /sdcard/kellies-voice.png`. Some protected video apps intentionally block or black out screenshots; in that case, a phone photo showing the popup is more honest and useful.

**Before putting images in a public repo**, remove or blur:
- Home Assistant URLs, logins, access tokens, TV pairing IDs, Wi-Fi and device details.
- Private camera views, house numbers, faces, street addresses, vehicle/license plate details, family photos and notifications.
- Streaming-account profiles or copyrighted TV-program imagery. A neutral TV background is ideal.

Cropping, straightening and redacting private details is fine; don't redraw the on-screen UI or imply an edited concept image is a device capture. Use meaningful ALT text such as "Real onn 4K Pro Frigate driveway snapshot popup over TV content." If showing a camera feed, state whether it is a **snapshot** or **live video**. Please get consent before publishing photos containing recognizable people.

## Gallery publication checklist

1. Verify the image was really taken from the running app on Android TV.
2. Record the tested app/integration version and TV model in the PR.
3. Keep user/private details out of the public image.
4. Add each verified image here and to the root README gallery with a truthful caption.
5. Update the HACS `info.md` and integration README to display/link the same gallery. Do not replace known working examples with illustrations.
6. Remove "capture needed" notes only for the specific screenshots we actually have.

## Illustrative design references (not device screenshots)

- [Old reconstructed Assist response](conversation-preview.svg)
- [Old reconstructed floating overlay](overlay-preview.svg)
- [Night Signal visual concept](night-signal.png)
- [Alternate Night Signal concept](night-signal-warm-preview.svg)

These remain available as design references but are deliberately separate from the public real-device gallery.
