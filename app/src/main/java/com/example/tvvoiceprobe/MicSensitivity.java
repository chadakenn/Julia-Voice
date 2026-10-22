package com.example.tvvoiceprobe;

/** Microphone pause-detector presets. These values never start a recording. */
public final class MicSensitivity {
    public static final String QUIET = "quiet";
    public static final String NORMAL = "normal";
    public static final String TV = "tv";

    private MicSensitivity() {}

    public static String keyOrNull(String raw) {
        if (raw == null) return null;
        switch (raw.trim()) {
            case "Quiet room":
            case QUIET:
                return QUIET;
            case "Normal":
            case NORMAL:
                return NORMAL;
            case "TV on":
            case TV:
                return TV;
            default:
                return null;
        }
    }

    public static String key(String raw) {
        String value = keyOrNull(raw);
        return value == null ? NORMAL : value;
    }

    public static String label(String key) {
        switch (key(key)) {
            case QUIET: return "Quiet room";
            case TV: return "TV on";
            default: return "Normal";
        }
    }
}
