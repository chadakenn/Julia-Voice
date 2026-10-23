package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MediaBehaviorTest {
    @Test public void labelsMatchTheHomeAssistantOptions() {
        assertEquals(MediaBehavior.DUCK, MediaBehavior.keyOrNull("Lower volume"));
        assertEquals(MediaBehavior.PAUSE, MediaBehavior.keyOrNull("Pause playback"));
        assertEquals("Lower volume", MediaBehavior.label("duck"));
        assertEquals("Pause playback", MediaBehavior.label("pause"));
    }

    @Test public void invalidValuesAreIgnoredAndDefaultToLowerVolume() {
        assertNull(MediaBehavior.keyOrNull(null));
        assertNull(MediaBehavior.keyOrNull(""));
        assertNull(MediaBehavior.keyOrNull("Mute"));
        assertNull(MediaBehavior.keyOrNull("on"));
        assertEquals(MediaBehavior.DUCK, MediaBehavior.key("nope"));
        assertEquals(MediaBehavior.PAUSE, MediaBehavior.key("pause"));
        assertEquals("Lower volume", MediaBehavior.label(null));
    }
}
