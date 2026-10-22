# Seven-day forecast on the TV

This is on demand. Saying the phrase, or running the action yourself, fetches the forecast and shows a card. Nothing in this guide creates a weekly automation.

The card is not a camera. It does not change TV audio, start the microphone, or move the voice overlay. If a camera popup is already up, the forecast waits until that camera closes.

## What to install

1. Update the **HACS** Kellie's Voice integration, then **restart Home Assistant**. HACS does not install the Android app.
2. Install the matching **signed Android APK** over the current app from the TV's update check after the owner publishes it. Do not uninstall the app. This document is ahead of the installed v0.16.3 release until that build exists.
3. On the TV, **Settings → TV announcements & cameras** must be ON, and **Display over other apps** must be allowed.
4. You need a Home Assistant **weather** entity that supports a **daily** forecast. Home Assistant 2024.4 or newer provides `weather.get_forecasts`, which this action uses. An entity that only has hourly data will be refused.

## Choose the weather source

On the Kellie's Voice device page, set **Forecast source** to your weather entity, such as `weather.forecast_home`.

**Forecast position**, **Forecast size**, and **Forecast duration** move only the weather card. The usual place is bottom center for 20 seconds. They do not move camera popups. Home Assistant layout must be ON on the TV before those three selects are adopted. Changing them does not show a forecast by itself.

## Script for Julia

Home Assistant → **Settings → Automations & scenes → Scripts → Create script → Edit in YAML**. Paste this, then replace the weather entity if you did not set Forecast source:

```yaml
alias: Seven day forecast on TV
description: Show the next seven days of weather on Kellie's Voice.
mode: single
sequence:
  - action: kellies_voice.show_forecast
    data:
      entity_id: weather.forecast_home
      duration_seconds: 20
```

Save it. Then **Settings → Voice assistants → Expose** and expose **Seven day forecast on TV** to Assist. Ask:

- What's the weather this week?
- Show the seven-day forecast.
- What's the forecast for the next seven days?

You can also run `kellies_voice.show_forecast` from **Developer tools → Actions**. Leave `device_id` empty to send it to every paired TV. The picture in the README is a labeled simulation until it is photographed on a TV.

The spoken answer still comes from your Assist agent. The card is the seven-day view. If the weather entity is missing, has no daily forecast, or Home Assistant cannot read it, the action fails and no card is shown.

Press **Back** on the remote to close the card early. It also closes itself when the duration ends.
