# On-device wake word

This is not in the installed v0.16.4 release. It starts only after you install a build that contains it and turn it on yourself.

## What it does

Kellie's Voice can listen on the TV for **Okay Nabu** using microWakeWord, the same small on-device model the Home Assistant Android app uses. The model runs on the TV. No audio is sent to Home Assistant until the phrase is recognized and the normal voice overlay opens. The overlay then uses the existing Assist connection.

The switch is **off** by default.

- Settings → **Wake word** → **Turn wake word ON** starts it.
- The same menu turns it off and releases the microphone.
- A reboot, an app update, or the TV-alert receiver does **not** start it.
- Android's microphone privacy indicator stays on the whole time the listener is armed.
- The listener lets go of the microphone before the voice overlay records.
- A recognized **Okay Nabu** is itself an explicit request to begin listening once. The normal voice overlay starts Assist capture **even when the separate Auto-listen setting is OFF**, and stops on detected speech silence. It does **not** toggle Auto-listen persistently.
- The one-shot request expires if Home Assistant cannot reconnect promptly (about 10 seconds). STOP, CLOSE, permission loss or connection timeout must not unexpectedly start the microphone later.

## What it does not do

- It does not use the low-power chip that hears "Hey Google". Detection runs on the TV's main processor.
- It does not add "Hey Kellie", "Hey Jarvis", or "Hey Mycroft".
- It does not change Home Assistant. There is no new service and no new entity.
- It does not require saying the assistant's name in ordinary mic-button commands. The wake phrase is only needed when you **opt in to hands-free wake listening**.
- It has not been tested on an onn 4K Pro or any other television. A quiet room is the first check. The same phrase with the TV playing is the second, because the microphone hears the speakers.

The capture rate is 16 kHz. If this TV cannot open a 16 kHz microphone, the listener turns itself back off and leaves a notification. It does not guess another sample rate.

## Engine

The native code under `app/src/main/cpp` is the Home Assistant companion `microwakeword` module (Apache-2.0). CMake downloads TensorFlow Lite Micro when an APK is built. The pull-request workflow compiles that engine into the debug APK. The signed release workflow compiles it into the release APK. Passing `-PskipWakeNative=true` skips the native compile for a host unit-test run.
