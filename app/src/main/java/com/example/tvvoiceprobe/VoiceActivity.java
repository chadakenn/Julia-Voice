package com.example.tvvoiceprobe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

/**
 * Android TV D-pad-first Home Assistant Assist UI with explicitly enabled
 * overlay prototype. No launcher replacement, wake word, or Frigate PiP.
 */
public class VoiceActivity extends Activity implements HaAssistClient.Events {
    private static final int MIC_PERMISSION_REQUEST = 17;
    private static final int OVERLAY_MIC_PERMISSION_REQUEST = 18;
    private static final int OVERLAY_SETTINGS_REQUEST = 19;
    private final List<Button> buttons = new ArrayList<>();
    private int selected = 0;
    private TextView status;
    private TextView recognized;
    private TextView response;
    private TextView connection;
    private CredentialStore store;
    private HaAssistClient assist;
    private CredentialStore.Connection settings;
    private MediaPlayer player;
    private boolean playing;
    private boolean warningPending;
    private UpdateManager updater;
    private boolean manualUpdateCheck;
    private boolean transparentShortcut;
    private boolean initialConnectionRequested;

    @Override protected void onStart() {
        super.onStart();
        // Reuse saved credentials without asking for a manual Reconnect click.
        // No microphone is activated here, just the authenticated socket.
        if (transparentShortcut || assist == null || settings == null) return;
        if (!initialConnectionRequested) {
            initialConnectionRequested = true;
            assist.connect(settings.url, settings.token);
        } else {
            assist.ensureConnected();
        }
    }

    @Override public void onCreate(Bundle state) {
        // The onn ⭐ picker starts the package's main Activity. When its
        // explicit overlay fallback is enabled, draw no full-screen UI at all.
        // Changing the window theme before super.onCreate avoids the flash of
        // the ordinary TV screen during the service's foreground startup.
        boolean shortcut = shouldLaunchOverlayFromLauncher(getIntent());
        if (shortcut) setTheme(R.style.KellieVoiceShortcutTransparentTheme);
        super.onCreate(state);
        if (shortcut && canStartOverlayImmediately()) {
            transparentShortcut = true;
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            View transparentWindow = new View(this);
            setContentView(transparentWindow);
            transparentWindow.post(this::startTransparentShortcutOverlay);
            return;
        }
        store = new CredentialStore(this);
        assist = new HaAssistClient(this, this);
        updater = new UpdateManager(this, new UpdateManager.Listener() {
            @Override public void onUpToDate(String message) {
                rememberUpdateCheck();
                if (manualUpdateCheck) status(message);
                manualUpdateCheck = false;
            }
            @Override public void onAvailable(String version, String notes) {
                rememberUpdateCheck();
                manualUpdateCheck = false;
                if (isFinishing() || isDestroyed()) return;
                new AlertDialog.Builder(VoiceActivity.this)
                        .setTitle("Kellie's Voice update available: " + version)
                        .setMessage("Download the signed update from GitHub Releases?\n\n" +
                                (notes == null ? "" : notes.substring(0, Math.min(notes.length(), 700))))
                        .setNegativeButton("Later", null)
                        .setPositiveButton("Download update", (dialog, choice) -> {
                            status("Downloading verified update...");
                            updater.download();
                        })
                        .show();
            }
            @Override public void onProgress(String message) { status(message); }
            @Override public void onDownloaded() {
                status("Update verified. Ready to install.");
                promptInstallUpdate();
            }
            @Override public void onError(String message) {
                if (manualUpdateCheck) status(message);
                else if (!message.contains("No public GitHub Release")) status("Updater: " + message);
                manualUpdateCheck = false;
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(34, 22, 34, 22);
        root.setBackgroundColor(0xff1B1412);

        TextView title = label("KELLIE'S VOICE   ✦   HOME ASSISTANT", 24, 0xffE9B390);
        root.addView(title);

        connection = label("Home Assistant: not configured", 15, 0xffE8D7C9);
        root.addView(connection);

        status = label("Connect, then press TALK.", 17, 0xffE6AB82);
        root.addView(status);

        // Prioritize the conversation on small TV screens. The large seven-button
        // diagnostic layout hid all but the first line of the recognized speech.
        ScrollView scroller = new ScrollView(this);
        scroller.setFocusable(false);
        scroller.setFillViewport(true);
        LinearLayout history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        history.setPadding(10, 6, 10, 6);

        history.addView(label("HOME ASSISTANT SAYS", 16, 0xffC59A80));
        response = label("Your answer will appear here.", 25, 0xffF7E8DB);
        history.addView(response);

        history.addView(label("YOU SAID", 16, 0xffC59A80));
        recognized = label("Nothing yet", 21, 0xffffffff);
        history.addView(recognized);

        scroller.addView(history);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));

        // Three controls in one bottom row leave most of the screen for the
        // actual Home Assistant response. The other actions live in SETTINGS.
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        addButton(toolbar, "TALK", v -> startTalk());
        addButton(toolbar, "STOP", v -> assist.finishTalking());
        addButton(toolbar, "SETTINGS", v -> showSettingsMenu());
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));

