package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {
    private boolean isStart;

    public StreamRequestEvent(boolean isStartRequest){
        this.isStart = isStartRequest;
    }


    public boolean isStart() {
        return isStart;
    }
}
