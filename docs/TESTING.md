# Field testing and verified milestones

Updated: **2026-10-20**. Device: user's **onn 4K Pro / Google TV**, with the Android app built through GitHub Actions.

Distinguish what has actually worked on hardware from compiled features. A green build is **not** proof of behavior on a TV.

## v0.13 Home Assistant disconnection recovery (pending TV test)

- [ ] With saved credentials, launch full-screen Kellie's Voice and verify Home Assistant changes to connected **without selecting Reconnect**
- [ ] Open ⭐ overlay and confirm auto-listen and speech playback still work
- [ ] Temporarily disable the TV's network or restart HA; verify the overlay/full-screen screen reports **automatic reconnection** and recovers after network returns, without manual Reconnect
- [ ] With a lost connection, press ⭐ or TALK once. If reconnection completes within 30 seconds, it may start listening; if it takes longer, the microphone must **not** turn on automatically later
- [ ] Interrupt the network during listening: confirm microphone streaming stops and UI doesn't remain stuck in the listening state
- [ ] Leave the app in the background, then reopen it; it should retry immediately instead of waiting for the existing retry countdown
- [ ] With an intentionally invalid token **in a safe, test-only setup**, verify the authentication error is shown and retrying stops; restore the proper token afterward
- [ ] Check that restarting HA, normal overlay CLOSE/BACK, and intentionally forgetting credentials never trigger zombie sockets or unexpected microphone use
- [ ] Keep original `com.example.tvvoiceprobe` and signed release key; install over working APK without wiping HA configuration

CI success confirms compilation/signing only. These checks require actual hardware to distinguish Android TV network sleep, multicast-DNS and HA restarts from app logic. A permanently running Home Assistant event receiver and 24/7 socket service are intentionally **not** part of v0.13.

## Follow-up listening (not yet tested on a TV)

The unit tests check the session rules only. They do not prove the onn microphone, the privacy dot, or TV audio.

- [ ] With follow-up **Off**, one question still behaves as it does today. The microphone does not reopen after the answer.
- [ ] Turn follow-up **On** at the TV, with 8 seconds. Ask a question. When speech finishes, the panel should say it is listening. Ask "What about tomorrow?" without pressing the remote. Julia should use the same conversation.
- [ ] Stay quiet for the selected 5, 8, 10, or 15 seconds. Capture should stop. The panel should not say the system privacy dot is off unless that dot has actually cleared.
- [ ] During the follow-up wait, before the next question reaches Home Assistant, the system microphone indicator stays **on**. It must not stay off while the preview is recording.
- [ ] When the wait ends, **STOP**, **Back**, close, a speech error, or a dropped connection, the indicator may turn off only after capture has stopped. It must not turn off and back on in the moment the same recorder is handed to Assist.
- [ ] Pull the network during the answer. The follow-up must not start after reconnect.
- [ ] While YouTube is playing, confirm the TV audio is ducked only while a question is being captured, then returns.
- [ ] From Home Assistant, change **Follow-up silence** while the TV switch is Off. The TV must stay Off. Turn the TV switch On and confirm a later Home Assistant change can set 5, 8, 10, or 15 seconds.

## v0.12 overlay layout / copper settings acceptance (pending hardware)

