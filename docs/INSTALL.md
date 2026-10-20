# Install and update on the onn 4K Pro

This is a **foreground Android TV Assist alpha**. The current package ID is `com.example.tvvoiceprobe`. The latest user-tested features are HA token authentication and speech-to-text. **TV-speaker TTS and the compact UI still need confirmation on device.**

## Get the correct APK

1. Open [Build HA TV Voice Signed APK](../.github/workflows/build-apk.yml) via GitHub's **Actions** tab.
2. Select the **latest successful** run, preferably the latest release after any code change.
3. Under **Artifacts**, download **`onn-tv-ha-assist-signed`**. Sign into GitHub if the artifacts aren't visible.
4. Extract the ZIP. The file you transfer is **`app-release.apk`**, *not* the old `app-debug.apk`.
5. On a Samsung phone: install **Send Files to TV** on both the phone and onn device, connect both to the same home network, open Receive on the TV, and use Send on the phone to transfer the extracted APK. Open the transferred APK with a TV file manager.
6. Allow **Install unknown apps** for your chosen file manager when Google TV prompts for it, then install.

Use a USB flash drive or ADB (`adb install -r app-release.apk`) if phone transfer is troublesome.

## Crucial: one-time move from debug APK to signed APK

The earliest GitHub Actions builds used **disposable debug signing keys**. The new permanently signed `app-release.apk` has a *different* certificate. Android correctly refuses to install it on top of one of those original builds and may say **"conflicts with an existing package"** or **"another app"**.

**Keep the old app installed until the signed APK is downloaded and its build says Success.** Then:

1. In Google TV **Settings → Apps**, find **HA TV Voice Alpha** (older builds may be **TV Voice Probe**).
2. Uninstall that old, debug-signed app **once**.
3. Install the signed `app-release.apk`.
4. Reconfigure your Home Assistant connection and token. Uninstallation clears encrypted local settings and app backup is disabled.
5. From then on, use **only** `app-release.apk` artifacts signed with this repo's same permanent keystore. Install them **over** the existing signed app; no uninstall and no new token should be required.

If a *later signed update* conflicts again, **stop and investigate the keystore/alias/certificate** rather than uninstalling repeatedly. See [signing](SIGNING.md).

## Set up Home Assistant

1. Home Assistant needs a working **Assist pipeline** with STT, a conversation agent, and optionally TTS. The app uses the HA **preferred pipeline** for now.
2. Generate a **long-lived access token** from your own Home Assistant user profile's Security section; keep it private. Don't paste it in GitHub, issues, screenshots, or this chat.
3. Launch **HA TV Voice Alpha**. Select **SETTINGS → Configure Home Assistant**, enter its base URL including protocol and port, and the token; then choose **Save and connect**.
4. For local-only Home Assistant, `http://homeassistant.local:8123` may work if the TV resolves the hostname, **but HTTP sends the token and recorded audio unencrypted over the network**. Prefer HTTPS with a valid trusted certificate when available.
5. Use the Google TV smartphone remote/keyboard to make entering credentials easier; clipboard paste support varies. QR-code pairing has **not yet been implemented**.
6. Expect **Connected and authenticated** before talking.

## First conversation test

Use the normal remote's D-pad and OK (not its Google Assistant microphone button):

1. Select **TALK**.
2. Wait for **LISTENING** and speak toward the onn 4K Pro's **built-in microphone**.
3. Select **STOP**. Home Assistant should transcribe and answer.
4. The compact layout shows the **Home Assistant answer above what you said**. Confirm separately whether speech comes through the TV speakers. TTS playback has not yet been validated.
5. Select **SETTINGS** for reconnecting, credentials, new conversation or hardware microphone diagnostics.

Google Assistant currently intercepts the physical remote's mic button; it does **not** activate this app. The ⭐ shortcut and overlays are later milestones.

## Troubleshooting

See [Troubleshooting](TROUBLESHOOTING.md) for signing conflicts, failed authentication, microphone, audio playback, and screen issues, and [Testing](TESTING.md) for our verified hardware results.
