package core.recording;

import android.content.Context;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import config.RecordConfig;
import core.BaseModuleManager;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.DeviceEvents;
import core.eeg.MbtEEGManager;
import core.recording.localstorage.MbtRecordBuffering;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.ConnectionStateEvent;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtRecordingManager extends BaseModuleManager {

    private static String TAG = MbtRecordingManager.class.getName();

    /**
     * Recording configuration is all the data required to store the EEG packets in a JSON file.
     * The JSON file content and file name can be defined using this object.
     */
    private RecordConfig recordConfig;

    /**
     * Temporary buffer created when a recording is started,
     * and filled with EEG raw data acquired.
     * The whole buffer is saved in a JSON file when the streaming is stopped.
     */
    private MbtRecordBuffering recordBuffering;

    public MbtRecordingManager(@NonNull Context context) {
        super(context);
    }

    /**
     * Record the data in a JSON file once the streaming is stopped
     * @param request is the start or stop streaming request
     */
    @Subscribe (threadMode = ThreadMode.ASYNC)
    public void onStreamRequest(final StreamRequestEvent request) {
        if (request.isStart()) { //start streaming

            if (request.recordData()) {
                recordConfig = request.getRecordConfig();
                if(recordBuffering != null)
                    recordBuffering.resetBuffer();
                else
                    recordBuffering = new MbtRecordBuffering(mContext);
            }

        } else { //stop streaming
            try {
                Thread.sleep(100); //packets are received with a small delay
                if (request.recordData())
                    recordConfig = request.getRecordConfig();

                //Save the EEG packets and associated data on a JSON file
                if(recordConfig != null) {
                    EventBusManager.postEvent(new DeviceEvents.GetDeviceEvent(), new EventBusManager.Callback<DeviceEvents.PostDeviceEvent>() {
                        @Override
                        @Subscribe
                        public Void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                            EventBusManager.registerOrUnregister(false, this);
                            if (device != null && device.getDevice() != null)
                                recordBuffering.storeRecordBuffer(device.getDevice(), recordConfig);

                            return null;
                        }
                    });
                    if(!recordConfig.enableMultipleRecordings())
                        recordBuffering = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            recordConfig = null;
        }
    }

    @Subscribe
    public void onConnectionStateChanged(ConnectionStateEvent connectionStateEvent) {
        recordBuffering = null;
    }

    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe()
    public void onNewPackets(@NonNull final ClientReadyEEGEvent event) {
        if(recordConfig != null)
            recordBuffering.record(event.getEegPackets());
    }


}
