package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;

public class StreamRequestEvent extends BluetoothRequests {
    private boolean isStart;

    public StreamRequestEvent(boolean isStartRequest){
        this.isStart = isStartRequest;
    }


    public boolean isStart() {
        return isStart;
    }
}
