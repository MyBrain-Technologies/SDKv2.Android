package core.bluetooth.requests;


import android.util.Log;

import config.RecordConfig;
import config.StreamConfig;
import config.SynchronisationConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private final boolean isStart;
    private final boolean isRecord;
    private final RecordConfig recordConfig;
    private final StreamConfig streamConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean isRecordRequest, RecordConfig recordConfig, StreamConfig streamConfig){
        this.isStart = isStartRequest;
        this.isRecord = isRecordRequest;
        this.recordConfig = recordConfig;
        this.streamConfig = streamConfig;
    }

    public boolean isStart() {
        return isStart;
    }

    public boolean monitorDeviceStatus() {
        return streamConfig.getDeviceStatusListener() != null;
    }

    public boolean computeQualities() {
        return streamConfig.shouldComputeQualities();
    }

    public boolean recordData() {
        return recordConfig != null;
    }

    public boolean startStream() {
        Log.d("Start stream ",""+(isStart && !isRecord));
        return isStart && !isRecord;
    }

    public boolean stopStream() {
        Log.d("Stop stream ",""+(!isStart && !isRecord));
        return !isStart && !isRecord;
    }

    public RecordConfig getRecordConfig() {
        return recordConfig;
    }

    public StreamConfig getStreamConfig() {
        return streamConfig;
    }

    public SynchronisationConfig.AbstractConfig getSynchronisationConfig(){
        return streamConfig == null ? null : streamConfig.getSynchronisationConfig();
    }
}
