package core.recording;

import android.content.Context;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import config.RecordConfig;
import core.BaseModuleManager;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.DeviceEvents;
import core.eeg.MbtEEGManager;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.storage.MbtEEGPacket;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import utils.AsyncUtils;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtRecordingManager extends BaseModuleManager {

    private RecordConfig recordConfig;
    private ArrayList<MbtEEGPacket> eegPackets;

    /**
     * Map that stores the path of the recording as a JSON file and its associated ID
     */
    private Map<String, String> savedRecordings;

    public MbtRecordingManager(@NonNull Context context) {
        super(context);
    }

    /**
     * Record the data in a JSON file once the streaming is stopped
     *
     * @param request
     */
    @Subscribe
    public void onStreamRequest(final StreamRequestEvent request) {
        if (request.isStart()) { //start streaming
            if (request.recordDataAsJson()){
                recordConfig = request.getRecordConfig();
                savedRecordings = new HashMap<>();
                eegPackets = new ArrayList<>();
            }
        } else { //stop streaming
            saveRecording();
            eegPackets = null;
            recordConfig = null;
            savedRecordings = null;
        }

    }

    private void saveRecording() {
        if(recordConfig != null){

            EventBusManager.postEvent(new DeviceEvents.GetDeviceEvent(), new EventBusManager.Callback<DeviceEvents.PostDeviceEvent>(){
                @Override
                @Subscribe
                public Void onEventCallback(DeviceEvents.PostDeviceEvent device) {
                    EventBusManager.registerOrUnregister(false,this);
                    if(device != null && device.getDevice() != null) {

                        if(recordConfig.getFilename() == null)
                            recordConfig.setFilename(FileManager.createFilename(recordConfig.getTimestamp(),
                                    recordConfig.getProjectName(),
                                    device.getDevice().getProductName(),
                                    recordConfig.getSubjectId(),
                                    recordConfig.getCondition()));

                        //Check if map is empty or not
                        if(!savedRecordings.isEmpty()){
                            AsyncUtils.executeAsync(new Runnable() {
                                @Override
                                public void run() {
                                    FileManager.updateJSONWithCurrentRecordNb(savedRecordings);
                                }
                            });
                        }

                        File file = FileManager.createFile(mContext,
                                recordConfig.getFolder(),
                                recordConfig.getFilename(),
                                recordConfig.useExternalStorage());
                        if(file == null){
                            //onError(RecordingError.ERROR_CREATE, "Null file");
                            return null;
                        }
                        FileManager.storeDataInFile(mContext,
                                file,
                                recordConfig.getSubjectId(),
                                device.getDevice(),
                                eegPackets,
                                recordConfig.getRecordingParameters(),
                                recordConfig.getRecordInfo().setSPVersion(ContextSP.SP_VERSION),
                                savedRecordings.size(),
                                recordConfig.getComments(),
                                recordConfig.getTimestamp());
                    }
                    return null;
                }
            });
        }
    }

    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe()
    public void onNewPackets(@NonNull final ClientReadyEEGEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
        record(event.getEegPackets());
    }

    private void record(MbtEEGPacket eegPacket){
        if(eegPackets != null)
            eegPackets.add(eegPacket);
    }
}
