package com.example.tvvoiceprobe;

/**
 * When the opt-in wake listener may run. A reboot, an app update, or a missing
 * permission never starts it. Home Assistant is not contacted from here.
 */
public final class WakeWordSession {
    public static final String ACTION_START = "com.example.tvvoiceprobe.WAKE_WORD_START";
    public static final String ACTION_STOP = "com.example.tvvoiceprobe.WAKE_WORD_STOP";
    /** Two seconds at 10 ms chunks, matching the Home Assistant companion cooldown. */
    public static final int COOLDOWN_CHUNKS = 200;

    private int cooldown;

    public static boolean mayStart(boolean enabled, String action,
                                   boolean micGranted, boolean overlayGranted) {
        return enabled
                && ACTION_START.equals(action)
                && micGranted
                && overlayGranted;
    }

    /** The wake listener must not hold the microphone while the voice overlay is up. */
    public static boolean shouldCapture(boolean enabled, boolean overlayOpen) {
        return enabled && !overlayOpen;
    }

    /**
     * @param detected the engine scored this chunk as the wake word
     * @return true only for a new detection, never during the cooldown
     */
    public boolean onChunk(boolean detected) {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!detected) return false;
        cooldown = COOLDOWN_CHUNKS;
        return true;
    }
}
