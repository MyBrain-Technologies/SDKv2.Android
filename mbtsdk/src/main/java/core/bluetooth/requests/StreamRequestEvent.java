package core.bluetooth.requests;

import config.EegStreamConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private boolean isStart = false;
    private boolean monitorDeviceStatus = false;
    private boolean computeQualities = false;
    private EegStreamConfig eegStreamConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus){
        new StreamRequestEvent(isStartRequest, computeQualities, monitorDeviceStatus, new EegStreamConfig.Builder().create());
    }

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus, EegStreamConfig eegStreamConfig){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
        this.eegStreamConfig = eegStreamConfig;
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

    public EegStreamConfig getEegStreamConfig() {
        return eegStreamConfig;
    }
}
