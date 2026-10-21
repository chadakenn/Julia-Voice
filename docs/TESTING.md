# Field testing and verified milestones

Updated: **2026-10-20**. Device: user's **onn 4K Pro / Google TV**, with the Android app built through GitHub Actions.

Distinguish what has actually worked on hardware from compiled features. A green build is **not** proof of behavior on a TV.

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

The permanently signed **[build #39](https://github.com/chadakenn/Julia-Voice/actions/runs/39400677478)** completed compilation and APK signature verification. The following are **not yet hardware-verified**:

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
