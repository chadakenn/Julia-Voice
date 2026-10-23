# Natural TV timers without a wake word (Home Assistant)

**Julia is the maintainer's personal name for their Home Assistant assistant.** Kellie's Voice is the TV app and HACS integration; you can use any Assist conversation agent and any name you prefer. The assistant's name is not a wake word. [Full naming explanation](../README.md#kellies-voice-vs-julia-whats-the-difference).

You **do not need to say "Julia"**, or any assistant name, to operate Kellie's Voice. Until a user enables a separate wake-word feature, activate listening with the TV app's controls and speak naturally:

- "Set a pizza timer for five minutes."
- "Set a timer for five minutes." (no name: the TV timer is named `Timer`)
- "Start a Laundry timer for 30 minutes."
- "Set a Rice timer for one hour."
- "Cancel the Laundry timer."
- "Stop." or "Cancel." (acts on the most recently started TV timer while timer context is active)

This configuration sends authenticated Home Assistant actions to Kellie's Voice's **own on-screen countdowns**, not Home Assistant's native Assist satellite timer API. It needs the matching Kellie's Voice v0.16.4 (or later) Android/HACS components, an opted-in TV event receiver and overlay permission. Home Assistant's `TimersNotSupportedError: device_id=None` is the *native timer* handler rejecting a client without timer capability; the Kellie's Voice action itself can still work.

