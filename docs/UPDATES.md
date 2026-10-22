# Kellie's Voice: signed APK updates

**Canonical GitHub repository:** [chadakenn/Kellies-Voice](https://github.com/chadakenn/Kellies-Voice), renamed on 2026-10-21. GitHub release asset URLs preserve that exact capitalization.

The Android package ID remains `com.example.tvvoiceprobe`, and the original private release-signing key remains in use. **Never uninstall to rebrand.** Install a same-signed update over the existing app to preserve Home Assistant credentials, settings and the ⭐ shortcut.

## First update after the repository rename

The installed v0.10 and earlier updater may reject a release asset under the renamed path, because it validates the exact filename and case-sensitive repository prefix. GitHub redirects alone do not cure that limitation.

1. Visit [GitHub Actions for Kellie's Voice](https://github.com/chadakenn/Kellies-Voice/actions/workflows/build-apk.yml).
2. Open the latest successful **Build Kellie's Voice Signed APK** run and download the `kellies-voice-signed` artifact.
3. Extract `app-release.apk`. Install it **over** the existing app on the onn 4K Pro, without uninstalling. If Android reports a signature mismatch, stop and report it; do not clear app data.
4. Confirm the saved Home Assistant connection, ⭐ overlay shortcut, microphone permission, voice responses and warm copper branding.
5. Test **SETTINGS → Check for app updates**. A successful check with only the older public v0.6 published should report that v0.11 is up to date.

## Release URL compatibility

The updater requests `https://api.github.com/repos/chadakenn/Kellies-Voice/releases/latest` and allows release APK assets only at `https://github.com/chadakenn/Kellies-Voice/releases/download/`.

The old **Julia-Voice** URL is no longer a fallback. It did not help TVs still running v0.10 or earlier — those builds have the old prefix compiled in and still need the one-time sideload above. A fallback in the new app only mattered when `Kellies-Voice` itself returned 404, which would also let a recreated `Julia-Voice` repository offer a download. After the SHA-256 check, the app now rejects an APK whose package name is not `com.example.tvvoiceprobe` or whose signing certificate does not match the installed app, before Android's installer opens.

The GitHub rename has **already occurred**. Do not create a new repository at the old `Julia-Voice` path. For local clones, use `git remote set-url origin https://github.com/chadakenn/Kellies-Voice.git`.

## Public release, only with owner approval

Normal pushes create a **signed development APK artifact**, not a public GitHub Release. After physical TV testing, the owner can run **Build Kellie's Voice Signed APK** manually with `publish_release` checked. That workflow builds the signed APK, verifies the signature, and publishes `app-release.apk` and checksum metadata `update.json`.

**v0.15.0 release:** Publication was owner-authorized. A one-time `[publish-v0.15.0]` gated main-branch workflow path builds/tests the latest app, signs and verifies its APK, and uploads `app-release.apk` with SHA-256 `update.json`. Later releases still require a new owner-authorized version bump/manual workflow.

**v0.15.1 patch release:** Adds a manual pairing fallback and the local Home Assistant brand icon. It retains the same signed Android package identity and publishes after Android and HA tests. HACS may still show a generic icon due to an upstream HACS frontend issue, while Home Assistant's native integration page uses the bundled brand asset.

## v0.16.0: update directly from Kellie's Voice (October 2026)

With version 0.15.1 already installed, open the **Kellie's Voice** TV app → **SETTINGS → Check for app updates**. When v0.16.0 appears, choose **Download update** and then **Install downloaded update**, accepting Android's normal package-installer prompt. You may have to grant Kellie's Voice **Install unknown apps** once. Do not uninstall or clear app data.

The app's built-in updater reads the public stable GitHub Release, requires `app-release.apk` and `update.json`, compares Android `versionCode` values, validates the SHA-256 checksum, package ID and installed signing certificate, then delegates installation to Android. A GitHub Actions artifact **without** a published Release is **not** visible to in-app updates. Developer/prerelease builds are deliberately not offered by the stable update endpoint.

After updating the TV, in **HACS** update the Home Assistant Kellie's Voice integration to **v0.16.0**, then restart HA. The new live camera action is experimental until hardware-tested; announcement and Frigate snapshot popups remain the tested default. The OTA update does not replace or upgrade the HA integration automatically.

The release workflow publishes only when explicitly approved and the signed APK passes tests/signature verification. Normal GitHub pushes continue producing signed artifacts but do not publish user-facing app updates.

## v0.16.1 live streaming TV compatibility update

Use `Kellie's Voice → Settings → Check for app updates` after the v0.16.1 GitHub Release is published. This patch uses `TextureView` for Android TV overlays and shows a real, **clearly labeled snapshot fallback** when live streaming fails. The live camera is still experimental and needs moving-video proof on an onn TV. No changes to the Frigate person popups, HA pairing or TV announcement services. The HACS integration does not require a new version for this Android-only update.

## v0.16.2: dismiss camera by voice, timed camera requests

The new signed Android TV update can receive a `kellies_voice_hide_camera` event and immediately dismiss only the active live or snapshot camera popup. A matching v0.16.2 HACS integration registers `kellies_voice.hide_camera`, with optional `device_id` to target a single paired TV. Regular announcements and voice overlay remain untouched. The player stops and releases its resources. The existing `show_live_camera` action accepts durations from 15 to 180 seconds, including 30 seconds. Scripts exposed to Assist can optionally accept a `duration_seconds` field, although named 30-second scripts may be more reliable for voice agents that cannot pass script parameters.

**Upgrade sequence:** TV app Settings → Check for app updates → install v0.16.2 over v0.16.1; **HACS** → Kellie's Voice integration → update to v0.16.2 → restart Home Assistant; open the TV app so the event receiver reconnects. Do not uninstall or clear credentials.

**Test:** call `kellies_voice.show_live_camera` with `duration_seconds: 30` and confirm it automatically disappears; during a longer 90-second camera view call `kellies_voice.hide_camera` and confirm immediate dismissal without changing the TV playback. Confirm the command does not dismiss an on-screen text-only announcement. Use voice-assistant scripts for natural phrases.
