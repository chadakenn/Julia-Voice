package com.example.tvvoiceprobe;

import android.media.AudioDeviceInfo;
import org.junit.Test;
import static org.junit.Assert.*;

public class MicRoutePolicyTest {
    @Test public void remoteSubmixOnlyIsNotAVoiceMicrophone() {
        assertTrue(MicRoutePolicy.noUsableInputs(new int[] {
                AudioDeviceInfo.TYPE_REMOTE_SUBMIX
        }));
    }

    @Test public void absentInputsAreRejectedButUnknownEnumerationIsNot() {
        assertTrue(MicRoutePolicy.noUsableInputs(new int[0]));
        assertFalse(MicRoutePolicy.noUsableInputs(null));
    }

    @Test public void realAudioInputsCanWorkAlongsideRemoteSubmix() {
        int[] good = {
                AudioDeviceInfo.TYPE_BUILTIN_MIC,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_BLE_HEADSET
        };
        for (int type : good) {
            assertFalse(MicRoutePolicy.noUsableInputs(new int[] {
                    AudioDeviceInfo.TYPE_REMOTE_SUBMIX, type
            }));
            assertFalse(MicRoutePolicy.unavailableRoute(type));
        }
    }

    @Test public void unknownVendorTypeDoesNotTriggerFalseNoMicError() {
        assertFalse(MicRoutePolicy.noUsableInputs(new int[] { -999 }));
    }

    @Test public void invalidActualRoutesAreRecognized() {
        assertTrue(MicRoutePolicy.unavailableRoute(AudioDeviceInfo.TYPE_REMOTE_SUBMIX));
        assertTrue(MicRoutePolicy.unavailableRoute(AudioDeviceInfo.TYPE_TV_TUNER));
    }
}
