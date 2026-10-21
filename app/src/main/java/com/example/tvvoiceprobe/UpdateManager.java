package com.example.tvvoiceprobe;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User-approved, non-root in-app GitHub Releases updater.
 * Never downloads an APK from an arbitrary URL or bypasses Android's installer.
 * Validates the release checksum and then lets Android enforce package signature.
 */
public final class UpdateManager {
    // The owner plans to rename Julia-Voice to kellies-voice after TVs are upgraded.
    // Keep both exact owner-controlled HTTPS release paths for transition.
    private static final String LATEST =
            "https://api.github.com/repos/chadakenn/kellies-voice/releases/latest";
    private static final String LEGACY_LATEST =
            "https://api.github.com/repos/chadakenn/Julia-Voice/releases/latest";
    private static final String RELEASE_PREFIX =
            "https://github.com/chadakenn/kellies-voice/releases/download/";
    private static final String LEGACY_RELEASE_PREFIX =
            "https://github.com/chadakenn/Julia-Voice/releases/download/";
    private static final long MAX_APK_BYTES = 150L * 1024L * 1024L;
    private final Activity activity;
    private final Listener listener;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private volatile Release available;
    private volatile boolean busy;

    public interface Listener {
        void onUpToDate(String message);
        void onAvailable(String version, String notes);
        void onProgress(String message);
        void onDownloaded();
        void onError(String message);
    }

    private static final class Release {
        final int code;
        final String name;
        final String notes;
        final String sha256;
        final String apkUrl;
        final long size;
        Release(int code, String name, String notes, String sha256, String apkUrl, long size) {
            this.code = code;
            this.name = name;
            this.notes = notes;
            this.sha256 = sha256;
            this.apkUrl = apkUrl;
            this.size = size;
        }
    }

    public UpdateManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public boolean isBusy() { return busy; }

