package com.example.tvvoiceprobe;

/**
 * A TV popup's explicit remote action may be consequential. Allow one
 * dispatch only; each new popup owns a fresh gate. The Android UI thread
 * calls this, so there is no need for cross-thread state.
 */
public final class TvPopupActionGate {
    private boolean consumed;

    public boolean claim() {
        if (consumed) return false;
        consumed = true;
        return true;
    }
}
