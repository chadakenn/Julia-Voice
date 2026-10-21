# Kellie's Voice: signed updates and repository rename

The Android application is still `com.example.tvvoiceprobe` and uses the same private release-signing key. **Do not uninstall it to switch names.** Install the same-signed APK on top of the installed Julia Voice app to retain its locally saved Home Assistant connection and preferences.

## Development APK on the TV

1. Open [Kellie's Voice GitHub Actions](https://github.com/chadakenn/Julia-Voice/actions/workflows/build-apk.yml).
2. Choose the latest successful **Build Kellie's Voice Signed APK** run.
3. Download the `kellies-voice-signed` artifact, extract `app-release.apk`, and install over the current app on the onn 4K Pro.
4. Confirm the original Home Assistant connection, ⭐ button, microphone, floating overlay, speech playback, icon and branded launch. If Android reports a signature mismatch, stop: **do not uninstall**.
5. Regular commits build a signed APK artifact but **do not publish a GitHub Release**.

## Repo rename safety

The current repository remains **[chadakenn/Julia-Voice](https://github.com/chadakenn/Julia-Voice)**. The planned new repository path is **`chadakenn/kellies-voice`**.

- The already-installed Julia Voice v0.9 updater expects the Julia-Voice release asset URL. Renaming the repo before upgrading all TVs may prevent the old updater from accepting new asset URLs.
- **First** install/test the signed v0.10 rebrand on TVs that need automatic updates.
- v0.10 checks `https://api.github.com/repos/chadakenn/kellies-voice/releases/latest` and falls back to `https://api.github.com/repos/chadakenn/Julia-Voice/releases/latest` while the old repository still exists.
- Downloaded APKs are only allowed from exact approved repository URL prefixes and must pass release tag, version code, checksum, size, HTTPS/host allowlist, and Android package signature checks. No silent installs.
- **Then** the repository owner can rename the repository at GitHub Settings → General → Repository name → `kellies-voice`.
- After the rename, local clones can change remote with `git remote set-url origin https://github.com/chadakenn/kellies-voice.git`. Avoid creating a new repo at the old name as it may break GitHub redirects.

## Public release, separately approved

Once tested on the physical TV, the owner can manually run **Build Kellie's Voice Signed APK** with **publish_release** checked. That publishes a GitHub Release containing `app-release.apk` and `update.json` (SHA-256 metadata). The standard push workflow leaves the published Release untouched.

**Public release status:** v0.6 is the last published version. v0.10 is a development rebrand until a separate public release is authorized and passes hardware tests.
