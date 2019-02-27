package eventbus.events;

import android.support.annotation.NonNull;

/**
 * Event posted when a new raw EEG data acquired is transmitted through Bluetooth
 * Event data contains the raw EEG data array
 *
 * @author Sophie Zecri on 29/05/2018
 */
public class ConfigEEGEvent {

    private Byte[] config;

    public ConfigEEGEvent(@NonNull Byte[] config) {
        this.config = config;
    }

    /**
     * Gets the raw EEG data array acquired
     * @return the raw EEG data array acquired
     */
    public Byte[] getConfig() {
        return config;
    }

}
