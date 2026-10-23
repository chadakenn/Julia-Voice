# Optional one-press door unlock from a Kellie's Voice TV button

Kellie's Voice may show an **Unlock front door** button on a Home Assistant announcement or doorbell camera popup. Pressing **OK** on that button sends a Home Assistant event. **You decide whether that event runs `lock.unlock`.** A second confirmation is **not required** unless you choose to add one.

This is **opt-in**. No lock automation is created by HACS and no TV gets permission to unlock doors by installing the APK. An owner must configure a Home Assistant automation with a **specific TV pairing ID and specific lock entity**. Never include a PIN, OAuth token or camera stream URL in a TV action label or event.

## 1. Choose a TV and lock

In the TV app, find the stable Kellie's Voice **pairing ID**. It is the ID used by `kellies_voice.show_camera`, **not** the Home Assistant device-registry ID that Assist uses. In the examples, replace:

- `REPLACE_WITH_PAIRED_TV_ID` with the specific authorized TV's pairing ID.
- `camera.doorbell` with your actual camera, if showing the doorbell feed.
- `lock.front_door` with the precise lock you authorize.

Treat access to that physical TV remote, its signed-in Home Assistant account and your Home Assistant event bus as part of the security boundary. An integration/account that is allowed to emit arbitrary HA events could spoof a button event. **The announcement ID limits stale/accidental commands; it is not cryptographic authorization.** For locks, use a dedicated HA account with appropriately constrained permissions where available, keep HA authenticated, and enable the automation only if you accept this model.

## 2. Display the button

Send a camera popup from an explicit HA automation or Developer Tools action. The popup is never shown automatically by installing HACS:

```yaml
action: kellies_voice.show_camera
data:
  device_id: REPLACE_WITH_PAIRED_TV_ID
  entity_id: camera.doorbell
  title: Front door
  duration_seconds: 30
  speak: Someone is at the front door.
  actions:
    - id: unlock_front_door
      label: Unlock front door
    - id: dismiss
      label: Dismiss
```

This causes the TV to display a button. Pressing it publishes `kellies_voice_announcement_action` with the selected `action`, target `device_id`, and the **per-popup** 32-character `announcement_id`. The current unreleased button fix permits **one event per visible card** and then closes it. **Nothing unlocks until you deliberately add an HA automation.**

## 3. Recommended direct one-press setup (with short-lived popup matching)

This uses one text helper and two HA automations to ensure the incoming button corresponds to a **recent popup sent to that same TV**. This does **not** require another button press, voice confirmation or PIN.

Create a **Text** helper named **Kellie's Voice pending door action** (entity ID `input_text.kellies_voice_pending_door_action`; maximum length at least 48, such as 80). Start it empty.

**Automation A: remember only an eligible front-door popup.** Create an HA automation with the YAML below. It saves the random popup ID for at most the effective 40-second window.

```yaml
alias: Kellie's Voice arm front door button
triggers:
  - trigger: event
    event_type: kellies_voice_camera_popup
conditions:
  - condition: template
    value_template: >-
      {{ trigger.event.data.device_id == 'REPLACE_WITH_PAIRED_TV_ID'
         and (trigger.event.data.announcement_id | default('') | regex_match('^[0-9a-f]{32}$'))
         and ('unlock_front_door' in
              (trigger.event.data.actions | default([], true) | map(attribute='id') | list)) }}
actions:
  - action: input_text.set_value
    target:
      entity_id: input_text.kellies_voice_pending_door_action
    data:
      value: "{{ trigger.event.data.announcement_id }}|{{ as_timestamp(now()) | int }}"
mode: restart
```

**Automation B: unlock exactly once when the same popup button is pressed.** It checks the button name, the TV's pairing ID, the matching ID and the 40-second freshness window, **clears the ID first**, then unlocks only the selected lock.

```yaml
alias: Kellie's Voice front door button unlock
triggers:
  - trigger: event
    event_type: kellies_voice_announcement_action
    event_data:
      action: unlock_front_door
      device_id: REPLACE_WITH_PAIRED_TV_ID
conditions:
  - condition: template
    value_template: >-
      {% set parts = states('input_text.kellies_voice_pending_door_action').split('|') %}
      {% set age = as_timestamp(now()) - (parts[1] | int(0)) if parts | length == 2 else 999999 %}
      {{ parts | length == 2
         and trigger.event.data.announcement_id | default('') == parts[0]
         and age >= 0 and age <= 40 }}
actions:
  - action: input_text.set_value
    target:
      entity_id: input_text.kellies_voice_pending_door_action
    data:
      value: ""
  - if:
      - condition: state
        entity_id: lock.front_door
        state: locked
    then:
      - action: lock.unlock
        target:
          entity_id: lock.front_door
mode: single
```

Once you enable both automations, pressing **Unlock front door** on a matching popup immediately sends the unlock action through Home Assistant. Other cards, wrong TVs, mismatched or stale popup IDs, duplicate button events, and expired cards must not unlock the door. Note that Home Assistant administrators and integrations that can fire arbitrary events can potentially impersonate both the popup and the click. A separate trust or confirmation factor is an **optional** owner choice, not a requirement imposed by Kellie's Voice.

## 4. Simpler alternative (without popup-ID matching)

For less setup, you *can* trigger directly on `kellies_voice_announcement_action` where `action: unlock_front_door` and `device_id` matches the chosen TV, followed by `lock.unlock` on the selected entity. This is intentionally more permissive: any sufficiently privileged HA event publisher able to spoof that event can cause an unlock, including an old/replayed event. The recent-popup recipe above is preferable.

## 5. Test before enabling live unlocking

First replace the `lock.unlock` action in Automation B with `persistent_notification.create`, then press **Unlock** and verify one notification. Test wrong-TV events, timeout, double-click, Back, missing announcement IDs and a replaced popup; all should cause **no unlock**. Once satisfied, explicitly change the action to your actual lock, and test while you can physically observe the door. Some locks require an additional unlock code supplied through a credential-protected Home Assistant integration; do not embed that code in TV events or screenshots.

A button event merely asks HA to perform the action; Android does not know whether the lock succeeded unless you build a separate HA state/confirmation popup. Keep regular door access available in case Wi-Fi, HA or the lock is offline.

**Status:** This is unreleased PR documentation; the single-dispatch TV code has passed source review but still needs Android CI and real-remote tests. No lock action was configured in the owner's Home Assistant by drafting these examples.
