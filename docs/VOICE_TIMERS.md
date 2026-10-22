# Voice timers: v0.16.4 and Assist setup

Named TV timers show small countdown pills over the current app. At completion: one chime, a 180 ms gap, a second chime, the optional configured phrase, then a 500 ms pause before the next pair. **Alerts repeat until dismissed, with no automatic cutoff.** Multiple completed timers share one audio sequence; dismiss the current timer to hear the next. Back dismisses every ringing timer, leaving running timers alone. The completion window has D-pad buttons; countdowns do not take remote focus.

The bundled chime plays locally. Optional speech uses the TV's Android text-to-speech engine, not the microphone or HA voice agent; it may sound different from Julia. If speech is unavailable, chimes continue. A stalled utterance is stopped after 15 seconds so the pair sequence can resume. The phrase is configured per timer and limited to 140 characters.

**Shipped in v0.16.4** for both the signed Android TV APK and the matching Home Assistant integration. Timers need an opted-in TV event receiver and remain an awake-TV feature. The optional hands-free microphone follow-up from PR #46 is not in this release. In v0.16.4 the onn owner confirmed countdown, repeating chimes, and Back-to-dismiss; the remote OK / Dismiss button did not work on their TV and is tracked in [issue #54](https://github.com/chadakenn/Kellies-Voice/issues/54) / draft PR #55. Until that fix is released and verified, use Back or the `dismiss_timers` Home Assistant action.

## Requirements and behavior

- Matching Android and HA integration builds, an opted-in TV event receiver, overlay permission, and one paired TV device ID from Kellie's Voice. The target ID is required; timer commands are never broadcast by default.
- Up to eight timers per TV, with whole-second durations from 1 second to 24 hours. Names use 1–40 ASCII letters/digits/spaces/hyphens/underscores. Names ignore case and repeated spaces. Starting the same name replaces it; duplicate event delivery does not reset it.
- Timers use monotonic elapsed time while the receiver is running. Their deadlines and alert settings are saved locally and restored when the receiver restarts, including expired timers still awaiting dismissal. Turning TV events off explicitly forgets timers and silences alerts.
- The TV must receive the initial command. HA event delivery has no acknowledgement or offline command queue; a spoken confirmation that HA sent a command is not proof the TV received it. Once received, a timer does not require an HA connection to count down or ring. An offline cancel cannot reach the TV; use its Dismiss button or Back.
- This is an awake-TV feature. Android suspension, force-stop, muted alarm volume or firmware restrictions can prevent/delay alerts. No exact-alarm/wake-lock guarantee is implemented. Cameras/weather retain separate windows. Concurrent voice and timer audio, sleep/reboot, and focus interactions require onn testing.

## Home Assistant actions

Start a named timer through **Developer tools → Actions**:

```yaml
action: kellies_voice.start_timer
data:
  device_id: REPLACE_WITH_PAIRED_TV_ID
  name: Pizza
  duration_seconds: 300
  speak_alert: true
  alert_phrase: Your pizza timer is finished.
```

Set `speak_alert: false` for chimes only. `alert_phrase` is optional; the default is the timer name followed by “timer is finished.” Cancel a running timer or dismiss its completion alert:

```yaml
action: kellies_voice.cancel_timer
data:
  device_id: REPLACE_WITH_PAIRED_TV_ID
  name: Pizza
```

Dismiss all ringing timers, preserving running timers:

```yaml
action: kellies_voice.dismiss_timers
data:
  device_id: REPLACE_WITH_PAIRED_TV_ID
```

## Voice control with an LLM agent (Ollama, etc.)

For an LLM-backed conversation agent, **exposing parameterized Home Assistant scripts** is the simplest way to say natural requests such as “Julia, set a pizza timer for five minutes” without a fixed-duration script for every timer. Create these on **Settings → Automations & scenes → Scripts → Create script → Edit in YAML**. These scripts are **not automatically installed by HACS**, because every installation uses a different paired TV ID and conversation agent. Replace `REPLACE_WITH_PAIRED_TV_ID` with the TV's pairing ID shown in **Kellie's Voice → Settings → Home Assistant layout → Show this TV's pairing ID** (the Home Assistant device's `kellie_device_id` attribute contains the same ID).

### Script 1: start or replace a named timer

Save this script as **Julia start TV timer**. Its fields and descriptions tell the LLM how to convert requested minutes/hours to integer seconds.

