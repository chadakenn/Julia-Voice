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

The **signed v0.6 release is publicly available** at [GitHub Releases](https://github.com/chadakenn/Julia-Voice/releases/tag/v0.6). An installed v0.6 APK should correctly report **You're up to date** until a newer version is published.

The **Julia Voice v0.7 branding APK has passed signed GitHub Actions builds**, but it has **not been published as a stable GitHub Release or tested on the onn 4K Pro**. Once we validate the new UI and deliberately publish v0.7, an installed v0.6 app can discover, download and open its installer using the updater.

A manual `workflow_dispatch` run **without** the publish checkbox produces a signed APK in the `julia-voice-signed` artifact, but will not announce it to installed users.

## Repository rename completed: `chadakenn/Julia-Voice`

The repository was renamed on **2026-10-20**. GitHub's canonical home for the project is [Julia Voice](https://github.com/chadakenn/Julia-Voice), including its [Releases](https://github.com/chadakenn/Julia-Voice/releases). GitHub may redirect links to the previous repository name.

**Important existing-version limitation:** The **published v0.6 updater** hard-codes its original release asset URL prefix. GitHub now returns **`chadakenn/Julia-Voice`** in release-asset URLs, which v0.6 may reject even when GitHub redirects work. **Do not rely on v0.6's in-app update for the first move to v0.7.**

The updated **v0.7 source** uses GitHub's exact canonical capitalization, `Julia-Voice`, and recognizes the previous repository URL prefix only as a tightly scoped compatibility exception. APK downloads still require approved HTTPS hosts, exact release filenames, metadata tag/version, size, SHA-256 hash and Android's package-signature verification. GitHub Actions builds v0.7 with the existing package ID and private signing key.

**Recovery / upgrade sequence:**

1. Download the latest [**signed `julia-voice-signed` artifact](https://github.com/chadakenn/Julia-Voice/actions/workflows/build-apk.yml) from a successful run of the current workflow; unzip `app-release.apk`. This is the **development v0.7 APK** until a deliberate public release is published.
2. On the onn 4K Pro, install the APK **over** the existing installed app. Do **not** uninstall Julia Voice, because that would erase its locally saved Home Assistant connection and preferences. Android should accept an update when the package ID and signing key match.
3. Verify that Julia Voice opens, the new launcher icon and overlay work, the saved Home Assistant connection remains, and **SETTINGS → Check for app updates** successfully checks the [new Releases endpoint](https://api.github.com/repos/chadakenn/Julia-Voice/releases/latest). While v0.7 is not yet published, the currently published v0.6 should result in **You're up to date**.
4. Only after testing, run **Build Julia Voice Signed APK** with the owner-only `publish_release` checkbox enabled to publish **v0.7**, so subsequent updates can happen through the app. Normal pushes do not publish releases.
5. For local checkouts, use `git remote set-url origin https://github.com/chadakenn/Julia-Voice.git`. Avoid recreating a repository at the old address, which can interfere with redirects.

**Still needs hardware verification:** successful over-the-top installation and in-app update checks on the onn 4K Pro after renaming. The above is a migration plan, not a claim that the device has already passed these tests.

## Privacy and security

- Release metadata and APK are fetched only from approved GitHub HTTPS hosts; the user's **Home Assistant credentials are never sent to GitHub**.
- The downloaded APK must have the exact expected size and SHA-256. Android independently validates package identity and the permanent signing certificate before installation.
- Store the signing key and password only in GitHub Actions encrypted secrets and offline private backups, never source code.
- No arbitrary download URL, debug APK, update server on your LAN, or Android root access is required.
- If GitHub is unreachable, leave the existing app untouched; retry later.
