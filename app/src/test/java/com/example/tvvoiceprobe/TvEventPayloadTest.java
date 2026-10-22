package com.example.tvvoiceprobe;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvEventPayloadTest {
    @Test public void targetOnlyMatchesThisTvOrBroadcast() {
        assertTrue(TvEventPayload.addressedTo(null, "tv-123"));
        assertTrue(TvEventPayload.addressedTo("", "tv-123"));
        assertTrue(TvEventPayload.addressedTo("tv-123", "tv-123"));
        assertFalse(TvEventPayload.addressedTo("tv-456", "tv-123"));
    }

    @Test public void onlyHomeAssistantCameraEntitiesAreAllowed() {
        assertEquals("camera.driveway", TvEventPayload.cameraEntity("camera.driveway"));
        assertNull(TvEventPayload.cameraEntity("http://other-host/photo.jpg"));
        assertNull(TvEventPayload.cameraEntity("camera../credentials"));
        assertNull(TvEventPayload.cameraEntity("sensor.temperature"));
    }

    @Test public void liveStreamIsExplicitAndUrlIsSameOrigin() {
        assertEquals("kellies_voice_live_camera", TvEventPayload.LIVE_CAMERA);
        assertEquals(90, TvEventPayload.liveDuration(0));
        assertEquals(15, TvEventPayload.liveDuration(1));
        assertEquals(180, TvEventPayload.liveDuration(999));
        assertTrue(TvEventPayload.hlsPathAllowed("/api/hls/token/master_playlist.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("https://evil.example/playlist.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("//evil.example/api/hls/video.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("/api/hls/../etc/video.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("/api/hls/token/master_playlist.m3u8?leak=1"));
    }

    @Test public void durationAndTextAreBounded() {
        assertEquals(10, TvEventPayload.duration(0));
        assertEquals(4, TvEventPayload.duration(1));
        assertEquals(45, TvEventPayload.duration(999));
        assertEquals("abc", TvEventPayload.cleanText("abc", 10));
        assertEquals("abc", TvEventPayload.cleanText("abcdefg", 3));
    }
}
