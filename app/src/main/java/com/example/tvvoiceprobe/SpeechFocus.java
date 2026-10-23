package com.example.tvvoiceprobe;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Requests temporary audio focus while Kellie captures speech or plays her own
 * spoken answer. By default Android is asked to duck a playing media app. In
 * pause mode the request is not a ducking one, so players that follow audio
 * focus pause and then resume when focus is released.
 *
 * This never changes the TV's saved system volume. Android and the foreground
 * player decide whether to honor the request.
 */
public final class SpeechFocus {
    /** How long a released pause-mode focus lingers so the next holder can take over. */
    public static final long HANDOFF_MS = 1500L;

    private final AudioManager audio;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayedRelease = this::release;
    private AudioFocusRequest request;
    private boolean requestPauses;

    public SpeechFocus(Context context) {
        audio = (AudioManager) context.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
    }

    /** Lower the volume of other apps. */
    public boolean acquire() {
        return acquire(false);
    }

    /** @param pauseMedia true to ask other apps to pause instead of ducking */
    public synchronized boolean acquire(boolean pauseMedia) {
        // A release that was waiting for a hand-off is no longer wanted.
        handler.removeCallbacks(delayedRelease);
        if (request != null) {
            if (requestPauses == pauseMedia) return true;
            releaseNow(); // The mode changed, so ask again with the new kind.
        }
        if (audio == null) return false;
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            AudioFocusRequest next = new AudioFocusRequest.Builder(pauseMedia
                    ? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    : AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(change -> { })
                    .build();
            if (audio.requestAudioFocus(next) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return false;
            }
            request = next;
            requestPauses = pauseMedia;
            return true;
        } catch (RuntimeException ignored) {
            // TV vendors can reject focus requests. Voice capture still works.
            return false;
        }
    }

    public synchronized void release() {
        handler.removeCallbacks(delayedRelease);
        releaseNow();
    }

    /**
     * Release after a short delay unless focus is requested again first. This
     * lets a paused app stay paused while the spoken answer takes over, instead
     * of resuming for a moment between the question and the answer.
     */
    public synchronized void releaseSoon(long delayMs) {
        if (request == null) return;
        handler.removeCallbacks(delayedRelease);
        handler.postDelayed(delayedRelease, delayMs);
    }

    private void releaseNow() {
        if (request == null) return;
        AudioFocusRequest previous = request;
        request = null;
        try {
            if (audio != null) audio.abandonAudioFocusRequest(previous);
        } catch (RuntimeException ignored) {
            // Never fail a voice session while restoring playback.
        }
    }
}
