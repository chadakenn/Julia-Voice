package com.example.tvvoiceprobe;

/** Main-thread alert ownership, shared by the Android controller and host tests. */
public final class TimerAlertSession {
    public enum Focus { GAIN, TRANSIENT_LOSS, LOSS }
    public interface Output {
        boolean requestFocus(int requestToken);
        void abandonFocus();
        void stopPlayback();
        void startPlayback(int playbackToken);
    }

    private final Output output;
    private Object selected;
    private boolean controls;
    private boolean requested;
    private boolean playing;
    private boolean waitingForGain;
    private boolean closed;
    private int requestToken;
    private int playbackToken;
    private long retryAt;

    public TimerAlertSession(Output output) { this.output = output; }

    public void update(Object next, boolean controlsAvailable, long now) {
        if (closed) return;
        if (next != selected) {
            reset();
            selected = next;
            retryAt = 0;
        }
        controls = controlsAvailable;
        if (selected == null || !controls) { reset(); return; }
        if (playing || waitingForGain || now < retryAt) return;
        requested = true;
        int token = ++requestToken;
        if (output.requestFocus(token)) {
            play();
        } else {
            requested = false;
            requestToken++;
            output.abandonFocus();
            retryAt = now + 2000;
        }
    }

    public void focusChanged(int token, Focus change, long now) {
        if (closed || !requested || token != requestToken || selected == null || !controls) return;
        if (change == Focus.GAIN) {
            waitingForGain = false;
            if (!playing) play();
        } else if (change == Focus.TRANSIENT_LOSS) {
            stop();
            waitingForGain = true;
            // Keep the request so Android can return focus after the interruption.
        } else {
            reset();
            retryAt = now + 2000;
        }
    }

    public boolean isPlaying(int token) {
        return !closed && controls && requested && playing && token == playbackToken;
    }

    public void close() {
        reset();
        selected = null;
        closed = true;
    }

    private void play() {
        playing = true;
        output.startPlayback(++playbackToken);
    }

    private void stop() {
        playing = false;
        playbackToken++;
        output.stopPlayback();
    }

    private void reset() {
        boolean held = requested;
        requested = false;
        waitingForGain = false;
        requestToken++;
        stop();
        if (held) output.abandonFocus();
    }
}
