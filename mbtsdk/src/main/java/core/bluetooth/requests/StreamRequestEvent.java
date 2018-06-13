package core.bluetooth.requests;

import core.bluetooth.requests.BluetoothRequests;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {
    private final boolean isStart;
    private final boolean monitorDeviceStatus;

    public StreamRequestEvent(boolean isStartRequest, boolean monitorDeviceStatus){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
    }


    public boolean isStart() {
        return isStart;
    }


    public boolean shouldMonitorDeviceStatus() {
        return monitorDeviceStatus;
    }
}
