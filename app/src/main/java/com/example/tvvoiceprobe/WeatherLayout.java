package com.example.tvvoiceprobe;

/** Placement for the forecast card only. Cameras and the voice overlay ignore these. */
public final class WeatherLayout {
    public static final String BOTTOM_CENTER = "bottom_center";

    private WeatherLayout() {}

    public static String positionOrNull(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if ("Bottom center".equals(value) || BOTTOM_CENTER.equals(value)) return BOTTOM_CENTER;
        return TvAlertLayout.positionOrNull(value);
    }

    public static String position(String raw) {
        String value = positionOrNull(raw);
        return value == null ? BOTTOM_CENTER : value;
    }

    public static String positionLabel(String value) {
        if (BOTTOM_CENTER.equals(position(value))) return "Bottom center";
        return TvAlertLayout.positionLabel(value);
    }

    public static int durationSeconds(String raw) {
        if (raw == null) return 20;
        switch (raw.trim()) {
            case "8":
            case "8 seconds":
                return 8;
            case "30":
            case "30 seconds":
                return 30;
            case "45":
            case "45 seconds":
                return 45;
            case "20":
            case "20 seconds":
                return 20;
            default:
                return -1;
        }
    }

    public static String durationLabel(int seconds) {
        if (seconds == 8 || seconds == 30 || seconds == 45) return seconds + " seconds";
        return "20 seconds";
    }

    public static int widthDp(String size) {
        return TvAlertLayout.widthDp(size, true, false);
    }
}
