# Home Assistant-managed TV configuration (future phase)

## What is implemented in v0.12

The TV has an on-device copper/charcoal settings menu, backed by a single validated `OverlayPreferences` adapter. Supported position values: `default`, `top_left`, `top_right`, `bottom_left`, `bottom_right`, `center`, and `bottom_center`. Supported sizes: `small`, `medium`, `large`. Supported spoken-reply auto-hide delays: `0` (never), `5`, `7` (legacy default), `10`, and `20` seconds. Existing auto-listen, skin, and onn ⭐ fallback settings are preserved.

These are **local settings only**. Changing them does **not** contact Home Assistant, and the current app does not expose an HTTP server or accept remote configuration commands.

## Proposed future HA dashboard

When ready, Home Assistant will provide one central screen to edit each TV's settings:

- Target TV (one or more devices)
- Floating overlay position and size
- Voice start/stop preferences and auto-hide time
- Announcement popups, their priority, duration and position
- Frigate camera popup layout, snapshot/live choice and routing
- Weather card position and size

An HA integration (or deliberately configured helpers/automations in an initial prototype) could provide selectors and publish **structured, authenticated settings messages** to a companion service on each TV. The helper names are not yet part of any public API.

**Suggested versioned configuration** (design only, not yet accepted by the app):

```json
{
  "schema": 1,
  "target": "living_room_tv",
  "overlay": {
    "position": "bottom_right",
    "size": "medium",
    "auto_hide_seconds": 10,
    "auto_listen": true
  }
}
```

### Safeguards

1. Only receive config from the already trusted Home Assistant connection; do not open a public local-network control port just for settings.
2. Require an explicit one-time TV owner opt-in and target a particular TV. A future HA configuration sender must not be able to turn on the microphone, start recording, bypass overlay permission, or install an APK merely by changing settings.
3. Validate keys/allowed values through the same `OverlayPreferences` setters used by the local UI; reject unknown keys and unsupported values.
4. Do not copy Home Assistant long-lived tokens into HA helpers, GitHub, logs, or dashboard state.
5. Save accepted preferences locally, so the TV starts with the last valid settings if HA is unavailable. Keep a **Reset local layout** path on the TV until remote configuration has been field-tested.
6. For push alerts or synced settings while no voice overlay is open, a future long-lived, low-power, authenticated HA listener must be designed separately from the opt-in microphone foreground service. No always-on mic and no silent camera stream.
7. Never show or speak Frigate camera images or announcements until the user enables them and chooses the cameras/automation sources.

### Suggested rollout

**Phase 1, v0.12:** local placement/size/timing and matching TV menu. **Phase 2:** announce messages in a TV overlay using an explicit HA-triggered event. **Phase 3:** Frigate snapshot alerts. **Phase 4:** weather cards. **Phase 5:** HA settings dashboard and authenticated device-targeted sync, after the event transport has been secured.

The existing TV settings stay as a backup until the Home Assistant management path works reliably. This document is a proposal, not an implementation claim.
