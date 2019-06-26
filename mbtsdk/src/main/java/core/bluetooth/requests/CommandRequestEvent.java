package core.bluetooth.requests;


import command.CommandInterface.MbtCommand;

/**
 * Event triggered when a Mailbox command request is sent from the SDK to the headset
 *  in order to configure a parameter,
 *  or get values stored by the headset
 *  or ask the headset to perform an action. .
 */
public class CommandRequestEvent extends BluetoothRequests{

    /**
     * Mailbox command sent from the SDK to the headset
     * in order to configure a parameter,
     * or get values stored by the headset
     * or ask the headset to perform an action. .
     */
    private final MbtCommand command;

    /**
     * Event triggered when a Mailbox command request is sent from the SDK to the headset
     *  in order to configure a parameter,
     *  or get values stored by the headset
     *  or ask the headset to perform an action. .
     */
    public CommandRequestEvent(MbtCommand command) {
        this.command = command;
    }

    /**
     * Get the mailbox command sent from the SDK to the headset
     * in order to configure a parameter,
     * or get values stored by the headset
     * or ask the headset to perform an action. .
     */
    public MbtCommand getCommand() {
        return command;
    }

}
