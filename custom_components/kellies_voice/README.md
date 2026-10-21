# Kellie's Voice for Home Assistant

This integration only changes overlay layout. It does not start the microphone, replace the TV login, or install an APK.

Home Assistant never connects to the TV. The TV advertises itself, you click **Add**, and the TV reads these controls with the Home Assistant login it already has.

## Add it

1. Copy this folder to `config/custom_components/kellies_voice` on the Home Assistant machine.
2. Restart Home Assistant.
3. On the TV, open **Kellie's Voice → Settings → Home Assistant layout** and turn it on. Leave the app open.
4. In Home Assistant, the TV shows up as a discovered device. Click **Add**.

The device page then has position, size, style, auto-hide, and auto-listen.

The first time the TV sees a new control, it copies the layout already saved on the TV into Home Assistant. After that, changes in Home Assistant win. The on-TV menu still works and updates Home Assistant too.

If the app is closed, a change waits until the next time Kellie's Voice connects. Turning the switch off on the TV stops discovery and ignores later Home Assistant layout changes.
