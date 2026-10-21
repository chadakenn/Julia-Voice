package com.example.tvvoiceprobe;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

/**
 * Requests temporary audio focus so Android can duck a playing media app while
 * Kellie captures speech or plays her own spoken answer.
 *
 * This never changes the TV's saved system volume. Android and the foreground
 * player decide whether to honor ducking.
 */
public final class SpeechFocus {
    private final AudioManager audio;
    private AudioFocusRequest request;

    public SpeechFocus(Context context) {
        audio = (AudioManager) context.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
    }

    public synchronized boolean acquire() {
        if (request != null) return true;
        if (audio == null) return false;
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            AudioFocusRequest next = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(change -> { })
                    .build();
            if (audio.requestAudioFocus(next) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return false;
            }
            request = next;
            return true;
        } catch (RuntimeException ignored) {
            // TV vendors can reject focus requests. Voice capture still works.
            return false;
        }
    }

    public synchronized void release() {
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
