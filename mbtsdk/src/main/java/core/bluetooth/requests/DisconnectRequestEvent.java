package core.bluetooth.requests;

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