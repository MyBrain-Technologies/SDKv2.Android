package command;

import androidx.annotation.Keep;

import engine.clientevents.BaseError;

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
@Keep
public abstract class BluetoothCommand <T,U extends BaseError> extends CommandInterface.MbtCommand<U> { //todo replace BaseError with a specific CommandError

    /**
     * Returns the optional data specific to the implemented class
     * @return the optional data specific to the implemented class
     */
    public abstract T getData();

    //todo add javadoc
    @Override
    public abstract boolean isValid();
}



