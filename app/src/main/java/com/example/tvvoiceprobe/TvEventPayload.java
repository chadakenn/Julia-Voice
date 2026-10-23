package com.example.tvvoiceprobe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Strict, dependency-free validation for authenticated Home Assistant TV events. */
public final class TvEventPayload {
    public static final String ANNOUNCEMENT = "kellies_voice_announcement";
    public static final String CAMERA_POPUP = "kellies_voice_camera_popup";
    public static final String LIVE_CAMERA = "kellies_voice_live_camera";
    public static final String HIDE_CAMERA = "kellies_voice_hide_camera";
    public static final String ACTION = "kellies_voice_announcement_action";
    private static final Pattern CAMERA_ID = Pattern.compile("camera\\.[a-zA-Z0-9_]{1,80}");
    private static final Pattern ACTION_ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");

    public static final class Action {
        public final String id;
        public final String label;

        private Action(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private TvEventPayload() { }

    public static boolean addressedTo(String requestedId, String thisId) {
        return requestedId == null || requestedId.trim().isEmpty()
                || (thisId != null && requestedId.trim().equals(thisId));
    }

    public static String cleanText(String value, int maximum) {
        if (value == null) return "";
        String text = value.trim().replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");
        int limit = Math.max(1, maximum);
        return text.length() <= limit ? text : text.substring(0, limit).trim();
    }

    public static int duration(int seconds) {
        return Math.min(45, Math.max(4, seconds <= 0 ? 10 : seconds));
    }

    public static int liveDuration(int seconds) {
        return Math.min(180, Math.max(15, seconds <= 0 ? 90 : seconds));
    }

    public static boolean hlsPathAllowed(String path) {
        return path != null && path.startsWith("/api/hls/")
                && path.endsWith(".m3u8") && !path.contains("..")
                && !path.contains("\\") && !path.contains("//")
                && !path.contains("?") && !path.contains("#")
                && path.length() <= 600;
    }

    public static String cameraEntity(String value) {
        if (value == null) return null;
        String entity = value.trim();
        return CAMERA_ID.matcher(entity).matches() ? entity : null;
    }

    /** 32 hex characters, the id Home Assistant puts on the alert. */
    public static String announcementId(String value) {
        if (value == null) return "";
        String id = value.trim().toLowerCase();
        return id.matches("[0-9a-f]{32}") ? id : "";
    }

    /** A sentence the TV may speak. A web address, or anything too long, stays silent. */
    public static String speak(String value) {
        if (value == null) return "";
        String text = value.trim().replaceAll("\\s+", " ");
        String lower = text.toLowerCase();
        if (text.isEmpty() || text.length() > 180) return "";
        if (lower.contains("://") || lower.startsWith("http") || lower.contains("www.")) return "";
        return text;
    }

    public static Action action(String id, String label) {
        if (id == null || label == null) return null;
        String cleanId = id.trim().toLowerCase();
        String cleanLabel = cleanText(label, 24);
        if (!ACTION_ID.matcher(cleanId).matches() || cleanLabel.isEmpty()) return null;
        return new Action(cleanId, cleanLabel);
    }

    /** At most three buttons, and each id only once. Null candidates are skipped. */
    public static List<Action> takeActions(Action[] candidates) {
        ArrayList<Action> actions = new ArrayList<>();
        if (candidates == null) return actions;
        for (Action action : candidates) {
            if (action == null || actions.size() == 3) continue;
            boolean duplicate = false;
            for (Action existing : actions) {
                if (existing.id.equals(action.id)) duplicate = true;
            }
            if (!duplicate) actions.add(action);
        }
        return actions;
    }
}
