package eventbus.events;

import android.support.annotation.NonNull;

import features.ScannableDevices;

/**
 * Event posted when a new raw EEG data acquired is transmitted through Bluetooth
 * Event data contains the raw EEG data array
 *
 * @author Sophie Zecri on 29/05/2018
 */
public class BluetoothEEGEvent {

    private byte[] data;
    private ScannableDevices deviceType;

    public BluetoothEEGEvent(@NonNull byte[] data, ScannableDevices deviceType) {
        this.data = data;
        this.deviceType = deviceType;
    }

    /**
     * Gets the raw EEG data array acquired
     * @return the raw EEG data array acquired
     */
    public byte[] getData() {
        return data;
    }

    public ScannableDevices getDeviceType() {
        return deviceType;
    }
}
