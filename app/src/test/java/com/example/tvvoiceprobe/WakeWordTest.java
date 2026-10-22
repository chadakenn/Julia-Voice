package com.example.tvvoiceprobe;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WakeWordTest {
    @Test public void shippedOkayNabuConfigIsTheOnlyModel() throws Exception {
        Path dir = Path.of("src/main/assets/wakeword");
        WakeWordConfig config = WakeWordConfig.parse(
                new String(Files.readAllBytes(dir.resolve("okay_nabu.json")), StandardCharsets.UTF_8));
        assertEquals("Okay Nabu", config.phrase);
        assertEquals("okay_nabu.tflite", config.model);
        assertEquals(0.85f, config.probabilityCutoff, 0.0001f);
        assertEquals(10, config.featureStepSizeMs);
        assertEquals(5, config.slidingWindowSize);
        byte[] model = Files.readAllBytes(dir.resolve(config.model));
        assertTrue(model.length > 10_000);
        assertEquals('T', (char) model[4]);
        assertEquals('F', (char) model[5]);
        assertEquals('L', (char) model[6]);
        assertEquals('3', (char) model[7]);
        assertFalse(Files.exists(dir.resolve("hey_jarvis.tflite")));
        assertFalse(Files.exists(dir.resolve("hey_mycroft.tflite")));
    }

    @Test public void badNumbersAndFilenamesAreRejected() {
        String good = FilesQuiet.ok();
        assertRejected(good.replace("\"micro\"", "\"open\""));
        assertRejected(good.replace("okay_nabu.tflite", "../secrets.tflite"));
        assertRejected(good.replace("0.85", "NaN"));
        assertRejected(good.replace("0.85", "0"));
        assertRejected(good.replace("\"feature_step_size\": 10", "\"feature_step_size\": 0"));
    }

    @Test public void bootUpdateAndStickyRestartsCannotStartTheMicrophone() {
        assertFalse(WakeWordSession.mayStart(true, "android.intent.action.BOOT_COMPLETED", true, true));
        assertFalse(WakeWordSession.mayStart(true, "android.intent.action.MY_PACKAGE_REPLACED", true, true));
        assertFalse(WakeWordSession.mayStart(true, null, true, true));
        assertFalse(WakeWordSession.mayStart(true, WakeWordSession.ACTION_STOP, true, true));
        assertFalse(WakeWordSession.mayStart(false, WakeWordSession.ACTION_START, true, true));
        assertFalse(WakeWordSession.mayStart(true, WakeWordSession.ACTION_START, false, true));
        assertFalse(WakeWordSession.mayStart(true, WakeWordSession.ACTION_START, true, false));
        assertTrue(WakeWordSession.mayStart(true, WakeWordSession.ACTION_START, true, true));
    }

    @Test public void overlayOwnsTheMicrophoneUntilItCloses() {
        assertFalse(WakeWordSession.shouldCapture(true, true));
        assertFalse(WakeWordSession.shouldCapture(false, false));
        assertTrue(WakeWordSession.shouldCapture(true, false));
    }

    @Test public void oneDetectionDoesNotImmediatelyFireAgain() {
        WakeWordSession session = new WakeWordSession();
        assertTrue(session.onChunk(true));
        for (int i = 0; i < WakeWordSession.COOLDOWN_CHUNKS; i++) {
            assertFalse(session.onChunk(true));
        }
        assertTrue(session.onChunk(true));
        assertFalse(session.onChunk(false));
    }

    private static void assertRejected(String json) {
        try {
            WakeWordConfig.parse(json);
            fail("expected rejection");
        } catch (IllegalArgumentException expected) {
            assertFalse(expected.getMessage().isEmpty());
        }
    }

    private static final class FilesQuiet {
        static String ok() {
            try {
                return new String(Files.readAllBytes(
                        Path.of("src/main/assets/wakeword/okay_nabu.json")),
                        StandardCharsets.UTF_8);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
    }
}
