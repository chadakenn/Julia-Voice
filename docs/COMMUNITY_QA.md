# Controlled community QA preview, issues #67–#71

**This is not a release.** Install only with the owner's permission on test hardware. Preview application ID is `com.example.tvvoiceprobe`, versionCode is unchanged at **21**, and versionName is `0.16.4-community-qa1`.

## Obtaining the Android test build
Pull-request CI automatically builds a separate **debug APK** named `Kellies-Voice-test-apk`. It uses a different `.debug` application ID and does **not** replace the release-signed TV app.

For a release-key **in-place** preview, an authorized maintainer should open [GitHub Actions → Build Kellie's Voice Signed APK](https://github.com/chadakenn/Kellies-Voice/actions/workflows/build-apk.yml), select branch `test/community-67-71-qa1`, choose `publish_release=false`, and run the workflow. If the private signing secrets are configured and signature verification succeeds, the result is the private workflow artifact `kellies-voice-signed` containing `app-release.apk`. The workflow must not publish any GitHub Release from this branch. **Never share signing keys or tokens**.

Back up the existing app APK and HACS files first. Do **not** uninstall the signed app to install a test build; that would discard TV credentials/preferences. If the preview cannot install in place, stop and investigate signatures/version codes. Restoring a signed v0.16.4 APK with equal versionCode may require additional device-specific validation; do not promise a seamless rollback.

## Matching HACS component
This test branch includes modified `custom_components/kellies_voice` files. HACS **does not automatically install a preview branch**. Back up the original integration folder, then deliberately copy the matching test-branch component on a non-production HA instance and restart. The public v0.16.4 integration is **not** the matching preview for non-admin alerts or snapshot display controls.

## Test checklist
- **#67 Philips HDMI**: show live camera above HDMI and Netflix/TiviMate, confirm video moves where a software H.264 decoder exists and underlying picture remains visible during/after popup; snapshot fallback must be honest. Repeat hide/timeout.
- **#68 HA permissions**: use a dedicated non-admin HA account with read access to the TV's **TV alert position** entity. Check announce/camera/forecast/timer/hide through the scoped receiver, then revoke and confirm denial.
- **#69 pairing**: typo UUID should remain unverified, binary sensor off, last_seen=never. Correct ID + authenticated receiver turns on, disconnect turns off. This does **not** guarantee rendering of any popup.
- **#70 snapshots**: `kellies_voice.show_camera` with `show_header:false`, `image_fit:fit`, `refresh_seconds:3`, 4:3 image. No cropping, no flicker on repeated events; refresh stops after hide.
- **#71 microphone**: Philips with remote-submix-only input should get actionable error instead of bogus STT; connect USB mic to retest. onn 4K Pro must retain working built-in mic/STT/TTS.
- **Regression:** repeat with onn streaming video, timers, forecasts, announcements, TV alert enable/disable, reconnection, and multiple TVs. Do not activate unrelated wake-word/follow-up prototypes.

Share only redacted device model, firmware, APK version, HA version and concise symptoms. Keep all issues open until hardware QA passes.