**Multi-satellite safety:** Native Home Assistant sentence triggers are recognized globally, before normal intents. Until [PR #50](https://github.com/chadakenn/Kellies-Voice/pull/50) ships, v0.16.4 TV runs do **not** identify their originating HA device; `trigger.device_id` is null. The temporary `is none or ...` guard below allows this TV but can also catch *other clients without a device ID*. **Only opt into unprefixed sentence interception if this is acceptable in your HA setup.** Don't ship these globally to everyone's HA automatically. After PR #50, learn the correct HA *device-registry* ID and remove the `is none or` fallback to scope to the TV. Simply supplying a device ID does not by itself make the native Assist timer API support this app; see [issue #57](https://github.com/chadakenn/Kellies-Voice/issues/57).

## 1. Two helpers

Create **Settings → Devices & services → Helpers** → **Text** named `Kellies Voice Last TV Timer` (entity `input_text.kellies_voice_last_tv_timer`, maximum length 40) and **Toggle** named `Kellies Voice Timer Context` (entity `input_boolean.kellies_voice_timer_context`). They track the last timer name and whether a bare "stop" or "cancel" is allowed. Confirm actual entity IDs and adjust the examples if Home Assistant assigns something else.

Replace both placeholders:
- `REPLACE_WITH_KELLIES_PAIRING_ID` is the **Kellie's Voice pairing UUID** in the Android TV settings, used by `kellies_voice.start_timer` / `cancel_timer`.
- `REPLACE_WITH_HA_REGISTRY_DEVICE_ID` is the **32-character Home Assistant device-registry ID** of the TV from Settings → Devices & services → Kellie's Voice → Device. It is *not* the pairing UUID. With current v0.16.4, the TV passes no device ID, so the temporary null clause is required, but it cannot distinguish other null-ID devices.

## 2. Start named or unnamed TV timers (automation)

Create a new automation, switch to **Edit in YAML**, and paste:

```yaml
alias: Kellies Voice start TV timer by voice
description: Natural named/unnamed TV timers. Temporary null-device routing for v0.16.4 only.
triggers:
  - trigger: conversation
    command:
      - "set a {1..1440:minutes} minute {countdown_label} timer"
      - "set a {countdown_label} timer for {1..1440:minutes} minutes"
      - "set a {countdown_label} timer for {1..1440:minutes} minute"
      - "start a {countdown_label} timer for {1..1440:minutes} minutes"
      - "start a {1..1440:minutes} minute {countdown_label} timer"
      - "start a {countdown_label} countdown for {1..1440:minutes} minutes"
      - "set a {countdown_label} countdown for {1..1440:minutes} minutes"
      - "set a {1..1440:minutes} minute timer"
      - "set a timer for {1..1440:minutes} minutes"
      - "set a timer for {1..1440:minutes} minute"
      - "set timer for {1..1440:minutes} minutes"
      - "start a {1..1440:minutes} minute timer"
      - "start a timer for {1..1440:minutes} minutes"
      - "start a {1..1440:minutes} minute countdown"
      - "set a {countdown_label} timer for {1..86400:seconds} seconds"
      - "set a timer for {1..86400:seconds} seconds"
      - "set a {1..86400:seconds} second timer"
      - "set a {countdown_label} timer for {1..24:hours} hours"
      - "set a {countdown_label} timer for {1..24:hours} hour"
      - "set a timer for {1..24:hours} hours"
      - "set a timer for {1..24:hours} hour"
      - "set a {1..24:hours} hour timer"
conditions:
  - condition: template
    value_template: >-
      {{ trigger.device_id is none
         or trigger.device_id == 'REPLACE_WITH_HA_REGISTRY_DEVICE_ID' }}
actions:
  - variables:
      tv_timer_name: "{{ trigger.slots.countdown_label | default('Timer', true) | trim }}"
      tv_timer_seconds: >-
        {{ (trigger.slots.minutes | default(0) | int(0) * 60)
           + (trigger.slots.seconds | default(0) | int(0))
           + (trigger.slots.hours | default(0) | int(0) * 3600) }}
  - action: kellies_voice.start_timer
    data:
      device_id: REPLACE_WITH_KELLIES_PAIRING_ID
      name: "{{ tv_timer_name }}"
      duration_seconds: "{{ tv_timer_seconds }}"
      speak_alert: false
  - action: input_text.set_value
    target:
      entity_id: input_text.kellies_voice_last_tv_timer
    data:
      value: "{{ tv_timer_name }}"
  - action: input_boolean.turn_on
    target:
      entity_id: input_boolean.kellies_voice_timer_context
  - set_conversation_response: >-
      Sent {{ 'an unnamed timer' if tv_timer_name == 'Timer'
              else 'the ' ~ tv_timer_name ~ ' timer' }} to the TV.
mode: queued
max: 8
```

The range matcher supports spoken numbers such as "five" as well as digits. Adding the pairing ID targets **one TV**, never all TVs by default. The HA action confirms only that the event was sent; it cannot prove the TV received it.

## 3. Cancel a timer by name

```yaml
alias: Kellies Voice cancel named TV timer
triggers:
  - trigger: conversation
    command:
      - "cancel the {countdown_label} timer"
      - "cancel {countdown_label} timer"
      - "stop the {countdown_label} timer"
      - "stop {countdown_label} timer"
      - "cancel the {countdown_label} countdown"
      - "stop the {countdown_label} countdown"
      - "cancel {countdown_label} countdown"
      - "stop {countdown_label} countdown"
conditions:
  - condition: template
    value_template: >-
      {{ trigger.device_id is none
         or trigger.device_id == 'REPLACE_WITH_HA_REGISTRY_DEVICE_ID' }}
actions:
  - variables:
      tv_timer_name: "{{ trigger.slots.countdown_label | trim }}"
  - action: kellies_voice.cancel_timer
    data:
      device_id: REPLACE_WITH_KELLIES_PAIRING_ID
      name: "{{ tv_timer_name }}"
  - if:
      - condition: template
        value_template: >-
          {{ states('input_text.kellies_voice_last_tv_timer') | lower
             == tv_timer_name | lower }}
    then:
      - action: input_boolean.turn_off
        target:
          entity_id: input_boolean.kellies_voice_timer_context
      - action: input_text.set_value
        target:
          entity_id: input_text.kellies_voice_last_tv_timer
        data:
          value: ""
  - set_conversation_response: "Sent cancellation for {{ tv_timer_name }} to the TV."
mode: queued
max: 8
```

## 4. "Stop" / "Cancel" with no name

This cancels the **last named TV timer** and dismisses ringing alerts. With multiple ongoing timers it tracks only the most recently started one. In a home with competing Assist/TV/media voice commands, `stop` alone is inherently ambiguous. This automation requires the timer-context helper to be ON, and its exact unprefixed sentences can still be recognized globally on null-device pipelines. Disable this part if it interferes with any other assistant or media workflow.

```yaml
alias: Kellies Voice stop or cancel recent TV timer
triggers:
  - trigger: conversation
    command:
      - "stop"
      - "cancel"
      - "stop timer"
      - "cancel timer"
      - "stop the timer"
      - "cancel the timer"
      - "stop my timer"
      - "cancel my timer"
      - "stop the countdown"
      - "cancel the countdown"
conditions:
  - condition: state
    entity_id: input_boolean.kellies_voice_timer_context
    state: "on"
  - condition: template
    value_template: >-
      {{ trigger.device_id is none
         or trigger.device_id == 'REPLACE_WITH_HA_REGISTRY_DEVICE_ID' }}
actions:
  - variables:
      tv_timer_name: "{{ states('input_text.kellies_voice_last_tv_timer') }}"
  - action: kellies_voice.dismiss_timers
    data:
      device_id: REPLACE_WITH_KELLIES_PAIRING_ID
  - if:
      - condition: template
        value_template: "{{ tv_timer_name | length > 0 }}"
    then:
      - action: kellies_voice.cancel_timer
        data:
          device_id: REPLACE_WITH_KELLIES_PAIRING_ID
          name: "{{ tv_timer_name }}"
  - action: input_boolean.turn_off
    target:
      entity_id: input_boolean.kellies_voice_timer_context
  - action: input_text.set_value
    target:
      entity_id: input_text.kellies_voice_last_tv_timer
    data:
      value: ""
  - set_conversation_response: "Sent stop for the most recent TV timer."
mode: queued
max: 5
```

Optional separate ringing-only words such as "dismiss the timer alerts" should call `kellies_voice.dismiss_timers` without canceling other countdowns.

## 5. Verify on the TV

Test **"set a pizza timer for five minutes"**, **"cancel the pizza timer"**, **"set a timer for five minutes"**, **"stop"**, and **"set a laundry timer for five minutes"**, **"cancel"**. Watch the real TV countdown and note what Home Assistant STT displays. The sentences are examples, not a wake word. With Ollama as the conversation agent, consider **Prefer handling commands locally** so these sentence triggers run before the LLM.

If the app says "Sorry, timers are not supported on this device," check **Settings → System → Logs** for a native `TimersNotSupportedError` and **Automation → Traces** for the start automation. If there is no trace, the actual STT text/pipeline isn't matching. With a trace and no TV popup, verify device ID, receiver status, Android overlay permission and HACS/APK versions.

**Validation scope:** In the owner's Home Assistant (2026-10-22), default-agent text tests correctly routed arbitrary named Pizza/Laundry/Rice/Pasta timers, unnamed timers, seconds/minutes/hours, named cancel and bare `stop`/`cancel`. After each test a cancellation command was sent; no timer was intentionally left ringing. Actual TV microphone validation of these broader phrases and multi-device behavior remains necessary. This is a Home Assistant configuration workaround while [issue #57](https://github.com/chadakenn/Kellies-Voice/issues/57) and [PR #50](https://github.com/chadakenn/Kellies-Voice/pull/50) work toward proper device-aware native voice routing.
