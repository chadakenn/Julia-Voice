package com.example.tvvoiceprobe;

import okhttp3.HttpUrl;

/**
 * Which TTS URL MediaPlayer is allowed to open.
 * Kept free of Android APIs so the allowlist can be unit tested.
 */
public final class TtsUrls {
    private TtsUrls() { }

    public static HttpUrl allowedPlaybackUrl(String baseUrl, String address) {
        HttpUrl base = HttpUrl.parse(baseUrl);
        if (base == null) throw new IllegalArgumentException("Invalid Home Assistant address");
        HttpUrl playback = base.resolve(address);
        if (playback == null) throw new IllegalArgumentException("Invalid TTS audio URL");
        if (!playback.host().equals(base.host()) || playback.port() != base.port()
                || !playback.scheme().equals(base.scheme())) {
            // HA sometimes emits its internal URL. Rewrite only known TTS proxy paths.
            if (!playback.encodedPath().startsWith("/api/tts_proxy/")) {
                throw new SecurityException("Refusing unknown audio server");
            }
            playback = base.newBuilder()
                    .encodedPath(playback.encodedPath())
                    .encodedQuery(playback.encodedQuery())
                    .build();
        }
        return playback;
    }
}
