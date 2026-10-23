package com.example.tvvoiceprobe;

/**
 * What other TV apps do while Kellie listens or speaks. Lowering the volume is
 * the original behavior. Pausing asks the playing app to pause through Android
 * audio focus. Neither choice can start the microphone.
 */
public final class MediaBehavior {
    public static final String DUCK = "duck";
    public static final String PAUSE = "pause";

    private MediaBehavior() {}

    public static String keyOrNull(String raw) {
        if (raw == null) return null;
        switch (raw.trim()) {
            case "Lower volume":
            case DUCK:
                return DUCK;
            case "Pause playback":
            case PAUSE:
                return PAUSE;
            default:
                return null;
        }
    }

    public static String key(String raw) {
        String value = keyOrNull(raw);
        return value == null ? DUCK : value;
    }

    public static String label(String key) {
        return PAUSE.equals(key(key)) ? "Pause playback" : "Lower volume";
    }
}
