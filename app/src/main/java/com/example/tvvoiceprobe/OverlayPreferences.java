package com.example.tvvoiceprobe;

import android.content.Context;
import android.content.SharedPreferences;

/** Non-secret, device-local TV UI preferences. Credentials remain in CredentialStore. */
public final class OverlayPreferences {
    public static final String SKIN_GLASS = "glass";
    public static final String SKIN_CLASSIC = "classic";
    private static final String PREFS = "ha_tv_voice_overlay_options";
    private static final String KEY_SKIN = "skin";
    private static final String KEY_AUTO = "auto_listen";
    private static final String KEY_HIDE = "auto_hide";
    private static final String KEY_MAIN_SHORTCUT = "main_launcher_opens_overlay";

    private final SharedPreferences preferences;

    public OverlayPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String skin() {
        String value = preferences.getString(KEY_SKIN, SKIN_GLASS);
        return SKIN_CLASSIC.equals(value) ? SKIN_CLASSIC : SKIN_GLASS;
    }

    public void setSkin(String value) {
        preferences.edit().putString(KEY_SKIN,
                SKIN_CLASSIC.equals(value) ? SKIN_CLASSIC : SKIN_GLASS).apply();
    }

    public boolean autoListen() { return preferences.getBoolean(KEY_AUTO, true); }
    public void setAutoListen(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO, enabled).apply();
    }

    public boolean mainLauncherOpensOverlay() {
        return preferences.getBoolean(KEY_MAIN_SHORTCUT, false);
    }

    public void setMainLauncherOpensOverlay(boolean enabled) {
        preferences.edit().putBoolean(KEY_MAIN_SHORTCUT, enabled).apply();
    }

    public boolean autoHide() { return preferences.getBoolean(KEY_HIDE, true); }
    public void setAutoHide(boolean enabled) {
        preferences.edit().putBoolean(KEY_HIDE, enabled).apply();
    }
}
