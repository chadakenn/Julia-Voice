package com.example.tvvoiceprobe;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Separate TV launcher entry for optional mapping to an onn ⭐ shortcut.
 * A standard launcher action cannot take over the OEM/Google remote mic key.
 */
public final class OverlayShortcutActivity extends Activity {
    private boolean started;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView message = new TextView(this);
        message.setText("Opening Home Assistant floating voice...");
        message.setTextColor(0xffffffff);
        message.setTextSize(22);
        message.setPadding(45, 60, 45, 60);
        message.setBackgroundColor(0xff101c2b);
        setContentView(message);
    }

    @Override protected void onResume() {
        super.onResume();
        if (started) return;
        started = true;
        // Use a visible Activity to start the microphone-typed foreground
        // service. Android 14+ restricts starting it from the background.
        handler.postDelayed(() -> {
            if (isFinishing()) return;
            boolean configured = false;
            try { configured = new CredentialStore(this).load() != null; }
            catch (Exception ignored) { }
            if (!configured || !Settings.canDrawOverlays(this) ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "First open HA TV Voice, then SETTINGS > Floating voice overlay",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, VoiceActivity.class));
                finish();
                return;
            }
            try {
                startForegroundService(new Intent(this, VoiceOverlayService.class));
                // The service must promote itself while this Activity is still
                // in the foreground before the former TV app is revealed.
                handler.postDelayed(this::finish, 1000);
            } catch (Exception error) {
                Toast.makeText(this, "Overlay not supported. Open the regular TV Voice app.",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, VoiceActivity.class));
                finish();
            }
        }, 150);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
