package core.bluetooth.requests;

import command.DeviceCommandOld;

/**
 * Event triggered when a Mailbox command request is sent from the SDK to the headset
 *  in order to configure a parameter,
 *  or get values stored by the headset
 *  or ask the headset to perform an action. .
 */
public class DeviceCommandRequestEvent extends BluetoothRequests{

    /**
     * Mailbox command sent from the SDK to the headset
     * in order to configure a parameter,
     * or get values stored by the headset
     * or ask the headset to perform an action. .
     */
    private final DeviceCommandOld command;

    /**
     * Event triggered when a Mailbox command request is sent from the SDK to the headset
     *  in order to configure a parameter,
     *  or get values stored by the headset
     *  or ask the headset to perform an action. .
     */
    public DeviceCommandRequestEvent(DeviceCommandOld command) {
        this.command = command;
    }

    /**
     * Get the mailbox command sent from the SDK to the headset
     * in order to configure a parameter,
     * or get values stored by the headset
     * or ask the headset to perform an action. .
     */
    public DeviceCommandOld getCommand() {
        return command;
    }

}
