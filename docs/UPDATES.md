# In-app updates (v0.6)

**Current status:** v0.6 updater code is in the app and builds through GitHub Actions. TV installation and future upgrade flows still need hands-on testing. The old v0.5 APK cannot update itself because its code predates the updater.

## How it works for TV users

1. Install the **signed v0.6 `app-release.apk` over v0.5** once using the existing transfer method. **Do not uninstall** the currently signed app. Signed updates preserve Home Assistant token/settings.
2. Open **Julia Voice → SETTINGS → Check for app updates**. v0.6 also checks for releases approximately once per day when the full-screen app opens (not during a ⭐ shortcut launch).
3. If a **newer public stable GitHub Release** exists, the app offers its version and release notes. Choose **Download update** or **Later**.
4. The TV downloads `app-release.apk` directly to app-private cache over HTTPS. It verifies exact asset size and a SHA-256 checksum from the same public release's `update.json`.
5. When verified, choose **Open Android installer**. On the first attempt Android TV may open **Allow from this source / Install unknown apps**. Grant permission to **Julia Voice**, then return and select **SETTINGS → Install downloaded update**.
6. Accept Android's **Update** / **Install** prompt. **No root, unattended silent installation, or signing bypass.** Android also enforces package/signature compatibility. Continue using the app normally afterward.

The APK installer may require foreground user interaction; OTA does not mean silently updating in the background. The server's `/releases/latest` endpoint does not return drafts/prereleases. The app won't offer an installed or older `versionCode`, or APKs without the release integrity file.

The signed APK uses the **same private Android signing key stored in GitHub Actions secrets**, with versionCode increasing for each installable version. It keeps the existing application ID.

## Publishing a new stable release (owner)

**Only the approved release workflow publishes public updates.** Ordinary pushes still produce signed GitHub Actions ZIP artifacts, not updates the app offers. GitHub Releases must be intentionally published.

1. Edit `app/build.gradle`: increment `versionCode` (for example 6 → 7), change `versionName` to a new version whose prefix matches `N.N` or `N.N.N` (for example `0.7-feature-name`). Keep applicationId and signing key unchanged.
2. Commit and test the working app/build on the device. The ordinary build can run without publishing.
3. Open [Build Julia Voice Signed APK workflow](../.github/workflows/build-apk.yml) in **Actions**, click **Run workflow**, leave branch on **main**, then check **publish_release** (the checkbox that says *Publish a public GitHub Release after the signed APK passes verification*). Run it once.
4. The workflow rebuilds the APK, verifies the permanent signing key and APK signature, uploads the artifact, then (and only then) creates a **public GitHub Release** with:
   - `app-release.apk` (direct installer)
   - `update.json` (release tag, Android versionCode/name, SHA-256 of the APK)
5. The published Release appears at [GitHub Releases](../../releases). Apps with an older `versionCode` detect it on their next daily/explicit check.
6. Do not rerun the same version's release step or recycle tags. If it fails because a release already exists, increment versionName/versionCode in a future build.

## First v0.6 catch

**The repo had no GitHub Releases before the updater.** The first approved published v0.6 release gives future TV versions a stable endpoint but is **not itself a newer update for installed v0.6**, so its own UI should correctly report "You're up to date." To test download-and-install end-to-end, publish a **newer signed v0.7 release** after adding a legitimate change.

An `on: workflow_dispatch` run without the publish checkbox only builds the installer artifact ZIP, which is still useful for installing v0.6 once.

## Privacy and security

- Release metadata and APK are fetched only from approved GitHub HTTPS hosts; the user's **Home Assistant credentials are never sent to GitHub**.
- The downloaded APK must have the exact expected size and SHA-256. Android independently validates package identity and the permanent signing certificate before installation.
- Store the signing key and password only in GitHub Actions encrypted secrets and offline private backups, never source code.
- No arbitrary download URL, debug APK, update server on your LAN, or Android root access is required.
- If GitHub is unreachable, leave the existing app untouched; retry later.
