# Floating TV voice overlay: v0.4 prototype

> **Experimental, awaiting onscreen testing on the onn 4K Pro.** This is NOT yet Frigate PiP, a persistent satellite, or an always-on wake-word assistant. Keep the regular working v0.3 foreground voice app available as a fallback.

## What's new

- An opt-in, focusable `TYPE_APPLICATION_OVERLAY` panel with **TALK, STOP, CLOSE**, HA status, transcript and spoken TTS replies.
- While the panel is visible, the Android service runs as a **microphone-type foreground service** with an ongoing notification. **Microphone recording begins only when TALK is selected**, and stops on STOP, server STT/VAD ending, or when you dismiss the overlay.
- The overlay obtains its **encrypted Home Assistant settings** from the app's existing Android Keystore store. No second token setup should be necessary after installing a *signed* update over another signed version.
- Two ways to start it:
  1. Open the regular **HA TV Voice Alpha** → **SETTINGS → Open floating voice overlay (experimental)**.
  2. After permission setup, launch the **HA Voice Overlay** shortcut shown as a separate TV app entry. If Google TV offers an installed-app action for the programmable **⭐ button**, this shortcut is the one to choose.
- CLOSE or Back dismisses the window and stops the service, including its mic capture and connection. This is opt-in, not auto-started at boot.

## Install

Install the latest **successful** [signed GitHub Actions build](../.github/workflows/build-apk.yml), artifact `onn-tv-ha-assist-signed`, ZIP containing `app-release.apk`.

**Important: `versionCode=4`, `versionName=0.4-overlay-prototype`.** If you already installed v0.3 *signed* release, this APK should install **over it without uninstalling or retyping your HA token**. The private release signing key is the same, and versionCode increased.

**Do not uninstall the working signed app to install this update.** If it reports a signature conflict, stop and report it: we need to diagnose signing before modifying/removing the old app.

## First test (avoid disrupting movies)

1. Launch the updated **HA TV Voice Alpha**; check the normal foreground voice conversation still works.
2. Select **SETTINGS → Open floating voice overlay (experimental)**. Grant Android's **Display over other apps** permission if it offers that setting. Some TV firmware may not expose it.
3. Reopen the app and select the same overlay action after granting permission. If the microphone runtime permission prompt appears, approve it.
4. The app should return to the previous task, and a small translucent panel should appear near the **top-right**. Depending on the TV task manager, you may be looking at the TV launcher rather than a previously playing app. That is a firmware limitation to test, not a guarantee of playback preservation.
5. Confirm remote D-pad **LEFT/RIGHT** switches TALK / STOP / CLOSE, **OK** selects, and **BACK** closes.
6. Press **TALK**, speak toward the **onn 4K Pro box** (not the Google remote mic), then **STOP**. Check the transcript and answer and whether spoken TTS works.
7. Select **CLOSE**. Confirm the overlay disappears, its ongoing notification is removed, and TV controls return to the underlying app.
8. Optionally test again while YouTube or a non-DRM app is already open. DRM-protected apps may suppress overlays; don't assume Netflix, Nuvio, Stremio, etc. behave alike.

## ⭐ button quick-launch experiment

After the overlay permission is granted and normal conversation is configured:

1. Look in Google TV / onn remote shortcut settings to see whether the programmable ⭐ button can **launch a selected installed application**.
2. If it offers an app list, choose the separate **HA Voice Overlay** entry, **not** the regular **HA TV Voice Alpha** config screen.
3. Press ⭐ while another TV app is visible. The brief shortcut activity should start the foreground overlay service and then finish to reveal the previous task, subject to Google TV's activity-stack behavior.
4. The microphone button still invokes Google Assistant. **This does not remap the mic button or grant access to the Bluetooth remote mic.**
5. If ⭐ cannot select this shortcut, report what the TV settings offer; a button mapper or activity alias fallback may be needed. Do not disable Google services or grant root for this test.

## Known constraints

- Android TV firmware may deny overlays, may block focus/key input, may bring the launcher forward, or may suppress windows over DRM video. This is precisely what we need to test on real hardware.
- The foreground-service notification exists only while overlay is active. Do not start service from the background or when a microphone permission is missing. Android 14+ imposes foreground service microphone rules.
- The overlay **does not** run continuously in the background and **does not** listen for wake words. It intentionally closes when dismissed.
- Audio focus/ducking, camera PiP, automation announcements, a dedicated HA media player entity and secure phone pairing are separate future milestones.
- Use **trusted HTTPS** to Home Assistant where possible. Existing warned local HTTP transmits token and audio unencrypted.

## Troubleshooting

If nothing appears, return to the main app and check **Settings → Display over other apps** permission, and whether an onn/Google TV permission screen is supported. If the TV returns home rather than the prior app, test launching the **HA Voice Overlay** shortcut from another app. If voice transcription fails, verify whether the original full-screen TALK / STOP continues to work.

For bug reports provide firmware/OS version, whether overlay permission existed, whether the panel appeared over a **normal non-DRM app**, D-pad behavior, and any status error **without posting HA tokens or signing secrets**.
