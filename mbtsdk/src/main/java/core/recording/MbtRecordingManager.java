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
import engine.clientevents.RecordingError;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import utils.LogUtils;

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
     * Temporary buffer of EEG packets recorded.
     * This EEG packets are stored in a JSON file
     */
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
            if (request.recordData()){
                LogUtils.d(TAG," Start recording "+recordConfig.toString());
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

    /**
     * Save the EEG packets and associated data on a JSON file
     */
    private void saveRecording() {
        if(recordConfig != null){
            LogUtils.d(TAG," Saving buffer of "+eegPackets.size()+ " packets.");

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

                        LogUtils.d(TAG,"JSON(Folder: "+recordConfig.getFolder()+" Filename: "+recordConfig.getFilename()+")"+" External storage: "+recordConfig.useExternalStorage()+")");

                        File file = FileManager.createFile(mContext,
                                recordConfig.getFolder(),
                                recordConfig.getFilename(),
                                recordConfig.useExternalStorage());
                        if(file == null){
                            LogUtils.e(TAG, RecordingError.ERROR_CREATE.getMessage());
                            //onError(RecordingError.ERROR_CREATE, "Null file");
                            return null;
                        }
                        String recordingPath = FileManager.storeDataInFile(mContext,
                                file,
                                recordConfig.getSubjectId(),
                                device.getDevice(),
                                eegPackets,
                                recordConfig.getRecordingParameters(),
                                recordConfig.getRecordInfo().setSPVersion(ContextSP.SP_VERSION),
                                savedRecordings.size(),
                                recordConfig.getHeaderComments(),
                                recordConfig.getTimestamp());

                        LogUtils.d(TAG,"Recording stored in file: "+recordingPath);

                        if(recordingPath != null) {
                            savedRecordings.put(recordingPath, recordConfig.getRecordInfo().getRecordId());
                            //Check if Recordings map is empty or not to update the number of recording in each recording JSON file
                            if (savedRecordings.size() > 1)
                                FileManager.updateJSONWithCurrentRecordNb(savedRecordings);
                        }
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
