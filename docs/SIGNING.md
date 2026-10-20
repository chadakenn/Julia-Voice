# Android APK signing for this TV app

## Why the original APK reports "conflicts with another app"

The original `app-debug.apk` from GitHub Actions was signed with a temporary Android debug key generated on each hosted build machine. Android will not install a new APK over an installed app with the same package ID but a different certificate. This is Android's security protection, NOT a Home Assistant authentication error.

The original app uses `android:allowBackup="false"` and Android Keystore encrypted credentials. If you uninstall it, the saved Home Assistant URL and token are lost.

## Permanent, private release signing

The build workflow now signs `app-release.apk` with an **identical signing key on every future run**, read from GitHub Actions encrypted repository secrets.

The user receives a backup ZIP outside this repository with:
- `ha-tv-voice-signing.p12`: private Android signing key backup (NEVER COMMIT)
- `TV_SIGNING_KEYSTORE_B64.txt`: the base64 of that signing key, to paste into GitHub Actions encrypted secret
- `TV_SIGNING_PASSWORD.txt`: randomly generated strong keystore password, to paste into GitHub Actions encrypted secret
- `README.txt`: instructions

The files must be protected like passwords; anyone who possesses both the keystore and password could create updates that Android treats as authorized.

1. In GitHub open **Settings > Secrets and variables > Actions** for this repo.
2. Add repository secret named exactly `TV_SIGNING_KEYSTORE_B64`. Paste **the entire single line** from the corresponding private `.txt` file.
3. Add repository secret named exactly `TV_SIGNING_PASSWORD`. Paste the entire password from its private `.txt` file.
4. Open **Actions > Build HA TV Voice Signed APK > Run workflow**, then wait for a green successful build.
5. Download artifact `onn-tv-ha-assist-signed`. Extract `app-release.apk` and transfer to the TV.
6. **First stable release only**: because the old debug certificate is unrelated, uninstall the old alpha app from TV settings, then install the new `app-release.apk`. This clears the saved HA login; configure once more.
7. On each subsequent update, install the newly built `app-release.apk` **over the existing signed app without uninstalling**. The Home Assistant settings remain intact.

Never upload the signing key/password to the public repo, an issue, a chat screenshot, or a third-party file-sharing service. Back up both privately. GitHub Actions secret values are not visible in the source code.

No release APK is available until BOTH required secrets have been configured. The workflow intentionally fails instead of producing another incompatible debug APK.

If the password or key is lost, future releases cannot update this signed app without a reinstall and associated loss of stored credentials.
