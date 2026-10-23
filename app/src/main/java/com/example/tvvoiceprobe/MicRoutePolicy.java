package com.example.tvvoiceprobe;

import android.media.AudioDeviceInfo;

/**
 * Conservative audio INPUT policy. Android audio-policy configuration isn't
 * proof that an input is connected. Only getDevices(GET_DEVICES_INPUTS) lists
 * available sources; AudioRecord.getRoutedDevice() checks actual routing.
 *
 * Avoid false negatives for vendor-defined / unknown input types. Quiet
 * speech or silent samples are never evidence of a missing microphone.
 */
public final class MicRoutePolicy {
    public static final String NO_MIC_MESSAGE =
            "No accessible microphone detected. Connect a compatible USB mic or "
            + "use a TV microphone available to third-party apps. "
            + "Open Hardware microphone diagnostics for details.";

    private MicRoutePolicy() { }

    public static boolean unavailableRoute(int type) {
        return type == AudioDeviceInfo.TYPE_REMOTE_SUBMIX
                || type == AudioDeviceInfo.TYPE_TV_TUNER
                || type == AudioDeviceInfo.TYPE_FM_TUNER
                || type == AudioDeviceInfo.TYPE_TELEPHONY;
    }

    /**
     * Unknown device types may be valid vendor microphones; don't reject
     * them. A genuine empty input list or remote-submix-only TV is blocked.
     */
    public static boolean noUsableInputs(int[] types) {
        if (types == null) return false; // Firmware query failed: inconclusive.
        for (int type : types) {
            if (!unavailableRoute(type)) return false;
        }
        return true;
    }
}
