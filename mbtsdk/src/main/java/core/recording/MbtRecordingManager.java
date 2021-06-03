package core.recording;

import android.content.Context;
import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import config.RecordConfig;
import core.BaseModuleManager;
import core.Indus5Singleton;
import core.bluetooth.requests.RecordingRequestIndus5Event;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.DeviceEvents;
import core.device.event.indus5.RecordingSavedEvent;
import core.eeg.MbtEEGManager;
import core.eeg.acquisition.RecordingErrorData;
import core.recording.localstorage.MbtRecordBuffering;
import eventbus.MbtEventBus;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.IMSEvent;
import eventbus.events.PpgEvent;
import timber.log.Timber;

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
    public void onStreamRequestEvent(final StreamRequestEvent request) {
        Timber.i("StreamRequestEvent : start = %s", request.isStartStream());

        recordConfig = request.getRecordConfig();

        if (request.isStartStream()) { //start streaming

            if(recordBuffering != null)
                recordBuffering.resetPacketsBuffer();
            else
                recordBuffering = new MbtRecordBuffering(mContext);

        } else { //stop streaming
            try {
                Thread.sleep(500); //packets can be received with a small delay so we wait this packets

                if(recordConfig != null && !recordBuffering.isEegPacketsBufferEmpty())
                    storeRecording(); //Save the EEG packets and associated data on a JSON file

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            recordConfig = null;
        }
    }

    /**
     * for Indus5
     * Record the data in a JSON file once the streaming is stopped
     * @param request is the start or stop streaming request
     */
    @Subscribe (threadMode = ThreadMode.ASYNC)
    public void onStreamRequestIndus5(final RecordingRequestIndus5Event request) {
        Timber.i("onStreamRequestIndus5 : start = %s", request.isStart());

        recordConfig = request.getRecordConfig();

        if (request.isStart()) { //start streaming

            if(recordBuffering != null)
                recordBuffering.resetPacketsBuffer();
            else
                recordBuffering = new MbtRecordBuffering(mContext);

        } else { //stop streaming
            try {
                Thread.sleep(500); //packets can be received with a small delay so we wait this packets

                if(recordConfig != null && !recordBuffering.isEegPacketsBufferEmpty())
                    storeRecording(); //Save the EEG packets and associated data on a JSON file

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            recordConfig = null;
        }
    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onConnectionStateChanged(ConnectionStateEvent event) {
        if(recordBuffering != null && recordConfig != null){
            MbtEventBus.postEvent(new DeviceEvents.GetDeviceEvent(), new MbtEventBus.Callback<DeviceEvents.PostDeviceEvent>() {
                @Override
                @Subscribe
                public Void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                    MbtEventBus.registerOrUnregister(false, this);
                    if (device != null && device.getDevice() != null)
                        recordBuffering.storeRecordBuffer(device.getDevice(), recordConfig);

                    return null;
                }
            });
            if(!recordConfig.enableMultipleRecordings())
                recordBuffering = null;
        }
    }

    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewPackets(@NonNull final ClientReadyEEGEvent event) {
        if(recordBuffering != null)
            recordBuffering.record(event.getEegPackets());

    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewIMSPackets(@NonNull final IMSEvent event) {
        if(recordBuffering != null)
            recordBuffering.recordIMS(event.getPositions());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewPpgPackets(@NonNull final PpgEvent event) {
        if(recordBuffering != null)
            recordBuffering.recordPpg(event.getData());
    }

    private void storeRecording(){
        Timber.i("storeRecording : " + recordConfig.getDirectory() + " " + recordConfig.getFilename());

        if (Indus5Singleton.INSTANCE.isIndus5()) {
            RecordingErrorData clone = RecordingErrorData.getDefault().clone();
            recordBuffering.addErrorDataInfo(clone);
            recordBuffering.storeRecordBuffer(Indus5Singleton.getMbtDevice(), recordConfig);
            MbtEventBus.postEvent(new RecordingSavedEvent(recordConfig));
        } else {
            MbtEventBus.postEvent(new DeviceEvents.GetDeviceEvent(), new MbtEventBus.Callback<DeviceEvents.PostDeviceEvent>() {
                @Override
                @Subscribe
                public Void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                    MbtEventBus.registerOrUnregister(false, this);
                    if (device != null && device.getDevice() != null) {
                        recordBuffering.storeRecordBuffer(device.getDevice(), recordConfig);
                        MbtEventBus.postEvent(new RecordingSavedEvent(recordConfig));
                    }
                    return null;
                }
            });
        }

        if(!recordConfig.enableMultipleRecordings())
            recordBuffering = null;
    }


}
