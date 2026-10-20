package com.example.tvvoiceprobe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.KeyEvent;
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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new CredentialStore(this);
        assist = new HaAssistClient(this, this);
        updater = new UpdateManager(this, new UpdateManager.Listener() {
            @Override public void onUpToDate(String message) {
                if (manualUpdateCheck) status(message);
                manualUpdateCheck = false;
            }
            @Override public void onAvailable(String version, String notes) {
                manualUpdateCheck = false;
                if (isFinishing() || isDestroyed()) return;
                new AlertDialog.Builder(VoiceActivity.this)
                        .setTitle("Julia Voice update available: " + version)
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
        root.setBackgroundColor(0xff0b1732);

        TextView title = label("JULIA VOICE   ✦   HOME ASSISTANT", 24, 0xff9ed8ff);
        root.addView(title);

        connection = label("Home Assistant: not configured", 15, 0xffc7d8e9);
        root.addView(connection);

        status = label("Connect, then press TALK.", 17, 0xff5de1ec);
        root.addView(status);

        // Prioritize the conversation on small TV screens. The large seven-button
        // diagnostic layout hid all but the first line of the recognized speech.
        ScrollView scroller = new ScrollView(this);
        scroller.setFocusable(false);
        scroller.setFillViewport(true);
        LinearLayout history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        history.setPadding(10, 6, 10, 6);

        history.addView(label("HOME ASSISTANT SAYS", 16, 0xff8ba6c1));
        response = label("Your answer will appear here.", 25, 0xffd2e9fd);
        history.addView(response);

        history.addView(label("YOU SAID", 16, 0xff8ba6c1));
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
        JuliaSplashView splash = new JuliaSplashView(this);
        page.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        setContentView(page);
        try { settings = store.load(); }
        catch (Exception e) { status("Saved connection couldn't be decrypted. Configure it again."); }
        updateConnectionLabel();
        if (!buttons.isEmpty()) buttons.get(0).post(() -> focus(0));
        // Some onn firmware offers only the package's primary launcher in the
        // ⭐ app picker. An explicit opt-in makes that launch the overlay too.
        Intent launch = getIntent();
        boolean openOverlayOnLaunch = new OverlayPreferences(this).mainLauncherOpensOverlay()
                && launch != null && Intent.ACTION_MAIN.equals(launch.getAction())
                && (launch.hasCategory(Intent.CATEGORY_LAUNCHER)
                    || launch.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER));
        if (openOverlayOnLaunch) {
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
                checks.edit().putLong("last_checked", now).apply();
                root.post(() -> updater.check());
            }
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
        b.setBackgroundTintList(ColorStateList.valueOf(0xff405572));
        b.setOnClickListener(action);
        b.setOnFocusChangeListener((v, focused) -> {
            if (focused) selected = index;
            b.setBackgroundTintList(ColorStateList.valueOf(focused ? 0xff097eaf : 0xff405572));
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

    private void showSettingsMenu() {
        String[] entries = new String[] {
                "Configure Home Assistant",
                "Connect / reconnect",
                "Start new conversation",
                "Hardware microphone diagnostics",
                "Open floating voice overlay",
                "Overlay style and voice settings",
                "Check for app updates",
                "Install downloaded update",
                "Forget saved connection"
        };
        new AlertDialog.Builder(this)
                .setTitle("Julia Voice settings")
                .setItems(entries, (dialog, which) -> {
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
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void promptInstallUpdate() {
        if (isFinishing() || isDestroyed()) return;
        if (!updater.downloadedExists()) {
            status("No downloaded update. First choose Check for app updates.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Install verified TV Voice update?")
                .setMessage("Android will ask you to approve the package update. " +
                        "Your Home Assistant login and preferences should be retained " +
                        "because the release uses our permanent signing key.")
                .setNegativeButton("Later", null)
                .setPositiveButton("Open Android installer", (dialog, which) -> updater.install())
                .show();
    }

    private void showOverlayPreferences() {
        final OverlayPreferences preferences = new OverlayPreferences(this);
        final String[] options = new String[] {
                "Skin: " + ("glass".equals(preferences.skin()) ? "Glass" : "Classic") +
                        " (choose)",
                "Auto-listen on launch: " + (preferences.autoListen() ? "ON" : "OFF"),
                "Auto-hide after spoken reply: " + (preferences.autoHide() ? "ON" : "OFF"),
                "⭐ picker fallback (main app opens overlay): " +
                        (preferences.mainLauncherOpensOverlay() ? "ON" : "OFF")
        };
        new AlertDialog.Builder(this)
                .setTitle("TV Voice appearance & launch")
                .setItems(options, (dialog, index) -> {
                    if (index == 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Choose a voice overlay skin")
                                .setSingleChoiceItems(new String[] {
                                        "Glass • dark purple with animated neon waveform",
                                        "Classic • compact blue panel"
                                }, OverlayPreferences.SKIN_GLASS.equals(preferences.skin()) ? 0 : 1,
                                    (chooser, which) -> {
                                        preferences.setSkin(which == 0
                                                ? OverlayPreferences.SKIN_GLASS
                                                : OverlayPreferences.SKIN_CLASSIC);
                                        chooser.dismiss();
                                        status("Skin saved. Reopen the overlay to see it.");
                                    })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else if (index == 1) {
                        preferences.setAutoListen(!preferences.autoListen());
                        status("Auto-listen on overlay launch: " +
                                (preferences.autoListen() ? "ON" : "OFF"));
                        showOverlayPreferences();
                    } else if (index == 2) {
                        preferences.setAutoHide(!preferences.autoHide());
                        status("Close after spoken reply: " +
                                (preferences.autoHide() ? "ON" : "OFF"));
                        showOverlayPreferences();
                    } else if (index == 3) {
                        new AlertDialog.Builder(this)
                                .setTitle("⭐ Remote shortcut compatibility")
                                .setMessage("Enable this ONLY if the onn shortcut list shows " +
                                        "HA TV Voice Alpha but not HA Voice Overlay. " +
                                        "Launching the main app will then open the overlay. " +
                                        "Use the overlay's SETTINGS button to return here.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton(preferences.mainLauncherOpensOverlay()
                                        ? "Turn OFF" : "Turn ON", (d, which) -> {
                                    preferences.setMainLauncherOpensOverlay(
                                            !preferences.mainLauncherOpensOverlay());
                                    status("⭐ compatibility is now " +
                                            (preferences.mainLauncherOpensOverlay() ? "ON" : "OFF"));
                                })
                                .show();
                    }
                })
                .setNegativeButton("Done", null)
                .show();
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
                    .setTitle("Allow TV Voice overlay")
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
                status("Overlay permission wasn't granted. The normal TV voice app still works.");
            }
        }
    }

    private void configure() {
        LinearLayout form = new LinearLayout(this);
        form.setPadding(30, 10, 30, 10);
        form.setOrientation(LinearLayout.VERTICAL);

        form.addView(label("Home Assistant URL (prefer HTTPS / Nabu Casa)", 16, 0xff101c2b));
        EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setHint("https://your-ha-address");
        url.setText(settings == null ? "" : settings.url);
        form.addView(url);

        form.addView(label("Long-lived access token (never saved in GitHub)", 16, 0xff101c2b));
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
        assist.connect(settings.url, settings.token);
        updateConnectionLabel();
    }

    private void startTalk() {
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
            HttpUrl base = HttpUrl.parse(settings.url);
            HttpUrl playback = base.resolve(address);
            if (playback == null) throw new IllegalArgumentException("Invalid TTS audio URL");
            // Only load from our configured HA endpoint, never send HA credentials elsewhere.
            if (!playback.host().equals(base.host()) || playback.port() != base.port()
                    || !playback.scheme().equals(base.scheme())) {
                // HA sometimes emits its internal URL. Rewrite only known HA TTS proxy paths.
                if (!playback.encodedPath().startsWith("/api/tts_proxy/")) {
                    throw new SecurityException("Refusing unknown audio server");
                }
                playback = base.newBuilder()
                        .encodedPath(playback.encodedPath())
                        .encodedQuery(playback.encodedQuery()).build();
            }
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
        assist.shutdown();
        super.onDestroy();
    }
}
