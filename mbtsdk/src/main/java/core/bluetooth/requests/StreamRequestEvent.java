package core.bluetooth.requests;

import config.DeviceConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {
    private final boolean isStart;
    private final boolean monitorDeviceStatus;
    private final boolean computeQualities;
    private DeviceConfig deviceConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
    }
    public StreamRequestEvent(boolean isStartRequest, boolean computeQualities, boolean monitorDeviceStatus, DeviceConfig deviceConfig){
        this.isStart = isStartRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
        this.deviceConfig = deviceConfig;
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

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }
}
