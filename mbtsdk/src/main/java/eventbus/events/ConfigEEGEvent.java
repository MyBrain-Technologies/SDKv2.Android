package eventbus.events;

import android.support.annotation.NonNull;

import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import features.MbtDeviceType;

/**
 * Event posted when a new raw data is transmitted by the headset to the SDK through Bluetooth
 *
 * @author Sophie Zecri on 29/05/2018
 */
public class ConfigEEGEvent {

    private MbtDevice device;
    private MbtDevice.InternalConfig config;

    public ConfigEEGEvent(@NonNull MbtDevice device, @NonNull Byte[] config) {
        this.config = (device.deviceType.equals(MbtDeviceType.MELOMIND) ?
                MelomindDevice.convertRawInternalConfig(config) : VProDevice.convertRawInternalConfig(config)) ;
        this.device = device;
    }

    /**
     * Gets the raw EEG data array acquired
     * @return the raw EEG data array acquired
     */
    public MbtDevice.InternalConfig getConfig() {
        return config;
    }

    public MbtDevice getDevice() {
        return device;
    }
}