Signed [build #108](https://github.com/chadakenn/Kellies-Voice/actions/runs/39598350715) passed compilation and APK signature verification. This **does not** yet establish TV compatibility.

- [ ] Install `kellies-voice-signed/app-release.apk` over the working TV version without uninstalling; existing credentials and ⭐ fallback remain
- [ ] Main SETTINGS opens a copper/charcoal D-pad menu; UP/DOWN/OK and Android BACK navigate without accidentally selecting the TALK button behind the dialog
- [ ] SETTINGS → Overlay appearance & position shows position, size, auto-hide, style, auto-listen and ⭐ fallback
- [ ] Default Glass placement and medium size look unchanged from the previous installed app
- [ ] Confirm all four corners, center and bottom center without clipping near the TV's overscan/safe area
- [ ] Confirm small/medium/large fit the screen and the TALK / STOP / CLOSE / SETTINGS buttons are still reachable
- [ ] Confirm auto-hide occurs **after spoken TTS completes** at 5, 7, 10 and 20 seconds, and stays visible for Never
- [ ] Verify unplugging Home Assistant or losing connectivity doesn't reset the stored position/size; verify reopening overlay applies changes
- [ ] Check overlays above YouTube and the Google TV home screen, and verify microphone service stops on CLOSE/BACK
- [ ] Verify source retains original Android package ID and signing key and updater still checks exact approved GitHub release paths

**Not implemented:** remote control of TV settings from Home Assistant, assistant announcement popups, Frigate snapshots/live feed, or weather cards. See [future design](REMOTE_CONFIGURATION_PLAN.md).

## Proven on the onn 4K Pro

| Area | Observation | Status |
| --- | --- | --- |
| Android TV APK launches | Original diagnostic screen appeared on TV | ✅ Tested |
| D-pad remote | After focus fix, UP/DOWN/OK operated the app | ✅ Tested |
| Enumerate audio inputs | Android reported four input devices; this alone doesn't reveal remote mic access | ✅ Tested |
| Built-in mic audio | `VOICE_RECOGNITION` source showed peaks up to 3,484 / 32,768 when speaking toward the box without pressing the remote mic button | ✅ Nonzero audio |
| Plain `MIC` audio | Peaks around 320 / 32,768, and ~303 with remote voice button held | ✅ Measured |
| Remote microphone button | Pressing it opens **Google Assistant**, not our app | ✅ Tested; not supported |
| HA WebSocket token auth | UI displayed **Connected and authenticated** for the user's HA server | ✅ Tested |
| STT pipeline | Captured and displayed user's utterance **“What time is it?”** correctly | ✅ Tested |
| Compact answer-first layout | Shows HOME ASSISTANT SAYS above YOU SAID, with TALK / STOP / SETTINGS | ✅ Verified in user photo |
| TTS playback through TV speakers | `tts_output.url` consumed; HA answered “5:07 AM” and the user confirmed spoken TV audio | ✅ End-to-end tested |
| Permanent APK signing | GitHub Actions run **#31** built, signature-verified and uploaded `app-release.apk` | ✅ CI-tested |
| Migration debug to release | User was preparing to uninstall original debug build and install signed release | ⏳ Awaiting TV test |

**Caution:** Increased audio peak numbers indicate received audio samples, not validated acoustics for every listening distance or guaranteed input-device identity. The box microphone is the probable source based on the separate test and working STT; Bluetooth remote mic access remains unproven.

## v0.4 experimental overlay follow-up

The permanently signed **[build #39](https://github.com/chadakenn/Kellies-Voice/actions/runs/39400677478)** completed compilation and APK signature verification. The following are **not yet hardware-verified**:

- [ ] Signed v0.4 installs *over* signed v0.3 while preserving encrypted HA credentials
- [x] Overlay permission is effective: Google TV displayed our floating panel above its launcher (exact permission-screen steps not documented)
- [x] Floating panel appeared in the upper-right **over Google TV Home**, connected and authenticated to HA; behavior over other apps still needs checking
- [ ] D-pad selects TALK / STOP / CLOSE; BACK dismisses panel and releases controls
- [ ] Microphone capture, HA transcript, reply and TV TTS work through the floating panel
- [ ] Closing the overlay stops the foreground-service notification and microphone activity
- [ ] The separate Kellie's Voice Overlay launcher starts overlay while another TV app is visible
- [ ] Optional onn ⭐ app shortcut can target the overlay launcher if firmware supports it
- [ ] Test a non-DRM video app first; DRM/Netflix overlays and audio focus remain unknown

See [step-by-step overlay tests](OVERLAY_TESTING.md). No remote-microphone button interception, boot-time auto-start or always-on wake word was introduced.

## Current MVP acceptance checklist

- [x] App opens and navigates using regular onn remote
- [x] Authenticated connection to Home Assistant
- [x] Built-in microphone sends speech to Assist and yields a readable transcript
- [x] Stable keystore-backed signed release build succeeds in CI
- [ ] One-time migration to signed `app-release.apk` installed on TV
- [x] Compact answer-first UI visible on TV; actual HA reply is readable
- [x] Home Assistant answer appears correctly in the **HOME ASSISTANT SAYS** area
- [x] TV speakers played HA TTS on the first end-to-end test (longer-term playback reliability still to test)
- [ ] Conversation can be repeated without restarting the app
- [ ] Second signed APK update installs **over** first release while preserving saved HA credentials
- [ ] Safe error messages when TV goes offline, HA is unavailable, or permissions are declined
- [ ] Actual audio-focus behavior while a streaming app is running

## Suggested next test sequence

1. Install first stable signed APK after the original debug build is removed.
2. Configure and connect to Home Assistant; verify auth and microphone permission.
3. **Completed:** “What time is it?” was recognized, “5:07 AM” appeared as the reply, and the user confirmed audio from the TV speakers.
4. **Next:** test repeated TTS, follow-up requests and whether any other TV app audio interferes.
5. Ask another short follow-up, then test **SETTINGS → Start new conversation**.
6. Later, after a second signed build is available, verify an over-the-top upgrade preserves the HA login. No uninstall.
7. After the foreground conversation is stable, begin opt-in ⭐ button launch and overlay tests.

## Known unimplemented areas

Floating over Netflix/Stremio/Nuvio/YouTube, programmable ⭐ shortcut, intercepting Google's microphone button, background voice service, wake words, HA `media_player` entity, automation announcements, Frigate/go2rtc camera PiP, and dashboard-on-demand are **not shipped**.

For each new bug, note build run number, app version, device firmware, exact status text, test steps and whether it reproduces after restarting. **Never include access tokens, private signing keys or raw voice recordings.**