```yaml
alias: Julia start TV timer
description: >-
  Start or replace a named countdown timer on the Kellie's Voice TV only when
  the user asks Julia for a timer. Supply duration_seconds as whole seconds:
  five minutes is 300, one hour is 3600. Optionally name it Pizza or Laundry.
  The TV repeats completion chimes until dismissed.
mode: queued
max: 8
fields:
  duration_seconds:
    name: Timer duration, seconds
    description: Required integer 1 to 86400. Convert minutes and hours into seconds; 5 minutes is 300.
    required: true
    selector:
      number:
        min: 1
        max: 86400
        step: 1
        mode: box
  name:
    name: Timer name
    description: Optional short timer name such as Pizza, Laundry or Timer (maximum 40 characters).
    default: Timer
    selector:
      text: {}
  speak_alert:
    name: Speak on finish
    description: True only if the user explicitly asks for spoken completion; false means repeating chimes only.
    default: false
    selector:
      boolean: {}
  alert_phrase:
    name: Spoken completion phrase
    description: Optional text up to 140 characters read by Android TV TTS after the chimes.
    selector:
      text: {}
sequence:
  - action: kellies_voice.start_timer
    data:
      device_id: REPLACE_WITH_PAIRED_TV_ID
      name: "{{ name | default('Timer', true) }}"
      duration_seconds: "{{ duration_seconds | int(0) }}"
      speak_alert: "{{ speak_alert | default(false) | bool }}"
      alert_phrase: "{{ alert_phrase | default('', true) }}"
```

### Script 2: cancel a named timer

Create a second script named **Julia cancel TV timer**:

```yaml
alias: Julia cancel TV timer
description: >-
  Cancel the user's named Kellie's Voice TV timer or stop that timer's
  repeating alarm. Use the requested timer name, or Timer for an unnamed one.
mode: queued
fields:
  name:
    name: Timer name
    description: Existing timer name to cancel, such as Pizza, Laundry or Timer.
    default: Timer
    selector:
      text: {}
sequence:
  - action: kellies_voice.cancel_timer
    data:
      device_id: REPLACE_WITH_PAIRED_TV_ID
      name: "{{ name | default('Timer', true) }}"
```

### Script 3: silence currently ringing alerts

Create a third script named **Julia dismiss TV timer alerts**:

```yaml
alias: Julia dismiss TV timer alerts
description: >-
  Stop all currently ringing Kellie's Voice timer alerts when the user says
  dismiss or silence alarms. Leave unfinished countdown timers running.
mode: single
sequence:
  - action: kellies_voice.dismiss_timers
    data:
      device_id: REPLACE_WITH_PAIRED_TV_ID
```

For **all three scripts**, enable **Assist exposure** in the script's Voice assistants settings (or **Settings → Voice assistants → Expose**). Your Ollama or other LLM conversation integration must have **Control Home Assistant** enabled; the script must be available to the same Assist agent the TV uses. Keep names and descriptions descriptive, as LLM agents choose tools based on them.

Test spoken phrases on the TV: **“Julia, set a pizza timer for five minutes,” “Julia, cancel the pizza timer,”** and **“Julia, dismiss the timer alerts.”** Do not assume the command reached the TV just because the LLM says it ran. Check the visible pill or alarm. Some native Assist pipelines route generic timer phrases to their built-in satellite timer handling; named **TV** requests can resolve that ambiguity. The remote's **Back** key remains a known working dismiss fallback on v0.16.4.

The documented YAML configuration below is an **alternative** for native Assist custom sentences and `intent_script` users who don't want the LLM to choose parameterized tools.

## Reliable Julia phrases using Home Assistant sentence triggers

If saying **“Julia, set a two-minute Coffee timer”** produces **“Sorry, timers are not supported on this device,”** Home Assistant's *native* Assist timer handler is intercepting the phrase. Native Assist timers belong to a registered, timer-capable satellite; Kellie's Voice v0.16.4 instead manages its **own TV countdowns**. A Home Assistant log may show `TimersNotSupportedError: device_id=None`. This is not evidence that Kellie's Voice's TV countdown failed.

For a reliable custom route that avoids asking a small local LLM to select a complex script tool, create the following **three separate Home Assistant automations** under **Settings → Automations & scenes → Create automation → Edit in YAML**. Replace `REPLACE_WITH_PAIRED_TV_ID` in every action with your own TV pairing ID from Kellie's Voice settings. The examples are intentionally limited to commands beginning with **Julia** or ending with **on the TV**, so they do not hijack ordinary voice timers on other Assist satellites.

