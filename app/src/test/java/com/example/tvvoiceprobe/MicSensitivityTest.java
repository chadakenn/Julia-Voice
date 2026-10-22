package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MicSensitivityTest {
    @Test public void labelsMatchTheHomeAssistantOptions() {
        assertEquals(MicSensitivity.QUIET, MicSensitivity.keyOrNull("Quiet room"));
        assertEquals(MicSensitivity.NORMAL, MicSensitivity.keyOrNull("Normal"));
        assertEquals(MicSensitivity.TV, MicSensitivity.keyOrNull("TV on"));
        assertEquals("Quiet room", MicSensitivity.label("quiet"));
        assertEquals("Normal", MicSensitivity.label("normal"));
        assertEquals("TV on", MicSensitivity.label("tv"));
    }

    @Test public void invalidAndRepeatedValuesDoNotChangeThePreset() {
        assertNull(MicSensitivity.keyOrNull(null));
        assertNull(MicSensitivity.keyOrNull(""));
        assertNull(MicSensitivity.keyOrNull("Always on"));
        assertNull(MicSensitivity.keyOrNull("on"));
        assertEquals(MicSensitivity.NORMAL, MicSensitivity.key("nope"));
        assertEquals(MicSensitivity.TV, MicSensitivity.key("tv"));
    }
}
