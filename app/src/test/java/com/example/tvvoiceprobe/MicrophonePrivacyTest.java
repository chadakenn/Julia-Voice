package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MicrophonePrivacyTest {
    @Test public void android14WaitsWithoutTheMicrophoneType() {
        assertEquals(MicrophonePrivacy.TYPE_SPECIAL_USE, MicrophonePrivacy.serviceType(34, false));
        assertEquals(MicrophonePrivacy.TYPE_SPECIAL_USE, MicrophonePrivacy.serviceType(35, false));
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE, MicrophonePrivacy.serviceType(34, true));
    }

    @Test public void olderAndroidKeepsTheMicrophoneTypeWhileTheOverlayIsOpen() {
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE, MicrophonePrivacy.serviceType(33, false));
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE, MicrophonePrivacy.serviceType(29, true));
        assertEquals(MicrophonePrivacy.TYPE_NONE, MicrophonePrivacy.serviceType(28, true));
    }

    @Test public void rejectedSpecialUseDoesNotClaimTheMicrophoneIsOff() {
        int actual = MicrophonePrivacy.afterRejection(MicrophonePrivacy.TYPE_SPECIAL_USE, true);
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE, actual);
        String text = MicrophonePrivacy.notificationText(false, actual);
        assertEquals(MicrophonePrivacy.MIC_STUCK, text);
        assertFalse(text.contains("off"));
        assertEquals(MicrophonePrivacy.MIC_OFF,
                MicrophonePrivacy.notificationText(false, MicrophonePrivacy.TYPE_SPECIAL_USE));
        assertEquals(MicrophonePrivacy.MIC_ON,
                MicrophonePrivacy.notificationText(true, MicrophonePrivacy.TYPE_MICROPHONE));
    }
}