Home Assistant tests sentence automations **before** built-in commands, but only in its own conversation agent. If your pipeline uses Ollama, enable **Prefer handling commands locally** in the Home Assistant voice assistant pipeline to run sentence triggers first. If it is off, Ollama might answer using its own tools and this automation may not fire. Ask the exact phrases below; the wake word/name needs to be present in the recognized text.

### A. Start a named countdown by voice (minutes)

```yaml
alias: Julia TV countdown by voice
description: Direct Julia-specific named timer requests to the paired Kellie's Voice TV.
triggers:
  - trigger: conversation
    command:
      - "Julia set a {1..1440:minutes} minute {countdown_label} timer"
      - "Julia set a {countdown_label} timer for {1..1440:minutes} minutes"
      - "Julia start a {countdown_label} countdown for {1..1440:minutes} minutes"
      - "Julia start a {1..1440:minutes} minute {countdown_label} countdown"
      - "set a {1..1440:minutes} minute {countdown_label} timer on the TV"
      - "set a {countdown_label} timer for {1..1440:minutes} minutes on the TV"
      - "start a {countdown_label} countdown for {1..1440:minutes} minutes on the TV"
actions:
  - action: kellies_voice.start_timer
    data:
      device_id: REPLACE_WITH_PAIRED_TV_ID
      name: "{{ trigger.slots.countdown_label | trim }}"
      duration_seconds: "{{ (trigger.slots.minutes | int(0)) * 60 }}"
      speak_alert: false
  - set_conversation_response: "Started the {{ trigger.slots.countdown_label | trim }} countdown on the TV for {{ trigger.slots.minutes | int(0) }} minutes."
mode: queued
max: 8
```

The `{1..1440:minutes}` matcher understands spoken numbers (e.g., **two**) as well as digits. Example: **“Julia, set a two-minute Coffee timer.”** This creates a local timer on the paired TV with the name Coffee and duration 120 seconds. It is different from Home Assistant's built-in voice-timer service. Add a separate seconds/hour matcher if your household needs those units. Use a name allowed by the TV app (ASCII letters, digits, spaces, hyphens, underscores; max 40).

### B. Cancel a named countdown

```yaml
alias: Julia cancel TV countdown by voice
triggers:
  - trigger: conversation
    command:
      - "Julia cancel the {countdown_label} timer"
      - "Julia cancel the {countdown_label} countdown"
      - "Julia stop the {countdown_label} timer"
      - "Julia stop the {countdown_label} countdown"
      - "cancel the {countdown_label} timer on the TV"
      - "cancel the {countdown_label} countdown on the TV"
      - "stop the {countdown_label} countdown on the TV"
actions:
  - action: kellies_voice.cancel_timer
    data:
      device_id: REPLACE_WITH_PAIRED_TV_ID
      name: "{{ trigger.slots.countdown_label | trim }}"
  - set_conversation_response: "Sent the cancel command for {{ trigger.slots.countdown_label | trim }} to the TV."
mode: queued
max: 8
```

Example: **“Julia, cancel the Coffee timer.”** Cancel also stops that timer's ringing alarm, if it has already expired.

### C. Silence all currently ringing chimes

```yaml
alias: Julia dismiss TV timer chimes by voice
triggers:
  - trigger: conversation
    command:
      - "Julia dismiss the TV timer alerts"
      - "Julia dismiss the timer alerts"
      - "Julia silence the TV timer chimes"
      - "Julia stop the TV timer alarm"
      - "dismiss the ringing TV timers"
      - "silence the TV timer alarm"
actions:
  - action: kellies_voice.dismiss_timers
    data:
      device_id: REPLACE_WITH_PAIRED_TV_ID
  - set_conversation_response: "Stopped the ringing TV timer alerts."
mode: single
```

These automations are **home-specific configuration**, not a universal HACS installation: each owner must supply their own paired TV ID. They are independent of the optional LLM scripts above. Prefer one matching route per phrasing so the command isn't delivered twice.

**Verified in the developer's Home Assistant with text sent to the default conversation agent:** a 15-minute Coffee command matched the sentence automation, called `kellies_voice.start_timer`, and a separate spoken-style Coffee cancel command called `kellies_voice.cancel_timer`. All 15-minute test timers were canceled. **Physical spoken microphone verification is still pending.** The current signed v0.16.4 app is unchanged.

## Voice setup

Use Home Assistant's documented [custom sentence files](https://www.home-assistant.io/voice_control/custom_sentences_yaml/) and [intent scripts](https://www.home-assistant.io/integrations/intent_script/). The custom-sentence and intent-script setup below is still an alternative path and has not been exercised on the owner's Ollama pipeline. An LLM agent may need local command handling enabled; verify exact matching before relying on it. The “Kellie” variants avoid ambiguity with native satellite timers.

