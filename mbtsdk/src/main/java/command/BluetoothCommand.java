package command;



import engine.clientevents.BaseError;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
public abstract class BluetoothCommand <T,U extends BaseError> extends CommandInterface.MbtCommand<T,U> {

    /**
     * Returns the optional data specific to the implemented class
     * @return the optional data specific to the implemented class
     */
    public abstract T getData();

    @Override
    public abstract boolean isValid();
}



