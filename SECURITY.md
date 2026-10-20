# Security and privacy

This repo is public, but **your Home Assistant deployment, access tokens, signing material, camera credentials and local recordings are private**.

## Home Assistant connection

- The Android app authenticates to `/api/websocket` using the long-lived token **you enter on your TV**, not a token bundled in the APK.
- Local credentials are encrypted at rest using an **Android Keystore-backed key**; Android backup of app data is disabled.
- Pressing **TALK** intentionally streams microphone samples to the configured HA server for processing. The diagnostic MIC/VOICE_RECOGNITION tests only compute signal levels and don't upload or save recordings.
- Use an HTTPS URL with a valid trusted certificate when feasible. **The currently allowed HTTP/WS connection sends your access token and microphone audio unencrypted on the network.** Only consider it on a trusted, isolated LAN and plan a trusted TLS endpoint. We must not add a trust-all-certificate bypass.
- In the future, any remote UI pairing must use a short-lived authenticated channel. **Never put the bearer token in a QR code or public URL.**
- TTS sources are constrained to the configured HA server; only explicit HA TTS proxy paths may be rewritten to it. External playback and Frigate cameras need separate authorization and allowlists.

## Android signing key

- The current GitHub Actions release-signing key is recovered from encrypted repository secrets. **No keystore, password or base64 private key belongs in commits, issues, logs, artifacts, or screenshots.**
- Keep a private backup of the original keystore and its password. Without it, future installs cannot safely update over the already signed app.
- The GitHub workflow validates the keystore alias and verifies the release APK signature before publishing.
- **Never upload the private signing-key backup ZIP to this public repository.**
- A public signed release APK is safe to distribute because it contains only the **public signing certificate**, not the private signing key.

## Reporting a possible vulnerability

Please do not publish exploit details, tokens, private HA URLs or credentials in a public GitHub issue. Instead contact the repository owner through an appropriate private channel, or use GitHub's private vulnerability reporting if enabled. Include a short description, affected app version and reproduction steps without secrets.

## Scope and known limitations

This is an **alpha**, not a production smart-home security boundary. The app doesn't yet have secure account pairing, device revocation UX beyond clearing saved credentials, full streaming app overlay, a HA media-player service, Frigate PiP, a background service or remote microphone-button takeover.

If a device or APK is compromised, revoke the affected Home Assistant token from the Home Assistant user profile, then create a new one. Removing the saved TV connection does **not** revoke a token at Home Assistant.
