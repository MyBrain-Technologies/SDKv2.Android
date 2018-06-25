package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;

/**
 * An event class when a disconnection request is being sent by the user.
 */
public class DisconnectRequestEvent extends BluetoothRequests {

    private final boolean isInterrupted;

    public DisconnectRequestEvent(boolean isInterrupted){
        this.isInterrupted = isInterrupted;
    }


    public boolean isInterrupted() {
        return isInterrupted;
    }
}