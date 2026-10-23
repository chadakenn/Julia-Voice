package com.example.tvvoiceprobe;

/**
 * Which recorder the voice overlay is responsible for.
 * Idle is the only phase that may use the non-microphone foreground type.
 * Preview, handoff, and Assist all count as the microphone being on.
 * The wake-word service is separate and releases its recorder before this overlay opens.
 */
public final class MicOwnership {
    public static final int IDLE = 0;
    public static final int PREVIEW = 1;
    public static final int HANDOFF = 2;
    public static final int ASSIST = 3;

    private int phase = IDLE;

    public int phase() { return phase; }

    public boolean microphoneActive() { return phase != IDLE; }

    /** Follow-up preview, including the moment the same recorder is handed to Assist. */
    public boolean followUpCapture() { return phase == PREVIEW || phase == HANDOFF; }

    /** Call before the follow-up preview opens a recorder. Does not interrupt Assist. */
    public int armPreview() {
        if (phase == ASSIST || phase == HANDOFF) return phase;
        phase = PREVIEW;
        return phase;
    }

    /** Preview failed before a recorder existed. */
    public int previewDidNotOpen() {
        if (phase == PREVIEW) phase = IDLE;
        return phase;
    }

    /**
     * The preview recorder has been released (timeout or miss).
     * A handoff keeps the same recorder, so this does not clear that phase.
     */
    public int previewReleased() {
        if (phase == PREVIEW) phase = IDLE;
        return phase;
    }

    /** The preview recorder is moving to Assist. This must not pass through idle. */
    public int beginHandoff() {
        phase = HANDOFF;
        return phase;
    }

    public int assistOpened() {
        phase = ASSIST;
        return phase;
    }

    /** Assist finished and its recorder is already released. Leaves a live preview alone. */
    public int assistReleased() {
        if (phase == ASSIST) phase = IDLE;
        return phase;
    }

    /** STOP, Back, close, playback error, or a dropped connection. No recorder remains. */
    public int releaseAll() {
        phase = IDLE;
        return phase;
    }
}
