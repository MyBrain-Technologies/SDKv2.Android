package core.bluetooth.requests;


import android.util.Log;

import config.RecordConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private boolean isStart = false;
    private boolean isRecord = false;
    private boolean monitorDeviceStatus = false;
    private boolean computeQualities = false;
    private RecordConfig recordConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean isRecordRequest, boolean computeQualities, boolean monitorDeviceStatus, RecordConfig recordConfig){
        this.isStart = isStartRequest;
        this.isRecord = isRecordRequest;
        this.monitorDeviceStatus = monitorDeviceStatus;
        this.computeQualities = computeQualities;
        this.recordConfig = recordConfig;
    }

    public boolean isStart() {
        return isStart;
    }

    public boolean monitorDeviceStatus() {
        return monitorDeviceStatus;
    }

    public boolean computeQualities() {
        return computeQualities;
    }

    public boolean recordData() {
        return recordConfig != null;
    }

    public boolean stopStream() {
        Log.d("Stop stream ? ",""+(!isStart && !isRecord));
        return !isStart && !isRecord;
    }

    public RecordConfig getRecordConfig() {
        return recordConfig;
    }

}
