# Controlled onn 4K Pro preview: voice follow-up / privacy / timer / theme

**Purpose:** Test source from draft [PR #63](https://github.com/chadakenn/Kellies-Voice/pull/63) without merging or publishing a new GitHub Release. This branch is a **test build only**, not a public update.

**Test branch:** `test/onn-v0164-followup-preview`  
**Android package:** `com.example.tvvoiceprobe`, unchanged  
**Version:** `0.16.4-onn-test1`, `versionCode 21` (same as installed v0.16.4)  
**Signature:** must be the **same release-signing keystore** used by the installed v0.16.4 APK; never use the PR's `.debug` APK for an in-place upgrade.

## Build a signed test APK on GitHub

1. Open [Build Kellie's Voice Signed APK](https://github.com/chadakenn/Kellies-Voice/actions/workflows/build-apk.yml).
2. Click **Run workflow** and select branch **`test/onn-v0164-followup-preview`**.
3. Leave **publish_release = false / unchecked**. This branch also explicitly guards against release publishing when selected.
4. Click **Run workflow**. It signs and verifies `app-release.apk`, then stores an artifact called **`kellies-voice-signed`**. A test APK is **not** published to the public release feed and does not show up in the app's built-in update checker.
5. Open the completed *successful* run, download the **`kellies-voice-signed`** ZIP and extract **`app-release.apk`**. Do not use the separate **`Kellies-Voice-test-apk`** artifact from PR CI: that's a `com.example.tvvoiceprobe.debug` package and **will not** replace your installed app.
6. Copy the signed APK to the onn device and install it **over v0.16.4**, without uninstalling or clearing data. The same signature and versionCode allow an in-place test. Android's unknown-apps prompt may need approval. If Android reports **signature mismatch**, stop; do not delete the original app. If the installer rejects the same versionCode on a particular TV, use an approved higher-code preview strategy rather than force-downgrading or uninstalling.

**Rollback:** Keep or download the **original signed v0.16.4 `app-release.apk`**. Reinstall that official signed APK over the test build if the device permits equal-versionCode replacement. If the installer doesn't allow it, stop rather than uninstalling, changing credentials, or clearing data.

**Do not run** `publish_release=true` or merge this test branch to `main`. No GitHub Release, HACS release, or OTA channel is changed by this test.

## Before testing

- Confirm the TV remains paired to Home Assistant and its saved token survives the in-place install.
- Record the starting values for **Settings → Overlay appearance**, **Auto-listen**, and **Follow-up listening**. Follow-up starts OFF by default.
- The installed **v0.16.4 Home Assistant integration** is enough for initial app-side mic/theme/timer tests. The **new HA registry device ID** and **Follow-up silence select** need the matching integration files from #63. Do not confuse an old HACS integration with a failed Android device-ID implementation. Updating the live HA integration separately needs a deliberate rollback plan and HA restart; don't silently overwrite the working HACS files.
- **No wake-word listener** is included. For now, press the remote mic/TALK control; do not require saying “Julia” until wake word is added by explicit choice.

## Physical TV acceptance sequence

| Step | What to do | Expected result |
| --- | --- | --- |
| 1. Baseline | Launch Kellie's Voice; try a normal question, then close the overlay. | Existing HA pairing, voice, camera and remote controls remain working. |
| 2. Theme | Change the Aurora/Night Signal selection from HA; open the main screen and Settings. | Entire app follows chosen skin, not only the floating overlay. |
| 3. Timer | “Set a two-minute laundry timer.” | Named countdown appears without saying Julia. |
| 4. Timer stop | “Stop.” or “Cancel.” | Timer stops, chimes don't ring. |
| 5. Dismiss button | Start a **10-second test timer**, let it ring; press remote OK on visible Dismiss. | OK stops alert without needing Back. Back remains a fallback. |
| 6. Follow-up OFF | Keep Follow-up listening OFF. Ask about the weather. | Julia answers once. The mic does not unexpectedly reopen. |
| 7. Follow-up ON | In TV Settings choose Follow-up listening ON with 8 seconds. Ask “What's the weather?” then, after she finishes, “What about tomorrow?” **without pressing a button**. | Same conversation continues, mic dot stays visible during active preview/listening. |
| 8. Silence | After one follow-up answer, stay silent beyond the selected interval. | Preview releases mic and its indicator clears after recorder release. No surprise recording. |
| 9. STOP / Back | During active follow-up, press STOP then retry and press Back. | Both end capture, release recorder and don't resume unexpectedly. |
| 10. Disconnection | While follow-up is waiting, temporarily interrupt the TV's network/HA connection, then restore it. | No stale request starts a microphone later. |
| 11. TV sound | Play YouTube while activating a question/follow-up. | Audio focus/ducking returns to the prior level when mic is released. |
| 12. Regressions | Open driveway/doorbell live camera and the seven-day forecast card. | Prior features and placement still work. |

For each failure, record: **exact words spoken, on-screen response, mic indicator state, whether the app disconnected, and which step failed**. Do not claim success based on CI alone.

## Scope of this test

Includes: #50 origin-TV identity, #53 main theme, #55 timer OK, #56/#58 README/HACS banner source, #46 follow-up listening, and #62 run-scoped microphone privacy fix. It **does not include** #59 optional Okay Nabu wake word or #51/#61 announcement action buttons and door unlock. The banner cannot be considered visually verified until the HACS page is refreshed after a separately approved HA/repo update.

No live lock, garage door, or other security-sensitive device needs to be operated for this preview.