Save this as `/config/custom_sentences/en/kellie_timers.yaml`:

```yaml
language: en
intents:
  KellieStartTimer:
    data:
      - sentences:
          - "[Kellie ]set [a] {timer_duration} {timer_unit} timer [called {timer_name}]"
          - "[Kellie ]set [a] {timer_name} timer for {timer_duration} {timer_unit}"
  KellieCancelTimer:
    data:
      - sentences:
          - "[Kellie ](cancel|stop|dismiss) [the] {timer_name} timer"
          - "[Kellie ]cancel [the] timer"
  KellieDismissTimers:
    data:
      - sentences:
          - "[Kellie ]dismiss [the] timer alerts"
lists:
  timer_duration:
    range:
      from: 1
      to: 1440
  timer_unit:
    values:
      - in: "second[s]"
        out: 1
      - in: "minute[s]"
        out: 60
      - in: "hour[s]"
        out: 3600
  timer_name:
    wildcard: true
```

Merge the following under the existing `intent_script:` key in `/config/configuration.yaml` (do not create a second key). Replace the TV ID in all three actions. Change `speak_alert` and `alert_phrase` in the start intent for your preferred default. Validate configuration and restart HA; install matching Android/integration builds only after approval.

```yaml
intent_script:
  KellieStartTimer:
    description: Start a Kellie TV timer. timer_duration is an integer; timer_unit is seconds per unit; timer_name is optional text.
    action:
      - action: kellies_voice.start_timer
        data:
          device_id: REPLACE_WITH_PAIRED_TV_ID
          name: "{{ timer_name | default('Timer', true) }}"
          duration_seconds: "{{ (timer_duration | int) * (timer_unit | int) }}"
          speak_alert: false
          alert_phrase: "{{ timer_name | default('Timer', true) }} is finished."
    speech:
      text: "Sent the {{ timer_name | default('Timer', true) }} timer to the TV."
  KellieCancelTimer:
    action:
      - action: kellies_voice.cancel_timer
        data:
          device_id: REPLACE_WITH_PAIRED_TV_ID
          name: "{{ timer_name | default('Timer', true) }}"
    speech:
      text: "Sent the cancel command for {{ timer_name | default('Timer', true) }}."
  KellieDismissTimers:
    action:
      - action: kellies_voice.dismiss_timers
        data:
          device_id: REPLACE_WITH_PAIRED_TV_ID
    speech:
      text: "Sent the dismiss command for ringing timers."
```

Examples: “Set a 5 minute timer”, “Set a pizza timer for 5 minutes”, “Cancel the pizza timer”, “Dismiss timer alerts”. If native HA satellite handling takes the generic timer phrase, use “Kellie set a 5 minute timer”. No app-side parsing of arbitrary assistant answers is used.

## Required onn QA

Alerts stay silent unless overlay permission is granted and the attached completion window is visible with Dismiss controls. Losing permission or the window stops chimes and speech; an undismissed alert can resume when controls return. Temporary audio-focus loss pauses playback while retaining the request for focus gain. Permanent loss or denial retries after two seconds, so later timers are not left muted. These recovery paths have host regression tests; actual Android window/audio behavior still requires the checks below.

- [ ] Start two named timers over YouTube and verify countdowns without remote-focus theft.
- [ ] At expiry, verify exactly two chimes, optional complete phrase, short pause, repeat indefinitely until dismissed.
- [x] Owner's onn v0.16.4: 10-second Pizza timer pill appeared, chimes repeated and Back stopped the alert.
- [ ] Known v0.16.4 issue: D-pad OK / Dismiss button did not activate; follow #54 / draft PR #55 and retest on next release.
- [ ] Test D-pad Dismiss, Back, named voice cancellation, replacement while ringing, and multiple expired timers.
- [ ] Verify dismiss stops MediaPlayer/TTS callbacks, restores audio focus, and cannot restart a stale alert.
- [ ] Interrupt a ringing alert with other audio, then verify recovery and a later timer. Revoke overlay permission or hide/remove the completion window during chimes and speech: audio must stop; restore controls and verify safe resumption and Dismiss.
- [ ] Test absent TTS engine/voice, muted alarm volume, HA/network loss, app force-stop, receiver disable, sleep and restart.
- [ ] Confirm timers do not enable microphones, replace cameras/weather, or change release behavior.
