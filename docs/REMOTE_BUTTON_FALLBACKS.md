# onn 4K Pro: Remote button mapping fallback

Research began before hardware testing; updated 2026-10-20 with **actual onn 4K Pro observations**. The physical Google microphone key launches **Google Assistant**, while the built-in box microphone successfully captured speech sent to Home Assistant Assist. **No third-party button remapper or ⭐ custom launch has been tried yet.** No remapper or accessibility permission is needed for the working foreground TALK / STOP alpha.

## Candidate approaches (least invasive first)

1. **Onn 4K Pro built-in customizable ⭐ star button.** In the Google TV remote-button configuration, see whether it can launch our future Julia Voice app. This may be the simplest reliable push-to-talk *trigger* if the stock microphone/Assistant button is OEM-reserved. Exact menu behavior depends on model and firmware.
2. **tvQuickActions** (Android TV / Google TV app). Via its Accessibility Service, attempt to map a spare key (or possibly the star, Free TV, app shortcut, or voice button if delivered) to **Open App / Open Activity** or a targeted app intent. Its Free and Pro features differ; do not require a paid version before testing a free route. Docs say some keys are not detectable by normal remapping and some advanced actions may need ADB; do not disable system apps without an explicit rollback path.
3. **Button Mapper** as another Accessibility-based experiment if tvQuickActions fails. Preserve original remote navigation, Back and power behavior.
4. **Android default assistant RoleManager / Assist intent** remains a separate route to investigate; the existing Android diagnostic app can test role availability. The actual Google voice button is currently intercepted by Google.
5. **Alternative audio capture**: if remapping launches our app but remote mic is inaccessible, try onn 4K Pro built-in microphone, supported external USB mic, or a separate approved hands-free mechanism. This is a fallback, not the intended ideal.

## Critical separation of concerns

- **Key/action remapping** can sometimes *launch* our own app or an activity. It does **not prove** Google TV will route **Bluetooth remote microphone audio** into `AudioRecord`.
- Some voice/Assistant buttons are consumed by vendor/system software before accessibility remappers see an event. The stock assistant might also launch in parallel.
- With a remote-based microphone, audio capture may be tied to Google's voice-assistant session or vendor-specific protocol. Standard app microphone permission and a working remap do not guarantee access.
- Button mapping apps may require elevated Accessibility permissions. Grant only to a trusted app, test on a spare key first, and remove permissions when finished.
- Mapping any button to launch the TV app is not the same as showing a **noninterrupting overlay** over a streaming app; that separate capability must be tested.
- If a normal app icon launch interrupts movie playback, a mapped **explicit activity/service/intent** or a previously running service showing an authorized overlay might eventually work better, subject to current Android background launch and overlay restrictions.
- **tvQuickActions 3.7+ reportedly offers Web Widget Overlay actions**. This could be a convenient UI experiment, but not proof that an Assistant pipeline, actual mic transport, secure Frigate camera playback, or a native media-player endpoint will work.

## Remaining hardware test plan

1. **Completed:** installed the TV diagnostic APK and verified ordinary D-pad/OK controls. Android listed four audio input devices, box speech produced measurable microphone input, and the Google voice key launched Google Assistant. Continue probing extra keys only if needed.
2. In Google TV remote settings, test what ⭐ can be mapped to. Prefer direct native app launch if offered, and confirm the original streaming app can be resumed.
3. If ⭐ cannot launch the target action, test tvQuickActions with a **spare noncritical key**, using the Accessibility permission setup and **Open App** action. Record whether pressed key is recognized and whether the TV returns to the prior app.
4. Only then try mapping the **Google Assistant microphone** key; record whether key is detectable, whether both stock Assistant and our app open, and whether voice data reaches the probe.
5. **Completed for foreground STT:** the onn 4K Pro box microphone worked with Home Assistant's speech recognition for “What time is it?”. A detected key alone is NOT proof of Bluetooth remote mic support.
6. Prefer no root and don't disable Google packages for initial experiments. Keep rollback simple: remove mapping/Accessibility permission, restore stock ⭐ shortcut.

### Results matrix to fill after test

| Input/approach | Key detected? | Starts app? | Remote mic audio actually captured? | Original TV app preserved? |
| --- | --- | --- | --- | --- |
| Native mic / Assistant button | Key not confirmed in our activity | Opens **Google Assistant** | Not accessible through our current capture path | N/A |
| Native ⭐ customizable button | Not tested | Not tested | N/A; launching is separate from capturing | Not tested |
| tvQuickActions spare-key remap | Not tested | Not tested | N/A until capture tested | Not tested |
| tvQuickActions mic-key remap | Not tested | Not tested | Unverified | Not tested |
| Button Mapper alternative | Not tested | Not tested | Unverified | Not tested |
| onn 4K Pro built-in microphone | N/A | Foreground TALK tested | **Working box microphone audio and HA STT**; remote mic is different | Not a background overlay |

## Useful sources

- tvQuickActions documentation: https://tvdevinfo.com/tvquickactions/mappings/ and https://tvdevinfo.com/tvquickactions/quick-start/
- App overview / supported actions: https://play.google.com/store/apps/details?id=dev.vodik7.tvquickactions.free
- Vendor docs acknowledge some buttons cannot be remapped normally: https://tvdevinfo.com/tvquickactions/quick-start/
- Advanced ADB options are Pro-only (avoid for first tests): https://tvdevinfo.com/tvquickactions/settings/adb/
- tvQuickActions recent overlay feature notes: https://tvdevinfo.com/tvquickactions/changelog/
- Onn 4K Pro star-button overview: https://www.pcworld.com/article/2351725/walmart-onn-google-tv-4k-pro-review.html
- Android assistant role reference: https://developer.android.com/reference/android/app/role/RoleManager

**Current state:** hardware mic tests and HA WebSocket Assist STT worked on the actual TV; the remote's microphone button opens Google. The ⭐ shortcut and alternative remappers are **research candidates only**, not integrated. See [actual field results](TESTING.md) and [installation](INSTALL.md).
