package core.bluetooth.requests;

import config.DeviceConfig;

/**
 * An event class when a device configuration update request is being sent by the user.
 */
public class UpdateConfigurationRequestEvent extends BluetoothRequests{
    private final DeviceConfig config;

    public UpdateConfigurationRequestEvent(DeviceConfig config){
        this.config = config;
    }

    public DeviceConfig getConfig() {
        return config;
    }
}
