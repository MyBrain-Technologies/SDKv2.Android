package core.bluetooth.requests;

import config.EegStreamConfig;

/**
 * An event class when a device configuration update request is being sent by the user.
 */
public class UpdateConfigurationRequestEvent extends BluetoothRequests{
    private final EegStreamConfig config;

    public UpdateConfigurationRequestEvent(EegStreamConfig config) {
        this.config = config;
    }

    public EegStreamConfig getConfig() {
        return config;
    }

}
