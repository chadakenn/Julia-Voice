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

    @Test public void durationAndTextAreBounded() {
        assertEquals(10, TvEventPayload.duration(0));
        assertEquals(4, TvEventPayload.duration(1));
        assertEquals(45, TvEventPayload.duration(999));
        assertEquals("abc", TvEventPayload.cleanText("abc", 10));
        assertEquals("abc", TvEventPayload.cleanText("abcdefg", 3));
    }
}
