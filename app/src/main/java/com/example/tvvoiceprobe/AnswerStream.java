package com.example.tvvoiceprobe;

/**
 * Builds the visible answer from Home Assistant's streamed chat-log deltas.
 * Kept free of Android APIs so it can be unit tested. The final intent-end
 * answer always replaces whatever was streamed.
 */
public final class AnswerStream {
    /** A runaway agent must not grow this without limit. */
    static final int MAX_CHARS = 8000;

    private final StringBuilder text = new StringBuilder();

    /**
     * @param newMessage true when the delta starts a new assistant message,
     *                   for example the reply after a tool call
     * @param piece      the next chunk of reply text, or null for none
     * @return the full text so far when something changed, otherwise null.
     *         An empty string means a new message began and nothing was said yet.
     */
    public synchronized String append(boolean newMessage, String piece) {
        boolean changed = false;
        if (newMessage && text.length() > 0) {
            text.setLength(0);
            changed = true;
        }
        if (piece != null && !piece.isEmpty() && text.length() < MAX_CHARS) {
            int room = MAX_CHARS - text.length();
            text.append(piece, 0, Math.min(piece.length(), room));
            changed = true;
        }
        return changed ? text.toString() : null;
    }

    public synchronized void reset() {
        text.setLength(0);
    }
}