        // A short, focus-safe welcome view on ordinary full-screen launches.
        // Keep the existing root ready underneath, so HA configuration and
        // microphone behavior remain untouched.
        FrameLayout page = new FrameLayout(this);
        page.addView(root, new FrameLayout.LayoutParams(-1, -1));
        KellieSplashView splash = new KellieSplashView(this);
        page.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        setContentView(page);
        try { settings = store.load(); }
        catch (Exception e) { status("Saved connection couldn't be decrypted. Configure it again."); }
        updateConnectionLabel();
        if (!buttons.isEmpty()) buttons.get(0).post(() -> focus(0));
        // Some onn firmware shows only one entry per package in the ⭐ picker.
        // Launchers may strip CATEGORY_LAUNCHER/LEANBACK_LAUNCHER on the actual
        // start intent, so the explicit user opt-in + ACTION_MAIN is sufficient.
        if (shouldLaunchOverlayFromLauncher(getIntent())) {
            // ⭐ shortcuts should never be slowed by the splash.
            page.removeView(splash);
            root.post(this::launchOverlay);
        } else {
            splash.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                splash.animate().alpha(0f).setDuration(260)
                        .withEndAction(() -> page.removeView(splash)).start();
            }, 1050L);
            long now = System.currentTimeMillis();
            android.content.SharedPreferences checks =
                    getSharedPreferences("tv_voice_update_checks", MODE_PRIVATE);
            if (now - checks.getLong("last_checked", 0) >= 24L * 60 * 60 * 1000) {
                root.post(() -> updater.check());
            }
        }
    }

    private void rememberUpdateCheck() {
        getSharedPreferences("tv_voice_update_checks", MODE_PRIVATE)
                .edit().putLong("last_checked", System.currentTimeMillis()).apply();
    }

    private boolean canStartOverlayImmediately() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || !Settings.canDrawOverlays(this)) {
            return false;
        }
        try {
            return new CredentialStore(this).load() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void startTransparentShortcutOverlay() {
        if (isFinishing() || isDestroyed()) return;
        try {
            // Start from the foreground Activity before returning to the
            // video; the service promotes itself to microphone foreground.
            startForegroundService(new Intent(this, VoiceOverlayService.class));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed() && transparentShortcut) {
                    moveTaskToBack(true);
                }
            }, 750L);
        } catch (Exception ignored) {
            // On restricted firmware, recover to the real UI instead of
            // leaving the user with an invisible application.
            openNormalAppFromShortcut();
        }
    }

    private void openNormalAppFromShortcut() {
        Intent open = new Intent(this, VoiceActivity.class);
        open.setAction("com.example.tvvoiceprobe.OPEN_SETTINGS");
        startActivity(open);
        finish();
    }

    private boolean shouldLaunchOverlayFromLauncher(Intent intent) {
        // OPEN_SETTINGS and ordinary explicit activities must open the full UI.
        // The opt-in also prevents microphone startup on an ordinary install.
        return intent != null
                && Intent.ACTION_MAIN.equals(intent.getAction())
                && new OverlayPreferences(this).mainLauncherOpensOverlay();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // If Android reuses a running instance, onCreate is not called.
        // Handle a new launcher/⭐ command without requiring a fresh Activity.
        if (transparentShortcut) {
            if (shouldLaunchOverlayFromLauncher(intent)) {
                getWindow().getDecorView().post(this::startTransparentShortcutOverlay);
            } else {
                openNormalAppFromShortcut();
            }
        } else if (shouldLaunchOverlayFromLauncher(intent)) {
            getWindow().getDecorView().post(this::launchOverlay);
        }
    }

    private TextView label(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 7, 0, 7);
        t.setFocusable(false);
        return t;
    }

    private void addButton(LinearLayout parent, String value, View.OnClickListener action) {
        Button b = new Button(this);
        int index = buttons.size();
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(0xffffffff);
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setBackgroundTintList(ColorStateList.valueOf(0xff62453B));
        b.setOnClickListener(action);
        b.setOnFocusChangeListener((v, focused) -> {
            if (focused) selected = index;
            b.setBackgroundTintList(ColorStateList.valueOf(focused ? 0xffBA785B : 0xff62453B));
        });
        buttons.add(b);
        parent.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private void focus(int index) {
        if (buttons.isEmpty()) return;
        selected = Math.max(0, Math.min(index, buttons.size() - 1));
        buttons.get(selected).requestFocus();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !buttons.isEmpty()) {
            View now = getCurrentFocus();
            if (!(now instanceof Button) || !buttons.contains(now)) {
                buttons.get(selected).post(() -> focus(selected));
            }
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                focus(selected + 1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_LEFT) {
                focus(selected - 1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A) {
                if (!buttons.isEmpty()) buttons.get(selected).performClick();
                return true;
            }
        }
        // Ignore key release for navigation/selection to avoid duplicate activation.
        if (code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_LEFT
                || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void updateConnectionLabel() {
        if (connection == null) return;
        connection.setText(settings == null
                ? "Home Assistant: not configured"
                : "Home Assistant: " + settings.url + (assist.isConnected() ? "  ✓ connected" : "  (not connected)"));
    }

    /**
     * Same copper-and-charcoal visual language as the TV overlay.
     * All menus are navigable with the onn D-pad and remain local-only.
     */
    private void showCopperMenu(String heading, String subtitle,
            String[] entries, java.util.function.IntConsumer onSelect) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {0xff2B1B16, 0xff17100E});
        background.setCornerRadius(pad);
        background.setStroke(Math.max(1, pad / 16), 0xffB98465);
        body.setBackground(background);
        TextView title = label(heading, 25, 0xffF4D3BB);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        body.addView(title);
        TextView hint = label(subtitle, 14, 0xffCBA68F);
        hint.setPadding(0, 0, 0, pad / 2);
        body.addView(hint);
        Button first = null;
        for (int i = 0; i < entries.length; i++) {
            final int item = i;
            Button option = copperMenuButton(entries[i]);
            option.setOnClickListener(v -> {
                dialog.dismiss();
                onSelect.accept(item);
            });
            LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(-1, -2);
            row.bottomMargin = pad / 4;
            body.addView(option, row);
            if (first == null) first = option;
        }
        Button done = copperMenuButton("←  Back / Done");
        done.setOnClickListener(v -> dialog.dismiss());
        body.addView(done, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(body);
        dialog.setView(scroll, 0, 0, 0, 0);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int dpWidth = Math.round(720 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(Math.min(dpWidth, screenWidth - pad * 2),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        if (first != null) first.requestFocus();
    }

    private Button copperMenuButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(0xffF9E7DA);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(22, 10, 22, 10);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setBackground(menuRowBackground(false));
        b.setOnFocusChangeListener((v, focused) ->
                b.setBackground(menuRowBackground(focused)));
        return b;
    }

    private GradientDrawable menuRowBackground(boolean focused) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(focused ? 0xffA66A4B : 0xff493128);
        int dp = Math.round(1 * getResources().getDisplayMetrics().density);
        shape.setCornerRadius(Math.max(12, dp * 12));
        shape.setStroke(Math.max(1, dp), focused ? 0xffF6D1B4 : 0xff79513D);
        return shape;
    }

    private void showSettingsMenu() {
        String[] entries = new String[] {
                "Home Assistant connection",
                "Connect / reconnect",
                "Start new conversation",
                "Hardware microphone diagnostics",
                "Open floating voice overlay",
                "Overlay appearance & position",
                "Check for app updates",
                "Install downloaded update",
                "Forget saved connection"
        };
        showCopperMenu("KELLIE'S VOICE  ✦  SETTINGS",
                "Warm, simple controls for your TV. Choose with the remote.", entries,
                which -> {
                    switch (which) {
                        case 0: configure(); break;
                        case 1: connect(); break;
                        case 2: newConversation(); break;
                        case 3: startActivity(new Intent(this, MainActivity.class)); break;
                        case 4: launchOverlay(); break;
                        case 5: showOverlayPreferences(); break;
                        case 6:
                            manualUpdateCheck = true;
                            status("Checking GitHub Releases...");
                            updater.check();
                            break;
                        case 7: promptInstallUpdate(); break;
                        case 8: confirmForget(); break;
                        default: break;
                    }
                });
    }

    private void promptInstallUpdate() {
        if (isFinishing() || isDestroyed()) return;
        if (!updater.downloadedExists()) {
            status("No downloaded update. First choose Check for app updates.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Install verified Kellie's Voice update?")
                .setMessage("Android will ask you to approve the package update. " +
                        "Your Home Assistant login and preferences should be retained " +
                        "because the release uses our permanent signing key.")
                .setNegativeButton("Later", null)
                .setPositiveButton("Open Android installer", (dialog, which) -> updater.install())
                .show();
    }

    private void showOverlayPreferences() {
        OverlayPreferences prefs = new OverlayPreferences(this);
        String[] options = {
                "Position: " + OverlayPreferences.positionLabel(prefs.position()),
                "Size: " + OverlayPreferences.sizeLabel(prefs.size()),
                "Auto-hide after answer: " + prefs.hideLabel(),
                "Appearance: " + (OverlayPreferences.SKIN_GLASS.equals(prefs.skin())
                        ? "Copper Glass" : "Compact Charcoal"),
                "Auto-listen on launch: " + (prefs.autoListen() ? "ON" : "OFF"),
                "⭐ picker fallback: " + (prefs.mainLauncherOpensOverlay() ? "ON" : "OFF")
        };
        showCopperMenu("OVERLAY  ✦  APPEARANCE",
                "Changes save to this TV. Close and reopen the overlay to see layout changes.",
                options, index -> {
                    switch (index) {
                        case 0: chooseOverlayPosition(prefs); break;
                        case 1: chooseOverlaySize(prefs); break;
                        case 2: chooseOverlayHide(prefs); break;
                        case 3: chooseOverlaySkin(prefs); break;
                        case 4:
                            prefs.setAutoListen(!prefs.autoListen());
                            status("Auto-listen: " + (prefs.autoListen() ? "ON" : "OFF"));
                            showOverlayPreferences();
                            break;
                        case 5:
                            showCopperMenu("⭐ REMOTE SHORTCUT",
                                    "Enable only when onn lists Kellie's Voice but not the separate Overlay shortcut. The overlay SETTINGS button always opens this screen.",
                                    new String[] {
                                            prefs.mainLauncherOpensOverlay()
                                                    ? "Turn fallback OFF" : "Turn fallback ON",
                                            "Keep current setting"
                                    }, which -> {
                                        if (which == 0) {
                                            prefs.setMainLauncherOpensOverlay(
                                                    !prefs.mainLauncherOpensOverlay());
                                            status("⭐ fallback: " +
                                                    (prefs.mainLauncherOpensOverlay() ? "ON" : "OFF"));
                                        }
                                        showOverlayPreferences();
                                    });
                            break;
                        default: break;
                    }
                });
    }

    private void chooseOverlayPosition(OverlayPreferences prefs) {
        final String[] positions = {
                OverlayPreferences.POSITION_DEFAULT,
                OverlayPreferences.POSITION_TOP_LEFT,
                OverlayPreferences.POSITION_TOP_RIGHT,
                OverlayPreferences.POSITION_BOTTOM_LEFT,
                OverlayPreferences.POSITION_BOTTOM_RIGHT,
                OverlayPreferences.POSITION_CENTER,
                OverlayPreferences.POSITION_BOTTOM_CENTER
        };
        String[] labels = new String[positions.length];
        for (int i = 0; i < positions.length; i++) {
            labels[i] = (positions[i].equals(prefs.position()) ? "✓  " : "     ")
                    + OverlayPreferences.positionLabel(positions[i]);
        }
        showCopperMenu("OVERLAY  ✦  POSITION",
                "Choose where the floating voice card appears.", labels, which -> {
                    prefs.setPosition(positions[which]);
                    status("Overlay position saved. Reopen the overlay to apply.");
                    showOverlayPreferences();
                });
    }

    private void chooseOverlaySize(OverlayPreferences prefs) {
        final String[] values = {
                OverlayPreferences.SIZE_SMALL,
                OverlayPreferences.SIZE_MEDIUM,
                OverlayPreferences.SIZE_LARGE
        };
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            options[i] = (values[i].equals(prefs.size()) ? "✓  " : "     ")
                    + OverlayPreferences.sizeLabel(values[i]);
        }
        showCopperMenu("OVERLAY  ✦  SIZE",
                "Small saves screen space. Large makes text easier to read.", options, which -> {
                    prefs.setSize(values[which]);
                    status("Overlay size saved. Reopen the overlay to apply.");
                    showOverlayPreferences();
                });
    }

    private void chooseOverlayHide(OverlayPreferences prefs) {
        final int[] values = {5, 7, 10, 20, 0};
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            String label = values[i] == 0 ? "Never (manual close)"
                    : values[i] + " seconds";
            if (values[i] == 7) label += " (original)";
            options[i] = ((prefs.autoHide() && values[i] == prefs.hideSeconds())
                    || (!prefs.autoHide() && values[i] == 0) ? "✓  " : "     ") + label;
        }
        showCopperMenu("OVERLAY  ✦  AUTO-HIDE",
                "Starts counting after Kellie's spoken reply finishes.", options, which -> {
                    prefs.setHideSeconds(values[which]);
                    status("Auto-hide saved: " + prefs.hideLabel());
                    showOverlayPreferences();
                });
    }

    private void chooseOverlaySkin(OverlayPreferences prefs) {
        showCopperMenu("OVERLAY  ✦  STYLE",
                "Both styles use Kellie's charcoal and copper branding.",
                new String[] {
                        "Copper Glass (rounded, animated)",
                        "Compact Charcoal (smaller text)"
                }, which -> {
                    prefs.setSkin(which == 0 ? OverlayPreferences.SKIN_GLASS
                            : OverlayPreferences.SKIN_CLASSIC);
                    status("Appearance saved. Reopen the overlay to apply.");
                    showOverlayPreferences();
                });
    }

    private void launchOverlay() {
        // Never activate the overlay without explicit user action and consent.
        if (settings == null) {
            status("Configure Home Assistant before opening the overlay.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, OVERLAY_MIC_PERMISSION_REQUEST);
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            status("Grant 'Display over other apps', then start the overlay again.");
            new AlertDialog.Builder(this)
                    .setTitle("Allow Kellie's Voice overlay")
                    .setMessage("Google TV must allow this app to display over other apps. " +
                            "The microphone listens after launch when Auto-listen is enabled. " +
                            "The overlay stops completely when you choose CLOSE or BACK. " +
                            "If the TV has no overlay permission screen, this firmware may not support it.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open system permission", (dialog, which) -> {
                        try {
                            startActivityForResult(new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName())), OVERLAY_SETTINGS_REQUEST);
                        } catch (Exception error) {
                            status("No overlay permission screen found on this Google TV firmware.");
                        }
                    }).show();
            return;
        }
        try {
            Intent start = new Intent(this, VoiceOverlayService.class);
            startForegroundService(start);
            status("Starting voice overlay... CLOSE or BACK dismisses it.");
            // Reveal the prior task rather than forcing the TV launcher.
            // OEM behavior may vary; test with a harmless launcher screen first.
            new Handler(Looper.getMainLooper()).postDelayed(() -> moveTaskToBack(true), 750);
        } catch (Exception error) {
            status("Couldn't start overlay on this Google TV firmware.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_SETTINGS_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                status("Overlay allowed! Open SETTINGS and choose floating overlay again.");
            } else {
                status("Overlay permission wasn't granted. The Kellie's Voice app still works.");
            }
        }
    }

    private void configure() {
        LinearLayout form = new LinearLayout(this);
        form.setPadding(30, 10, 30, 10);
        form.setOrientation(LinearLayout.VERTICAL);

        form.addView(label("Home Assistant URL (prefer HTTPS / Nabu Casa)", 16, 0xff221713));
        EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setHint("https://your-ha-address");
        url.setText(settings == null ? "" : settings.url);
        form.addView(url);

        form.addView(label("Long-lived access token (never saved in GitHub)", 16, 0xff221713));
        EditText token = new EditText(this);
        token.setSingleLine(true);
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setHint(settings == null ? "Paste token" : "Leave blank to keep existing token");
        form.addView(token);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connect to Home Assistant");
        builder.setView(form);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Save and connect", (dialog, which) -> saveAndConnect(
                url.getText().toString().trim(), token.getText().toString().trim()));
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> url.requestFocus());
        dialog.show();
    }

    private void saveAndConnect(String url, String token) {
        if (settings != null && token.isEmpty()) token = settings.token;
        if (token.isEmpty()) { status("Enter a Home Assistant long-lived access token."); return; }
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null || (!"https".equals(parsed.scheme()) && !"http".equals(parsed.scheme()))) {
            status("Enter the full http:// or https:// Home Assistant address."); return;
        }
        final String acceptedToken = token;
        if ("http".equals(parsed.scheme())) {
            new AlertDialog.Builder(this)
                    .setTitle("HTTP connection is not encrypted")
                    .setMessage("Your Home Assistant token and voice audio would travel without transport encryption. " +
                            "Use HTTPS when possible. Continue with HTTP only on a trusted private network.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Use HTTP locally", (d, w) -> persistAndConnect(url, acceptedToken))
                    .show();
            return;
        }
        persistAndConnect(url, acceptedToken);
    }

    private void persistAndConnect(String url, String token) {
        try {
            store.save(url, token);
            settings = store.load();
            updateConnectionLabel();
            connect();
        } catch (Exception e) {
            status("Couldn't safely save connection. Try again.");
        }
    }

    private void connect() {
        if (settings == null) { configure(); return; }
        initialConnectionRequested = true;
        assist.connect(settings.url, settings.token);
        updateConnectionLabel();
    }

    private void startTalk() {
        if (assist != null && !assist.isConnected()) {
            assist.ensureConnected();
            status("Reconnecting to Home Assistant. Try TALK when connected.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
        } else {
            recognized.setText("Listening...");
            response.setText("Waiting for Home Assistant...");
            assist.talk();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) startTalk();
            else status("Microphone permission denied.");
        } else if (requestCode == OVERLAY_MIC_PERMISSION_REQUEST) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) launchOverlay();
            else status("Microphone permission denied; overlay not started.");
        }
    }

    private void newConversation() {
        assist.disconnect();
        recognized.setText("Nothing yet");
        response.setText("Ready for a fresh conversation.");
        if (settings != null) {
            connect();
        } else {
            status("Conversation cleared. Configure Home Assistant to continue.");
        }
    }

    private void confirmForget() {
        new AlertDialog.Builder(this)
                .setTitle("Forget Home Assistant login?")
                .setMessage("Deletes the saved URL and encrypted token from this TV.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Forget", (d, w) -> {
                    assist.disconnect();
                    if (player != null) { player.release(); player = null; }
                    store.clear();
                    settings = null;
                    updateConnectionLabel();
                    status("Saved connection deleted.");
                }).show();
    }

    @Override public void status(String message) {
        runOnUiThread(() -> {
            if (status != null) status.setText(message);
            updateConnectionLabel();
        });
    }

    @Override public void transcript(String value) {
        runOnUiThread(() -> { if (recognized != null) recognized.setText(value); });
    }

    @Override public void answer(String value) {
        runOnUiThread(() -> { if (response != null) response.setText(value); });
    }

    @Override public void ttsUrl(String url) { runOnUiThread(() -> playTts(url)); }

    private void playTts(String address) {
        if (settings == null) return;
        try {
            HttpUrl playback = TtsUrls.allowedPlaybackUrl(settings.url, address);
            if (player != null) { player.release(); player = null; }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + settings.token);
            player.setDataSource(this, Uri.parse(playback.toString()), headers);
            player.setOnPreparedListener(p -> { status("Speaking..."); p.start(); });
            player.setOnCompletionListener(p -> status("Finished. Select TALK for another question."));
            player.setOnErrorListener((p, what, extra) -> {
                status("TTS playback failed. The text answer is still above.");
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            status("Could not play TTS. The text answer is still above.");
        }
    }

    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        if (assist != null) assist.shutdown();
        super.onDestroy();
    }
}
