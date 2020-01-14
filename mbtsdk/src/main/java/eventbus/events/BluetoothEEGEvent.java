package eventbus.events;

import androidx.annotation.NonNull;


/**
 * Event posted when a new raw EEG data acquired is transmitted through Bluetooth
 * Event data contains the raw EEG data array
 *
 * @author Sophie Zecri on 29/05/2018
 */
public class BluetoothEEGEvent {

    private byte[] data;

    public BluetoothEEGEvent(@NonNull byte[] data) {
        this.data = data;
    }

    /**
     * Gets the raw EEG data array acquired
     * @return the raw EEG data array acquired
     */
    public byte[] getData() {
        return data;
    }

}
