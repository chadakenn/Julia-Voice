package com.example.tvvoiceprobe;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a Home Assistant forecast event into at most seven display rows.
 * It does not fetch weather and it does not accept image addresses.
 */
public final class ForecastCard {
    public static final String EVENT = "kellies_voice_forecast";

    public final String location;
    public final String units;
    public final int seconds;
    public final List<Day> days;

    private ForecastCard(String location, String units, int seconds, List<Day> days) {
        this.location = location;
        this.units = units;
        this.seconds = seconds;
        this.days = days;
    }

    public static final class Day {
        public final String label;
        public final String condition;
        public final Integer high;
        public final Integer low;
        public final Integer precipitation;

        public Day(String label, String condition, Integer high, Integer low, Integer precipitation) {
            this.label = label;
            this.condition = condition;
            this.high = high;
            this.low = low;
            this.precipitation = precipitation;
        }

        public String line(String units) {
            StringBuilder text = new StringBuilder();
            text.append(glyph(condition)).append("  ").append(label).append("   ");
            text.append(high == null ? "--" : high.toString());
            text.append(units).append(" / ");
            text.append(low == null ? "--" : low.toString()).append(units);
            if (precipitation != null) text.append("   ").append(precipitation).append("%");
            return text.toString();
        }
    }

    public static boolean fresh(long issuedEpochSeconds, long nowEpochSeconds) {
        if (issuedEpochSeconds <= 0) return true;
        long age = nowEpochSeconds - issuedEpochSeconds;
        return age >= -30 && age <= 120;
    }

    public static Day day(String label, String condition, String high, String low, String precipitation) {
        String name = clean(label, 16);
        if (name.isEmpty()) return null;
        Integer warm = number(high, -80, 140);
        Integer cool = number(low, -80, 140);
        if (warm == null && cool == null) return null;
        return new Day(name, conditionKey(condition), warm, cool, number(precipitation, 0, 100));
    }

    public static ForecastCard card(String location, String units, int seconds, List<Day> source) {
        List<Day> days = new ArrayList<>();
        if (source != null) {
            for (Day day : source) {
                if (day == null) continue;
                days.add(day);
                if (days.size() == 7) break;
            }
        }
        if (days.isEmpty()) return null;
        String place = clean(location, 40);
        if (place.isEmpty()) place = "Forecast";
        String unit = "°F".equals(units) || "°C".equals(units) ? units : "°";
        int shown = seconds < 8 || seconds > 45 ? 20 : seconds;
        return new ForecastCard(place, unit, shown, days);
    }

    public String heading() {
        return days.size() == 1 ? "1 day" : days.size() + " day forecast";
    }

    public static String glyph(String condition) {
        if (condition == null) return "•";
        switch (condition) {
            case "sunny": return "☀";
            case "partlycloudy": return "⛅";
            case "cloudy": return "☁";
            case "rainy":
            case "pouring":
            case "hail":
            case "lightning-rainy":
                return "☂";
            case "lightning": return "⚡";
            case "snowy":
            case "snowy-rainy":
                return "❄";
            case "fog": return "≋";
            case "clear-night": return "☾";
            case "windy":
            case "windy-variant":
                return "≈";
            default: return "•";
        }
    }

    private static String conditionKey(String raw) {
        if (raw == null) return "unknown";
        switch (raw.trim()) {
            case "clear-night":
            case "cloudy":
            case "exceptional":
            case "fog":
            case "hail":
            case "lightning":
            case "lightning-rainy":
            case "partlycloudy":
            case "pouring":
            case "rainy":
            case "snowy":
            case "snowy-rainy":
            case "sunny":
            case "windy":
            case "windy-variant":
                return raw.trim();
            default:
                return "unknown";
        }
    }

    private static String clean(String value, int maximum) {
        if (value == null) return "";
        String text = value.trim().replaceAll("[\\p{Cntrl}]", "");
        if (text.contains("://")) return "";
        return text.length() <= maximum ? text : text.substring(0, maximum).trim();
    }

    private static Integer number(String raw, int low, int high) {
        if (raw == null || raw.trim().isEmpty() || "null".equals(raw)) return null;
        try {
            int value = (int) Math.round(Double.parseDouble(raw.trim()));
            if (value < low || value > high) return null;
            return value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
