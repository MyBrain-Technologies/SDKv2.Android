package core.bluetooth.requests;

import android.util.Log;
import config.RecordConfig;
import config.StreamConfig;

/**
 * An event class when a stream request is being sent by the user.
 */
public class StreamRequestEvent extends BluetoothRequests {

    private final boolean isStart;
    private final boolean isRecord;
    private final StreamConfig streamConfig;
    private final RecordConfig recordConfig;

    public StreamRequestEvent(boolean isStartRequest, boolean isRecordRequest, StreamConfig streamConfig){
        this.isStart = isStartRequest;
        this.isRecord = isRecordRequest;
        this.streamConfig = streamConfig;
        this.recordConfig = null;
    }

    public StreamRequestEvent(boolean isStartRequest, boolean isRecordRequest, RecordConfig recordConfig){
        this.isStart = isStartRequest;
        this.isRecord = isRecordRequest;
        this.recordConfig = recordConfig;
        this.streamConfig = null;
    }

    public boolean isStart() {
        return isStart;
    }

    public boolean monitorDeviceStatus() {
        return streamConfig != null && streamConfig.getDeviceStatusListener() != null;
    }

    public boolean computeQualities() {
        return streamConfig != null && streamConfig.shouldComputeQualities();
    }

    public boolean recordData() {
        return recordConfig != null || streamConfig != null && streamConfig.getRecordConfig() != null;
    }

    public boolean startStream() {
        Log.d("Start stream ",""+(isStart && !isRecord));
        return isStart && !isRecord;
    }

    public boolean stopStream() {
        Log.d("Stop stream ",""+(!isStart && !isRecord));
        return !isStart && !isRecord;
    }

    public StreamConfig getStreamConfig() {
        return streamConfig;
    }

    public RecordConfig getRecordConfig() {
        return recordConfig != null ? recordConfig :
                streamConfig != null ? streamConfig.getRecordConfig() : null;
    }
}
