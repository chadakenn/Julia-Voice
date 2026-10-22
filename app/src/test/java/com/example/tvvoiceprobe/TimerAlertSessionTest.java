package com.example.tvvoiceprobe;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimerAlertSessionTest {
    private static final class Output implements TimerAlertSession.Output {
        boolean granted = true, audible;
        int requests, abandons, starts, request, playback;
        public boolean requestFocus(int token) { requests++; request = token; return granted; }
        public void abandonFocus() { abandons++; }
        public void stopPlayback() { audible = false; }
        public void startPlayback(int token) { starts++; playback = token; audible = true; }
    }

    @Test public void transientLossKeepsRequestAndGainRestartsWithFreshCallbacks() {
        Output out = new Output(); TimerAlertSession session = new TimerAlertSession(out);
        Object timer = new Object(); session.update(timer, true, 0);
        int old = out.playback;
        session.focusChanged(out.request, TimerAlertSession.Focus.TRANSIENT_LOSS, 10);
        assertFalse(out.audible); assertFalse(session.isPlaying(old)); assertEquals(0, out.abandons);
        session.update(timer, true, 5000);
        assertEquals(1, out.requests); assertFalse(out.audible);
        session.focusChanged(out.request, TimerAlertSession.Focus.GAIN, 5100);
        assertTrue(out.audible); assertEquals(2, out.starts);
        assertTrue(session.isPlaying(out.playback)); assertFalse(session.isPlaying(old));
    }

    @Test public void permanentLossRetriesWithoutGainAndRejectsOldFocusCallback() {
        Output out = new Output(); TimerAlertSession session = new TimerAlertSession(out);
        Object timer = new Object(); session.update(timer, true, 0);
        int oldRequest = out.request;
        session.focusChanged(oldRequest, TimerAlertSession.Focus.LOSS, 10);
        assertFalse(out.audible); assertEquals(1, out.abandons);
        session.update(timer, true, 2009); assertEquals(1, out.requests);
        session.update(timer, true, 2010); assertTrue(out.audible); assertEquals(2, out.requests);
        session.focusChanged(oldRequest, TimerAlertSession.Focus.LOSS, 2020);
        assertTrue(out.audible); assertEquals(1, out.abandons);
    }

    @Test public void deniedRequestRetriesAndLaterTimerIsNotPermanentlyMuted() {
        Output out = new Output(); out.granted = false;
        TimerAlertSession session = new TimerAlertSession(out); Object timer = new Object();
        session.update(timer, true, 0); assertFalse(out.audible);
        session.update(timer, true, 1999); assertEquals(1, out.requests);
        out.granted = true; session.update(timer, true, 2000); assertTrue(out.audible);
        session.focusChanged(out.request, TimerAlertSession.Focus.LOSS, 2001);
        session.update(null, false, 2002);
        session.update(new Object(), true, 2003);
        assertTrue(out.audible); assertEquals(3, out.requests);
    }

    @Test public void missingControlsPreventPlaybackAndLossStopsExistingAudio() {
        Output out = new Output(); TimerAlertSession session = new TimerAlertSession(out);
        Object timer = new Object(); session.update(timer, false, 0);
        assertEquals(0, out.requests); assertEquals(0, out.starts);
        session.update(timer, true, 1); assertTrue(out.audible);
        int oldPlayback = out.playback, oldRequest = out.request;
        session.update(timer, false, 2);
        assertFalse(out.audible); assertFalse(session.isPlaying(oldPlayback)); assertEquals(1, out.abandons);
        session.focusChanged(oldRequest, TimerAlertSession.Focus.GAIN, 3);
        assertFalse(out.audible);
        session.update(timer, true, 4); assertTrue(out.audible); assertEquals(2, out.starts);
        assertFalse(session.isPlaying(oldPlayback));
    }

    @Test public void dismissWhileInterruptedAndCloseCannotBeUndoneByCallbacks() {
        Output out = new Output(); TimerAlertSession session = new TimerAlertSession(out);
        session.update(new Object(), true, 0); int request = out.request;
        session.focusChanged(request, TimerAlertSession.Focus.TRANSIENT_LOSS, 1);
        session.update(null, false, 2);
        session.focusChanged(request, TimerAlertSession.Focus.GAIN, 3);
        assertFalse(out.audible); assertEquals(1, out.starts);
        session.update(new Object(), true, 4); request = out.request;
        int playback = out.playback; session.close();
        session.focusChanged(request, TimerAlertSession.Focus.GAIN, 5);
        session.update(new Object(), true, 6);
        assertFalse(out.audible); assertFalse(session.isPlaying(playback)); assertEquals(2, out.starts);
    }
}
