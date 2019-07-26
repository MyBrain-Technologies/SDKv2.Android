package core.bluetooth.requests;


import core.bluetooth.BtProtocol;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private final boolean isStart;
    private final boolean monitorDeviceStatus;
    private final boolean computeQualities;
    private final BtProtocol btProtocol;

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus, BtProtocol btProtocol){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
        this.btProtocol = btProtocol;
    }

    public boolean isStart() {
        return isStart;
    }

    public boolean shouldMonitorDeviceStatus() {
        return monitorDeviceStatus;
    }

    public boolean shouldComputeQualities() {
        return computeQualities;
    }

    public BtProtocol getBtProtocol() {
        return btProtocol;
    }
}
