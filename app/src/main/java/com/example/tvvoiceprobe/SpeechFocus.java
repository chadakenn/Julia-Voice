package com.example.tvvoiceprobe;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

/** Ducks whatever the TV is playing while Kellie listens or speaks. */
public final class SpeechFocus {
    private final AudioManager audio;
    private AudioFocusRequest request;

    public SpeechFocus(Context context) {
        audio = (AudioManager) context.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
    }

    public void acquire() {
        if (audio == null || request != null) return;
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
        if (audio.requestAudioFocus(next) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            request = next;
        }
    }

    public void release() {
        if (audio == null || request == null) return;
        audio.abandonAudioFocusRequest(request);
        request = null;
    }
}
