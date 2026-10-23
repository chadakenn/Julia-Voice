package com.example.tvvoiceprobe;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TvPopupActionGateTest {
    @Test public void remoteOkDispatchesOnlyOncePerVisibleCard() {
        TvPopupActionGate card = new TvPopupActionGate();
        assertTrue(card.claim());
        assertFalse(card.claim());
        assertFalse(card.claim());
    }

    @Test public void nextAnnouncementHasIndependentActionGate() {
        TvPopupActionGate first = new TvPopupActionGate();
        TvPopupActionGate next = new TvPopupActionGate();
        assertTrue(first.claim());
        assertFalse(first.claim());
        assertTrue(next.claim());
        assertFalse(next.claim());
    }
}
