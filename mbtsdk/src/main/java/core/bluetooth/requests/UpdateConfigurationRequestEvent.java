package core.bluetooth.requests;

import config.DeviceConfig;

public class UpdateConfigurationRequestEvent extends BluetoothRequests{
    private final DeviceConfig config;

    public UpdateConfigurationRequestEvent(DeviceConfig config){
        this.config = config;
    }

    public DeviceConfig getConfig() {
        return config;
    }
}
