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
| Compact answer-first layout | Replaces seven oversized buttons with TALK, STOP, SETTINGS | 🔶 Compiled, not hardware-verified |
| TTS event parsing | `tts_output.url` handling corrected in code | 🔶 Compiled, TV-speaker playback unverified |
| Permanent APK signing | GitHub Actions run **#31** built, signature-verified and uploaded `app-release.apk` | ✅ CI-tested |
| Migration debug to release | User was preparing to uninstall original debug build and install signed release | ⏳ Awaiting TV test |

**Caution:** Increased audio peak numbers indicate received audio samples, not validated acoustics for every listening distance or guaranteed input-device identity. The box microphone is the probable source based on the separate test and working STT; Bluetooth remote mic access remains unproven.

## Current MVP acceptance checklist

- [x] App opens and navigates using regular onn remote
- [x] Authenticated connection to Home Assistant
- [x] Built-in microphone sends speech to Assist and yields a readable transcript
- [x] Stable keystore-backed signed release build succeeds in CI
- [ ] One-time migration to signed `app-release.apk` installed on TV
- [ ] Compact answer-first UI readable from couch
- [ ] Home Assistant answer appears correctly in the **HOME ASSISTANT SAYS** area
- [ ] TV speakers play HA TTS successfully, without choppy/duplicate audio
- [ ] Conversation can be repeated without restarting the app
- [ ] Second signed APK update installs **over** first release while preserving saved HA credentials
- [ ] Safe error messages when TV goes offline, HA is unavailable, or permissions are declined
- [ ] Actual audio-focus behavior while a streaming app is running

## Suggested next test sequence

1. Install first stable signed APK after the original debug build is removed.
2. Configure and connect to Home Assistant; verify auth and microphone permission.
3. Say “What time is it?” then STOP. Read the **answer** and **transcript** in the compact interface.
4. Confirm whether spoken TTS plays and whether any other TV app audio interferes.
5. Ask another short follow-up, then test **SETTINGS → Start new conversation**.
6. Later, after a second signed build is available, verify an over-the-top upgrade preserves the HA login. No uninstall.
7. After the foreground conversation is stable, begin opt-in ⭐ button launch and overlay tests.

## Known unimplemented areas

Floating over Netflix/Stremio/Nuvio/YouTube, programmable ⭐ shortcut, intercepting Google's microphone button, background voice service, wake words, HA `media_player` entity, automation announcements, Frigate/go2rtc camera PiP, and dashboard-on-demand are **not shipped**.

For each new bug, note build run number, app version, device firmware, exact status text, test steps and whether it reproduces after restarting. **Never include access tokens, private signing keys or raw voice recordings.**
