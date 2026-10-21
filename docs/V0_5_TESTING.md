# v0.5: Glass voice overlay, one-press listening, and themes

**Status (2026-10-20): development build, awaiting field tests on the onn 4K Pro.** The v0.4 overlay displayed over Google TV Home and authenticated to Home Assistant. The older v0.3 foreground app successfully transcribed a question, showed the answer and spoke it through TV speakers. The **v0.5 one-press and Glass experience is new code**, not yet hardware verified.

## Download / safe update

Get the latest successful **Build Kellie's Voice Signed APK** run from GitHub Actions. Download artifact `kellies-voice-signed`, unzip and install **`app-release.apk`**.

Version `0.5-glass-voice` / Android versionCode **5** uses the **same permanently stored signing key** as signed v0.4. It should update over the existing app without requiring uninstalling or retyping Home Assistant credentials. **Do not uninstall v0.4.** If Google TV reports a signing conflict, stop and report the error.

## What's new

- **Glass** (default): wide lower-screen, dark violet/navy translucent card with cyan/purple procedural animated waveform and prominent assistant answer. The waveform is a **state animation**, not a display of recorded microphone amplitude.
- **Classic**: smaller blue upper-right card, closer to the v0.4 style.
- **Auto-listen** (default ON): launching the overlay starts a microphone session **after Home Assistant authenticates**, no second TALK click and no wake word.
- **Auto-stop** (best-effort): detects a voice followed by about 1.5 seconds of quieter audio and sends end-of-speech to Home Assistant. TV speaker audio or ambient noise may confuse this first local heuristic. The **STOP** button always remains available; there is also a 23-second safety timeout.
- **Auto-hide** (default ON): hides the overlay about 6.5 seconds after the spoken reply finishes, unless the user starts another question. If a spoken reply is unavailable, a longer fallback applies.
- **Manual and customizable options:** from regular Kellie's Voice app, choose **SETTINGS → Overlay style and voice settings**. Change Glass/Classic, auto-listen ON/OFF and auto-hide ON/OFF. Settings are stored locally, not in the public repository.
- **⭐ compatibility option:** separate installed-app launcher **Kellie's Voice Overlay** remains available. If your onn shortcut picker lists **only Kellie's Voice** (not Kellie's Voice Overlay), enable **SETTINGS → Overlay style and voice settings → ⭐ picker fallback (main app opens overlay)**. It makes the regular launcher open the overlay. Use the overlay's **SETTINGS** button to return to the full app later.

**The app cannot program your actual onn remote button remotely.** You need to configure its shortcut on the TV, and the firmware may hide activities under one package. It does not capture Google's Bluetooth voice remote microphone; use the onn box's microphones.

## Tonight's test sequence

1. Install **signed v0.5** over signed v0.4. Confirm the saved Home Assistant URL/token remain available.
2. From the regular app, open **SETTINGS → Overlay style and voice settings**. Confirm **Glass**, **Auto-listen ON**, **Auto-hide ON**.
3. Launch **Kellie's Voice Overlay** from the Google TV apps list, or use the existing main app's **Open floating voice overlay** item. **Don't press TALK.**
4. Watch for **CONNECTING → LISTENING**. Speak toward the onn box. Say **“What time is it?”** and pause naturally. If the local quiet detector doesn't end capture within a few seconds, press **STOP**.
5. Confirm a transcript, the Home Assistant answer, and audible speech. If enabled, the overlay should disappear after the spoken reply. Try CLOSE/BACK as an emergency dismiss.
6. **⭐ remote mapping:** press and hold ⭐ to access the customizable-button favorite app settings where supported. Choose **Kellie's Voice Overlay** if shown. If only the parent **Kellie's Voice** appears, return to settings and enable **⭐ picker fallback**, then select Kellie's Voice.
7. Start a **YouTube video**, press ⭐ once, ask a short question without hitting TALK, then observe whether the glass panel appears **over video** and whether audio mixes, ducks, pauses or fails. DRM-protected streaming apps may behave differently. **No promise of unbroken playback** until tested.

## If it misbehaves

- **It opens full-screen instead of the overlay:** check the selected shortcut or enable the main-app fallback.
- **No microphone activity:** ensure Android microphone/overlay permissions are still granted, check the on-screen status and use manual TALK.
- **It listens but never processes:** press STOP; the auto silence detector is experimental, especially with TV audio nearby.
- **It disappears too quickly:** disable Auto-hide in overlay settings.
- **Need settings while ⭐ fallback is ON:** press overlay **SETTINGS** to open the full-screen app.
- **It returns to Google TV Home rather than YouTube:** the device may be putting the old task behind the launcher during shortcut launch; report this with an innocuous screenshot.
- **No overlay over Netflix/other DRM video:** test over Google TV Home and YouTube first; Android TV overlay restrictions differ by vendor/app.

Keep signing keys and Home Assistant bearer tokens out of bug reports and screenshots. Report the exact status string and whether the overlay appears, listens, transcribes, speaks and closes.

## Future

Proper configurable skins gallery, animation options, larger media/info cards, TV audio focus/ducking, optional wake-word service, background HA announcements and Frigate live camera PiP remain separate roadmap work. **Wake words are not enabled in v0.5.**
