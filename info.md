# Kellie's Voice 🎙️📺

**Make your Android TV part of your Home Assistant experience without replacing what you're watching.**

Kellie's Voice pairs a **native Android TV / Google TV voice app** with this **HACS custom integration**. Ask Home Assistant questions through the TV, show useful answers in a floating overlay, and let automations display unobtrusive text announcements or Frigate camera snapshots.

### What this HACS integration adds
- **Per-TV controls:** move and resize the voice overlay, choose its look, auto-hide and auto-listen behavior.
- **Text announcements:** use `kellies_voice.announce` for laundry reminders, home status and other automations.
- **Camera popups:** use `kellies_voice.show_camera` to show an HA camera snapshot over the TV. Trigger from Frigate person detection for driveway and doorbell alerts.
- **Experimental live view:** use `kellies_voice.show_live_camera` for a muted temporary HLS overlay on request. Moving **driveway and doorbell** HLS video is verified on the onn 4K Pro running Android v0.16.1; other cameras and devices need testing.

**Important:** HACS installs only the Home Assistant integration. You must separately install the [signed Kellie's Voice Android TV APK](https://github.com/chadakenn/Kellies-Voice/releases/latest) and enable **Home Assistant layout** and, for alerts, **TV announcements & cameras** in its settings.

### Quick setup
1. Add `https://github.com/chadakenn/Kellies-Voice` as an **Integration** in **HACS → Custom repositories** if HACS doesn't already know this repo.
2. Download **v0.16.0 or newer** and restart Home Assistant.
3. In **Settings → Devices & services**, add the discovered Kellie's Voice TV, or choose manual pairing with the stable ID shown in the TV app.
4. Open the TV's HA device page to configure the overlay and use the actions above.

**Screenshots:** We're collecting verified real-device photos of the app, announcement cards and Frigate popups. The brand banner and older illustrated previews are **concept graphics**, not real captures. [Real screenshot gallery plan](https://github.com/chadakenn/Kellies-Voice/blob/main/docs/screenshots/README.md).

[**Full overview and examples**](https://github.com/chadakenn/Kellies-Voice#readme) · [**Detailed integration guide**](https://github.com/chadakenn/Kellies-Voice/blob/main/custom_components/kellies_voice/README.md) · [**Releases**](https://github.com/chadakenn/Kellies-Voice/releases/latest)

> Built and verified primarily on the onn 4K Pro. Other Android TV models and additional camera streams are not yet hardware-tested. This is a community project, not an official Home Assistant or HACS product.
