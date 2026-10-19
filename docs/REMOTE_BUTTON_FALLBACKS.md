# onn 4K Pro: Remote button mapping fallback

Research note added before hardware testing. **None of these remapping methods has been tested on this specific user's box yet.** Do not require remapping tools or accessibility permissions just to run our diagnostic APK.

## Candidate approaches (least invasive first)

1. **Onn 4K Pro built-in customizable ⭐ star button.** In the Google TV remote-button configuration, see whether it can launch our future TV Voice app. This may be the simplest reliable push-to-talk *trigger* if the stock microphone/Assistant button is OEM-reserved. Exact menu behavior depends on model and firmware.
2. **tvQuickActions** (Android TV / Google TV app). Via its Accessibility Service, attempt to map a spare key (or possibly the star, Free TV, app shortcut, or voice button if delivered) to **Open App / Open Activity** or a targeted app intent. Its Free and Pro features differ; do not require a paid version before testing a free route. Docs say some keys are not detectable by normal remapping and some advanced actions may need ADB; do not disable system apps without an explicit rollback path.
3. **Button Mapper** as another Accessibility-based experiment if tvQuickActions fails. Preserve original remote navigation, Back and power behavior.
4. **Android default assistant RoleManager / Assist intent** remains a separate route for actual assistant integration, tested with our current diagnostic app.
5. **Alternative audio capture**: if remapping launches our app but remote mic is inaccessible, try onn 4K Pro built-in microphone, supported external USB mic, or a separate approved hands-free mechanism. This is a fallback, not the intended ideal.

## Critical separation of concerns

- **Key/action remapping** can sometimes *launch* our own app or an activity. It does **not prove** Google TV will route **Bluetooth remote microphone audio** into `AudioRecord`.
- Some voice/Assistant buttons are consumed by vendor/system software before accessibility remappers see an event. The stock assistant might also launch in parallel.
- With a remote-based microphone, audio capture may be tied to Google's voice-assistant session or vendor-specific protocol. Standard app microphone permission and a working remap do not guarantee access.
- Button mapping apps may require elevated Accessibility permissions. Grant only to a trusted app, test on a spare key first, and remove permissions when finished.
- Mapping any button to launch the TV app is not the same as showing a **noninterrupting overlay** over a streaming app; that separate capability must be tested.
- If a normal app icon launch interrupts movie playback, a mapped **explicit activity/service/intent** or a previously running service showing an authorized overlay might eventually work better, subject to current Android background launch and overlay restrictions.
- **tvQuickActions 3.7+ reportedly offers Web Widget Overlay actions**. This could be a convenient UI experiment, but not proof that an Assistant pipeline, actual mic transport, secure Frigate camera playback, or a native media-player endpoint will work.

## Testing plan when home

1. **Before installing any remapper**, install current diagnostic APK; open it and press the mic key, ⭐, Free TV, etc. Record keycodes, Assist intent result, audio input devices and RMS/peak values.
2. In Google TV remote settings, test what ⭐ can be mapped to. Prefer direct native app launch if offered, and confirm the original streaming app can be resumed.
3. If ⭐ cannot launch the target action, test tvQuickActions with a **spare noncritical key**, using the Accessibility permission setup and **Open App** action. Record whether pressed key is recognized and whether the TV returns to the prior app.
4. Only then try mapping the **Google Assistant microphone** key; record whether key is detectable, whether both stock Assistant and our app open, and whether voice data reaches the probe.
5. Separately test audio from the onn 4K Pro's built-in microphone (where available). A detected key alone is NOT proof of remote mic support.
6. Prefer no root and don't disable Google packages for initial experiments. Keep rollback simple: remove mapping/Accessibility permission, restore stock ⭐ shortcut.

### Results matrix to fill after test

| Input/approach | Key detected? | Starts app? | Remote mic audio actually captured? | Original TV app preserved? |
| --- | --- | --- | --- | --- |
| Native mic / Assistant button | TBD | TBD | TBD | TBD |
| Native ⭐ customizable button | TBD | TBD | N/A until audio tested | TBD |
| tvQuickActions spare-key remap | TBD | TBD | N/A until audio tested | TBD |
| tvQuickActions mic-key remap | TBD | TBD | TBD | TBD |
| Button Mapper alternative | TBD | TBD | TBD | TBD |
| onn 4K Pro built-in microphone | N/A | N/A | Separately test local audio | N/A |

## Useful sources

- tvQuickActions documentation: https://tvdevinfo.com/tvquickactions/mappings/ and https://tvdevinfo.com/tvquickactions/quick-start/
- App overview / supported actions: https://play.google.com/store/apps/details?id=dev.vodik7.tvquickactions.free
- Vendor docs acknowledge some buttons cannot be remapped normally: https://tvdevinfo.com/tvquickactions/quick-start/
- Advanced ADB options are Pro-only (avoid for first tests): https://tvdevinfo.com/tvquickactions/settings/adb/
- tvQuickActions recent overlay feature notes: https://tvdevinfo.com/tvquickactions/changelog/
- Onn 4K Pro star-button overview: https://www.pcworld.com/article/2351725/walmart-onn-google-tv-4k-pro-review.html
- Android assistant role reference: https://developer.android.com/reference/android/app/role/RoleManager

**Current state:** diagnostic APK built; hardware testing not yet done. This note is research and a fallback plan, not confirmation of third-party mic-button support.
