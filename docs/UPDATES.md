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

## Current release status

The **signed v0.6 release is publicly available** at [GitHub Releases](https://github.com/chadakenn/onn-tv-voice-remote./releases/tag/v0.6). An installed v0.6 APK should correctly report **You're up to date** until a newer version is published.

The **Julia Voice v0.7 branding APK has passed signed GitHub Actions builds**, but it has **not been published as a stable GitHub Release or tested on the onn 4K Pro**. Once we validate the new UI and deliberately publish v0.7, an installed v0.6 app can discover, download and open its installer using the updater.

A manual `workflow_dispatch` run **without** the publish checkbox produces a signed APK in the `julia-voice-signed` artifact, but will not announce it to installed users.

## Planned GitHub repository rename: Julia Voice

**Target repository:** `chadakenn/julia-voice` (not renamed yet).

The published **v0.6** updater contains an exact hard-coded URL check for release assets under the original GitHub repository. GitHub redirects alone **may not fix** this because the release API can return asset URLs under the new repository name. **Do not rename the repository until all TVs you want to update automatically have moved off v0.6.** Otherwise those TVs may require a one-time manual APK installation.

The **v0.7 development source** has a migration-safe updater that prefers `chadakenn/julia-voice`, falls back to the original repository while it still exists, and permits signed-APK asset URLs from **only those two exact repository paths**. It still validates versionCode, exact asset filename and size, SHA-256 checksum, allowed HTTPS hosts, and the Android signing certificate enforced by the installer.

**Safe handoff sequence:**

1. Complete the **signed v0.7 field test** on onn hardware; verify it checks the published v0.6 release and displays "You're up to date" when appropriate.
2. Publish v0.7 as a GitHub Release in the **original repository** through the owner-approved workflow, with `publish_release` enabled.
3. Upgrade all TVs from v0.6 to v0.7 using the in-app update and Android installer **before changing the repository name**. If this fails, do a one-time manual v0.7 signed APK install over v0.6; never uninstall a same-signed version.
4. The owner renames the repository in **Settings → General → Repository name → julia-voice → Rename**. The GitHub connector cannot make the administrative rename in this chat.
5. Verify both the new GitHub Releases API URL and the v0.7 updater. Update all README/workflow/release links to the new canonical URL and update any local Git clones with `git remote set-url origin https://github.com/chadakenn/julia-voice.git`.
6. Never recreate a different repository at the old name; doing so can break GitHub's previous-path redirects. Keep the Android applicationId and signing key unchanged.

**Not yet verified:** a complete in-app update before and after repository rename on the onn 4K Pro. Do not assume old v0.6 installations will update after the rename.

## Privacy and security

- Release metadata and APK are fetched only from approved GitHub HTTPS hosts; the user's **Home Assistant credentials are never sent to GitHub**.
- The downloaded APK must have the exact expected size and SHA-256. Android independently validates package identity and the permanent signing certificate before installation.
- Store the signing key and password only in GitHub Actions encrypted secrets and offline private backups, never source code.
- No arbitrary download URL, debug APK, update server on your LAN, or Android root access is required.
- If GitHub is unreachable, leave the existing app untouched; retry later.
