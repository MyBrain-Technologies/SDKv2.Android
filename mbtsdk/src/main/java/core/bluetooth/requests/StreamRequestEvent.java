package core.bluetooth.requests;


/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private boolean isStart = false;
    private boolean monitorDeviceStatus = false;
    private boolean computeQualities = false;

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
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

}
