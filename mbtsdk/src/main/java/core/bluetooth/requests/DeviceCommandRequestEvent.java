package core.bluetooth.requests;

import command.DeviceCommand;

/**
 * An event class when a device configuration update request is being sent by the user.
 */
public class DeviceCommandRequestEvent/* extends BluetoothRequests*/{
    private final DeviceCommand command;

    public DeviceCommandRequestEvent(DeviceCommand command) {
        this.command = command;
    }

    public DeviceCommand getCommand() {
        return command;
    }

}

