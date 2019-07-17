package core.bluetooth.requests;


import config.SynchronisationConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private boolean isStart = false;
    private boolean monitorDeviceStatus = false;
    private boolean computeQualities = false;
    private SynchronisationConfig oscConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus, SynchronisationConfig oscConfig){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
        this.oscConfig = oscConfig;
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

    public SynchronisationConfig getOscConfig() {
        return oscConfig;
    }
}
