package core.bluetooth.requests;


import config.RecordConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private boolean isStart = false;
    private boolean monitorDeviceStatus = false;
    private boolean computeQualities = false;
    private RecordConfig recordConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus, RecordConfig recordConfig){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
        this.recordConfig = recordConfig;
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

    public boolean recordDataAsJson() {
        return recordConfig != null;
    }

    public RecordConfig getRecordConfig() {
        return recordConfig;
    }
}
