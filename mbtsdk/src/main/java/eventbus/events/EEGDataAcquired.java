package eventbus.events;

import android.support.annotation.NonNull;

/**
 * Event posted when a new raw EEG data acquired is transmitted through Bluetooth
 * Event data contains the raw EEG data array
 *
 * @author Sophie Zecri on 29/05/2018
 */
public class EEGDataAcquired {

    private byte[] data;

    public EEGDataAcquired(@NonNull byte[] data) {
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
