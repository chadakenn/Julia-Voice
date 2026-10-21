package com.example.tvvoiceprobe;

import org.junit.Test;

import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;

public class TtsUrlsTest {
    @Test public void relativeProxyResolvesAgainstTheConfiguredServer() {
        HttpUrl url = TtsUrls.allowedPlaybackUrl(
                "https://ha.example:8123", "/api/tts_proxy/abc");
        assertEquals("https://ha.example:8123/api/tts_proxy/abc", url.toString());
    }

    @Test public void dockerHostnameIsRewrittenOnlyForTheTtsProxy() {
        HttpUrl url = TtsUrls.allowedPlaybackUrl(
                "https://ha.example",
                "http://172.30.32.1:8123/api/tts_proxy/abc");
        assertEquals("https://ha.example/api/tts_proxy/abc", url.toString());
    }

    @Test public void foreignProxyPathIsForcedBackOntoHomeAssistant() {
        HttpUrl url = TtsUrls.allowedPlaybackUrl(
                "https://ha.example",
                "https://evil.example/api/tts_proxy/abc?x=1");
        assertEquals("https://ha.example/api/tts_proxy/abc?x=1", url.toString());
    }

    @Test(expected = SecurityException.class)
    public void unrelatedServerIsRejected() {
        TtsUrls.allowedPlaybackUrl("https://ha.example", "https://evil.example/song.mp3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingBaseIsRejected() {
        TtsUrls.allowedPlaybackUrl("not a url", "/api/tts_proxy/abc");
    }
}
