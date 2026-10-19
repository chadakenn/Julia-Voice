# onn TV Voice Remote Probe

An experimental Android TV diagnostic app for the onn 4K Pro and similar Google TV devices.

**Goal:** determine whether an app can receive the remote microphone button, become the default assistant, and capture microphone audio. The longer-term project is a push-to-talk Home Assistant Assist client.

This prototype is **not** a working Home Assistant voice satellite. It does not save or transmit microphone audio, require root, or replace the stock TV launcher.

## First test

1. Open **Actions** and select **Build Android TV Probe APK** to build a debug APK.
2. Download the artifact from the successful workflow run and extract `app-debug.apk`.
3. On the onn 4K Pro, enable Android debugging (Developer options), then use ADB to install the APK: `adb install -r app-debug.apk`.
4. Open **TV Voice Probe**. Press the remote microphone button, check the assistant role, and test microphone levels.
5. Record the on-screen results, especially whether the key is intercepted and whether audio is routed from the remote or the box itself.

**Security:** Keep Home Assistant access tokens off GitHub. This diagnostic does not connect to Home Assistant.

Development status: early prototype, not yet hardware-tested.
