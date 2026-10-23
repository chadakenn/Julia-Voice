package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test public void followUpPreviewIsAMicrophoneEvenWhenAssistIsNotListening() {
        MicOwnership owner = new MicOwnership();
        assertFalse(owner.microphoneActive());
        assertEquals(MicOwnership.PREVIEW, owner.armPreview());
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE,
                MicrophonePrivacy.serviceType(35, owner.microphoneActive()));
        String text = MicrophonePrivacy.notificationText(true,
                MicrophonePrivacy.TYPE_MICROPHONE, owner.followUpCapture());
        assertEquals(MicrophonePrivacy.MIC_FOLLOW, text);
        assertFalse(text.toLowerCase().contains("off"));
    }

    @Test public void handoffDoesNotPassThroughTheIdleForegroundType() {
        MicOwnership owner = new MicOwnership();
        owner.armPreview();
        int during = owner.beginHandoff();
        assertEquals(MicOwnership.HANDOFF, during);
        assertTrue(owner.microphoneActive());
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE,
                MicrophonePrivacy.serviceType(35, true));
        assertEquals(MicOwnership.ASSIST, owner.assistOpened());
        assertEquals(MicrophonePrivacy.TYPE_MICROPHONE,
                MicrophonePrivacy.serviceType(35, owner.microphoneActive()));
        assertEquals(MicOwnership.IDLE, owner.assistReleased());
        assertEquals(MicrophonePrivacy.TYPE_SPECIAL_USE,
                MicrophonePrivacy.serviceType(35, owner.microphoneActive()));
    }

    @Test public void everyStopReleasesOnlyAfterTheRecorderIsGone() {
        assertEquals(MicOwnership.IDLE, released(MicOwnership.PREVIEW).previewReleased());
        MicOwnership handoff = new MicOwnership();
        handoff.armPreview();
        handoff.beginHandoff();
        assertEquals(MicOwnership.HANDOFF, handoff.previewReleased());
        for (int start : new int[] {MicOwnership.PREVIEW, MicOwnership.HANDOFF, MicOwnership.ASSIST}) {
            MicOwnership stop = phase(start);
            assertEquals(MicOwnership.IDLE, stop.releaseAll());
            assertFalse(stop.microphoneActive());
        }
        MicOwnership failed = new MicOwnership();
        failed.armPreview();
        assertEquals(MicOwnership.IDLE, failed.previewDidNotOpen());
        MicOwnership assist = new MicOwnership();
        assist.assistOpened();
        assist.armPreview();
        assertEquals(MicOwnership.ASSIST, assist.phase());
        assertEquals(MicOwnership.IDLE, assist.assistReleased());
    }

    private static MicOwnership released(int phase) {
        return phase(phase);
    }

    private static MicOwnership phase(int phase) {
        MicOwnership owner = new MicOwnership();
        if (phase == MicOwnership.PREVIEW) owner.armPreview();
        else if (phase == MicOwnership.HANDOFF) {
            owner.armPreview();
            owner.beginHandoff();
        } else if (phase == MicOwnership.ASSIST) owner.assistOpened();
        return owner;
    }
}
