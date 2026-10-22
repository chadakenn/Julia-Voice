# Aurora Glass (proposed theme)

Optional midnight-blue and violet styling for the voice overlay and seven-day forecast card. Existing installs keep their current appearance; the theme does not enable listening or change forecast behavior.

After a matching Android build has been approved and installed, select **Settings → Overlay appearance & position → Appearance → Aurora Glass** and reopen the overlay. The same option is available in Home Assistant's **Overlay style** select after updating the matching integration and restarting HA. No scripts are needed for the theme; weather still uses the setup in [WEATHER_FORECAST.md](WEATHER_FORECAST.md).

For a layout inspired by the reference, select a bottom-left voice position and a top-right forecast position, adjusting sizes for the TV. Position choices remain separate settings. The theme's original/default voice position is bottom left. Explicit saved placement takes precedence.

Light is drawn inside each panel. It does not capture or blur the underlying streaming picture or place a wallpaper over the TV. Cameras keep their existing appearance. Waveform animation continues to represent assistant state, not microphone amplitude.

This branch is based on the weather fixes in PR #47. It does not incorporate PR #46's microphone changes. Integration conflicts with those changes still need review before any combined build. No release/version bump or install is included.

Physical onn QA remains pending: readability over bright TV content, D-pad focus on all four actions, long answers, seven-row forecast fit, all sizes/positions, and switching back to Copper Glass.
