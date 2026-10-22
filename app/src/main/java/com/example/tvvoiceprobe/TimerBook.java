package com.example.tvvoiceprobe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Main-thread owned named timers. Deadlines use elapsed time while running. */
public final class TimerBook {
    public static final int MAX_TIMERS = 8;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

    public static final class Entry {
        public final String name;
        public final long endEpochMs;
        public final long endElapsedMs;
        public final String phrase;
        public final boolean speak;
        public boolean finished;

        Entry(String name, long epoch, long elapsed, boolean speak, String phrase) {
            this.name = name;
            endEpochMs = epoch;
            endElapsedMs = elapsed;
            this.speak = speak;
            this.phrase = phrase;
        }

        public String remaining(long now) {
            if (finished || now >= endElapsedMs) return "Done";
            long seconds = Math.max(0, (endElapsedMs - now + 999) / 1000);
            long hours = seconds / 3600;
            return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, seconds / 60 % 60, seconds % 60)
                    : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        }
    }

    public static String key(String raw) {
        if (raw == null) return "";
        String clean = raw.trim().replaceAll("\\s+", " ");
        if (!clean.matches("[A-Za-z0-9][A-Za-z0-9 _-]{0,39}")) return "";
        return clean.toLowerCase(Locale.ROOT);
    }

    public boolean start(String raw, long endEpochMs, long nowEpochMs, long nowElapsedMs,
                         boolean speak, String phrase) {
        String key = key(raw);
        if (key.isEmpty() || endEpochMs <= 0 || endEpochMs > nowEpochMs + 86400000L) return false;
        if (!entries.containsKey(key) && entries.size() >= MAX_TIMERS) return false;
        Entry old = entries.get(key);
        if (old != null && old.endEpochMs == endEpochMs) return true; // duplicate delivery
        String text = phrase == null ? "" : phrase.replaceAll("[\\p{Cntrl}]", " ").trim();
        if (text.length() > 140) text = text.substring(0, 140);
        if (text.isEmpty()) text = raw.trim() + " timer is finished.";
        Entry entry = new Entry(raw.trim().replaceAll("\\s+", " "), endEpochMs,
                nowElapsedMs + Math.max(0, endEpochMs - nowEpochMs), speak, text);
        entry.finished = endEpochMs <= nowEpochMs;
        entries.put(key, entry);
        return true;
    }

    public boolean cancel(String name) { return entries.remove(key(name)) != null; }
    public List<Entry> entries() { return new ArrayList<>(entries.values()); }
    public Entry ringing() {
        for (Entry entry : entries.values()) if (entry.finished) return entry;
        return null;
    }
    public boolean tick(long now) {
        boolean changed = false;
        for (Entry entry : entries.values()) {
            if (!entry.finished && now >= entry.endElapsedMs) { entry.finished = true; changed = true; }
        }
        return changed;
    }
    public void dismissRinging() { entries.values().removeIf(entry -> entry.finished); }
}
