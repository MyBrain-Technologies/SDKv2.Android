package core.bluetooth.requests;

import config.DeviceConfig;

/**
 * An event class when a device configuration update request is being sent by the user.
 */
public class UpdateConfigurationRequestEvent extends BluetoothRequests{
    private final DeviceConfig config;
    private boolean isHeadsetConfiguredByBluetoothManager;

    public UpdateConfigurationRequestEvent(DeviceConfig config, boolean isHeadsetConfigured) {
        this.config = config;
        this.isHeadsetConfiguredByBluetoothManager = isHeadsetConfigured;
    }

    public DeviceConfig getConfig() {
        return config;
    }

    public boolean isHeadsetConfiguredByBluetoothManager() {
        return isHeadsetConfiguredByBluetoothManager;
    }
}
