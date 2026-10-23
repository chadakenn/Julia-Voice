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
        assertEquals("kellies_voice_hide_camera", TvEventPayload.HIDE_CAMERA);
        assertEquals(90, TvEventPayload.liveDuration(0));
        assertEquals(15, TvEventPayload.liveDuration(1));
        assertEquals(180, TvEventPayload.liveDuration(999));
        assertTrue(TvEventPayload.hlsPathAllowed("/api/hls/token/master_playlist.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("https://evil.example/playlist.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("//evil.example/api/hls/video.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("/api/hls/../etc/video.m3u8"));
        assertFalse(TvEventPayload.hlsPathAllowed("/api/hls/token/master_playlist.m3u8?leak=1"));
    }

    @Test public void buttonsAreLimitedAndSpeechRejectsAddresses() {
        assertEquals("Dismiss", TvEventPayload.action("dismiss", "Dismiss").label);
        assertEquals("unlock", TvEventPayload.action("Unlock", "Unlock").id);
        assertNull(TvEventPayload.action("open gate", "Open"));
        assertNull(TvEventPayload.action("talk", "   "));
        java.util.List<TvEventPayload.Action> actions = TvEventPayload.takeActions(new TvEventPayload.Action[] {
                TvEventPayload.action("talk", "Talk"),
                TvEventPayload.action("talk", "Again"),
                TvEventPayload.action("unlock", "Unlock"),
                TvEventPayload.action("open_gate", "Open gate"),
                TvEventPayload.action("dismiss", "Dismiss"),
        });
        assertEquals(3, actions.size());
        assertEquals("talk", actions.get(0).id);
        assertEquals("unlock", actions.get(1).id);
        assertEquals("open_gate", actions.get(2).id);
        assertEquals("", TvEventPayload.speak("https://example.com/clip.mp3"));
        assertEquals("", TvEventPayload.speak("x".repeat(181)));
        assertEquals("Someone is at the front door.", TvEventPayload.speak("  Someone is at the front door.  "));
        assertEquals("aabbccddeeff00112233445566778899",
                TvEventPayload.announcementId("AABBCCDDEEFF00112233445566778899"));
        assertEquals("", TvEventPayload.announcementId("6f9619ff-8b86-d011-b42d-00c04fc964ff"));
        assertEquals("kellies_voice_announcement_action", TvEventPayload.ACTION);
    }

    @Test public void durationAndTextAreBounded() {
        assertEquals(10, TvEventPayload.duration(0));
        assertEquals(4, TvEventPayload.duration(1));
        assertEquals(45, TvEventPayload.duration(999));
        assertEquals("abc", TvEventPayload.cleanText("abc", 10));
        assertEquals("abc", TvEventPayload.cleanText("abcdefg", 3));
    }
}
