package com.example.tvvoiceprobe;

/**
 * Exact GitHub release asset URLs the updater will fetch.
 * No Android APIs, so the allowlist can be unit tested on the host.
 */
public final class ReleaseAssets {
    public static final String RELEASE_PREFIX =
            "https://github.com/chadakenn/Kellies-Voice/releases/download/";

    private ReleaseAssets() { }

    public static void validateAssetUrl(String url, String tag, String filename) {
        if (url == null || tag == null || filename == null
                || !url.equals(RELEASE_PREFIX + tag + "/" + filename)) {
            throw new IllegalArgumentException("Unexpected release asset URL.");
        }
    }

    public static boolean stableTag(String tag) {
        return tag != null && tag.matches("v[0-9]+\\.[0-9]+(\\.[0-9]+)?");
    }
}