    public void check() {
        if (busy) return;
        busy = true;
        background.execute(() -> {
            try {
                JSONObject release = new JSONObject(readLatestReleaseMetadata());
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                    throw new IllegalStateException("No stable release is available.");
                }
                String tag = release.getString("tag_name");
                if (!tag.matches("v[0-9]+\\.[0-9]+(\\.[0-9]+)?")) {
                    throw new IllegalStateException("Unexpected release tag.");
                }
                JSONArray assets = release.getJSONArray("assets");
                String apk = null, metadata = null;
                long size = -1;
                for (int index = 0; index < assets.length(); index++) {
                    JSONObject item = assets.getJSONObject(index);
                    String filename = item.optString("name");
                    if ("app-release.apk".equals(filename)) {
                        apk = item.optString("browser_download_url");
                        size = item.optLong("size", -1);
                    } else if ("update.json".equals(filename)) {
                        metadata = item.optString("browser_download_url");
                    }
                }
                if (apk == null || metadata == null) {
                    throw new IllegalStateException("Release needs app-release.apk and update.json.");
                }
                validateAssetUrl(apk, tag, "app-release.apk");
                validateAssetUrl(metadata, tag, "update.json");
                if (size < 4096 || size > MAX_APK_BYTES) {
                    throw new IllegalStateException("Invalid APK file size in release.");
                }
                JSONObject info = new JSONObject(readText(metadata, 16384));
                int code = info.getInt("versionCode");
                String name = info.getString("versionName");
                String hash = info.getString("sha256").toLowerCase(Locale.US);
                if (!hash.matches("[a-f0-9]{64}") || !tag.equals(info.optString("tag"))) {
                    throw new IllegalStateException("Invalid release integrity metadata.");
                }
                int installed = installedVersionCode();
                if (code <= installed) {
                    available = null;
                    post(() -> listener.onUpToDate("You're up to date (v" +
                            activityVersion() + ")."));
                    return;
                }
                available = new Release(code, name,
                        release.optString("body", ""), hash, apk, size);
                post(() -> listener.onAvailable(name,
                        available == null ? "" : available.notes));
            } catch (Exception error) {
                String message = error.getMessage();
                if (message != null && message.contains("404")) {
                    message = "No public GitHub Release has been published yet.";
                } else if (message == null) {
                    message = "Update check failed. Try again later.";
                }
                final String finalMessage = message;
                post(() -> listener.onError(finalMessage));
            } finally {
                busy = false;
            }
        });
    }

    public void download() {
        Release release = available;
        if (release == null || busy) {
            post(() -> listener.onError("Check for updates first."));
            return;
        }
        busy = true;
        background.execute(() -> {
            File temp = new File(updateFolder(), "update.partial");
            File apk = downloadedApk();
            try {
                if (!updateFolder().exists() && !updateFolder().mkdirs()) {
                    throw new IllegalStateException("Cannot create APK cache directory.");
                }
                if (temp.exists()) temp.delete();
                if (apk.exists()) apk.delete();
                if (updateFolder().getUsableSpace() < release.size * 2) {
                    throw new IllegalStateException("Not enough free storage on Google TV.");
                }
                HttpURLConnection conn = openHttps(release.apkUrl);
                if (conn.getResponseCode() != 200) {
                    int code = conn.getResponseCode();
                    conn.disconnect();
                    throw new IllegalStateException("APK download failed: HTTP " + code);
                }
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                long count = 0;
                int reported = -1;
                try (InputStream input = conn.getInputStream();
                     FileOutputStream output = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[65536];
                    int amount;
                    while ((amount = input.read(buffer)) != -1) {
                        count += amount;
                        if (count > MAX_APK_BYTES || count > release.size) {
                            throw new IllegalStateException("APK exceeded expected release size.");
                        }
                        sha.update(buffer, 0, amount);
                        output.write(buffer, 0, amount);
                        int percent = (int) (100L * count / release.size);
                        if (percent >= reported + 10 || percent == 100) {
                            reported = percent;
                            int displayPercent = percent;
                            post(() -> listener.onProgress("Downloading: " + displayPercent + "%"));
                        }
                    }
                    output.getFD().sync();
                } finally {
                    conn.disconnect();
                }
                if (count != release.size) {
                    throw new IllegalStateException("Downloaded APK size did not match release.");
                }
                StringBuilder digest = new StringBuilder();
                for (byte value : sha.digest()) {
                    digest.append(String.format(Locale.US, "%02x", value & 255));
                }
                if (!release.sha256.equals(digest.toString())) {
                    throw new IllegalStateException("Downloaded APK failed SHA-256 verification.");
                }
                if (!temp.renameTo(apk)) {
                    throw new IllegalStateException("Could not save the verified APK.");
                }
                post(listener::onDownloaded);
            } catch (Exception error) {
                temp.delete();
                String message = error.getMessage() == null ? "Download failed" : error.getMessage();
                post(() -> listener.onError(message));
            } finally {
                busy = false;
            }
        });
    }

    public boolean downloadedExists() {
        File apk = downloadedApk();
        return apk.isFile() && apk.length() > 4096L;
    }

    public void install() {
        if (!downloadedExists()) {
            listener.onError("No downloaded APK found. Check and download again.");
            return;
        }
        try {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                listener.onError("Allow Kellie's Voice to install unknown apps in Android settings, " +
                        "then return and choose Install downloaded update again.");
                Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(settings);
                return;
            }
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", downloadedApk());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Exception error) {
            listener.onError("Can't open Android installer. Check unknown-app permission.");
        }
    }

    private int installedVersionCode() throws Exception {
        PackageInfo info = activity.getPackageManager()
                .getPackageInfo(activity.getPackageName(), 0);
        return (int) info.getLongVersionCode();
    }

    private String activityVersion() {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "installed";
        }
    }

    private File updateFolder() { return new File(activity.getCacheDir(), "updates"); }
    private File downloadedApk() { return new File(updateFolder(), "app-release.apk"); }
    private void post(Runnable runnable) { ui.post(runnable); }

    private void validateAssetUrl(String url, String tag, String filename) {
        // Require the exact known repo path and file name. This also permits
        // transitional releases from the original repo before renaming.
        if (!url.equals(RELEASE_PREFIX + tag + "/" + filename)
                && !url.equals(LEGACY_RELEASE_PREFIX + tag + "/" + filename)) {
            throw new IllegalArgumentException("Unexpected release asset URL.");
        }
    }

    private String readLatestReleaseMetadata() throws Exception {
        try {
            return readText(LATEST, 750000);
        } catch (IllegalStateException error) {
            // Retain an explicit fallback for rollout/migration edge cases.
            // It never broadens the APK host or asset-path allowlist.
            if (error.getMessage() == null || !error.getMessage().contains("404")) {
                throw error;
            }
            return readText(LEGACY_LATEST, 750000);
        }
    }

    private String readText(String url, int maxBytes) throws Exception {
        HttpURLConnection conn = openHttps(url);
        try {
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException("Update server returned HTTP " + conn.getResponseCode());
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = conn.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = input.read(buf)) != -1) {
                    if (output.size() + n > maxBytes) {
                        throw new IllegalStateException("Update metadata too large.");
                    }
                    output.write(buf, 0, n);
                }
            }
            return output.toString("UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection openHttps(String raw) throws Exception {
        URL next = new URL(raw);
        for (int i = 0; i < 6; i++) {
            if (!"https".equalsIgnoreCase(next.getProtocol())) {
                throw new SecurityException("Refusing insecure update URL.");
            }
            String host = next.getHost().toLowerCase(Locale.US);
            if (!(host.equals("api.github.com") || host.equals("github.com")
                    || host.equals("objects.githubusercontent.com")
                    || host.equals("release-assets.githubusercontent.com")
                    || host.equals("github-releases.githubusercontent.com"))) {
                throw new SecurityException("Untrusted APK download host.");
            }
            HttpURLConnection conn = (HttpURLConnection) next.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Kellies-Voice-Updater");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) throw new IllegalStateException("Update redirect missing.");
                next = new URL(next, location);
                continue;
            }
            return conn;
        }
        throw new IllegalStateException("Too many update download redirects.");
    }
}
