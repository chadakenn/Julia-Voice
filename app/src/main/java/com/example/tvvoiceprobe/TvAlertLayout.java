package com.example.tvvoiceprobe;

/**
 * Placement for announcement and camera popups only. These values never
 * describe the voice overlay. Option words match the Home Assistant selects.
 */
public final class TvAlertLayout {
    public static final String TOP_LEFT = "top_left";
    public static final String TOP_RIGHT = "top_right";
    public static final String BOTTOM_LEFT = "bottom_left";
    public static final String BOTTOM_RIGHT = "bottom_right";
    public static final String CENTER = "center";
    public static final String SMALL = "small";
    public static final String MEDIUM = "medium";
    public static final String LARGE = "large";

    private TvAlertLayout() {}

    /** @return a stored position, or null when the value is not one of the five. */
    public static String positionOrNull(String raw) {
        if (raw == null) return null;
        switch (raw.trim()) {
            case "Top left":
            case TOP_LEFT:
                return TOP_LEFT;
            case "Top right":
            case TOP_RIGHT:
                return TOP_RIGHT;
            case "Bottom left":
            case BOTTOM_LEFT:
                return BOTTOM_LEFT;
            case "Bottom right":
            case BOTTOM_RIGHT:
                return BOTTOM_RIGHT;
            case "Center":
            case CENTER:
                return CENTER;
            default:
                return null;
        }
    }

    public static String position(String raw) {
        String value = positionOrNull(raw);
        return value == null ? TOP_RIGHT : value;
    }

    public static String positionLabel(String value) {
        switch (position(value)) {
            case TOP_LEFT: return "Top left";
            case BOTTOM_LEFT: return "Bottom left";
            case BOTTOM_RIGHT: return "Bottom right";
            case CENTER: return "Center";
            default: return "Top right";
        }
    }

    /** @return a stored size, or null when the value is not small, medium, or large. */
    public static String sizeOrNull(String raw) {
        if (raw == null) return null;
        switch (raw.trim()) {
            case "Small":
            case SMALL:
                return SMALL;
            case "Medium":
            case "Medium (original)":
            case MEDIUM:
                return MEDIUM;
            case "Large":
            case LARGE:
                return LARGE;
            default:
                return null;
        }
    }

    public static String size(String raw) {
        String value = sizeOrNull(raw);
        return value == null ? MEDIUM : value;
    }

    public static String sizeLabel(String value) {
        switch (size(value)) {
            case SMALL: return "Small";
            case LARGE: return "Large";
            default: return "Medium";
        }
    }

    /** Medium widths are the popup sizes already tested on the onn. */
    public static int widthDp(String size, boolean hasPicture, boolean live) {
        int base = live ? 570 : hasPicture ? 505 : 430;
        return scale(base, size);
    }

    /** Medium heights keep today's snapshot and live frames. */
    public static int mediaHeightDp(String size, boolean live) {
        return scale(live ? 295 : 220, size);
    }

    public static int marginXDp(String position) {
        return CENTER.equals(position(position)) ? 0 : 28;
    }

    public static int marginYDp(String position) {
        return CENTER.equals(position(position)) ? 0 : 40;
    }

    private static int scale(int base, String size) {
        if (SMALL.equals(size(size))) return Math.round(base * 0.75f);
        if (LARGE.equals(size(size))) return Math.round(base * 1.25f);
        return base;
    }
}
