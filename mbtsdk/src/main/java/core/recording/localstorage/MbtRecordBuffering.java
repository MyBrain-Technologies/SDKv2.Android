package core.recording.localstorage;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import config.RecordConfig;
import core.device.model.MbtDevice;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.storage.MbtEEGPacket;
import engine.clientevents.RecordingError;
import model.MbtRecording;
import utils.LogUtils;

/**
 * MbtRecordBuffering is responsible for storing EEG raw data acquired
 * in a temporary buffer if a recording is started.
 * The whole buffer is saved in a JSON file when a streaming is stopped.
 *
 * @author Sophie ZECRI on 10/09/2018
 */
public class MbtRecordBuffering {

    private static String TAG = MbtRecordBuffering.class.getName();

    /**
     * Temporary buffer of EEG packets recorded.
     * This EEG packets are stored in a JSON file
     */
    private ArrayList<MbtEEGPacket> eegPacketsBuffer;

    /**
     * Map that stores the path of the recording as a JSON file and its associated ID
     */
    private Map<String, String> recordingsBuffer;

    private final Context mContext;

    public MbtRecordBuffering(Context mContext) {
        LogUtils.d(TAG, " Start recording ");
        this.mContext = mContext;
        recordingsBuffer = new HashMap<>();
        eegPacketsBuffer = new ArrayList<>();
    }

    public void resetPacketsBuffer(){
        eegPacketsBuffer = new ArrayList<>();
    }

    public boolean storeRecordBuffer(@NonNull MbtDevice device, @NonNull RecordConfig recordConfig) {
        if(eegPacketsBuffer == null || recordConfig == null || device == null)
            return false;

        LogUtils.d(TAG," Saving buffer of "+ eegPacketsBuffer.size()+ " packets.");

            if (recordConfig.getFilename() == null)
                recordConfig.setFilename(FileManager.createFilename(recordConfig.getTimestamp(),
                        recordConfig.getProjectName(),
                        device.getProductName(),
                        recordConfig.getSubjectID(),
                        recordConfig.getCondition()));

            LogUtils.d(TAG, "JSON(Folder: " + recordConfig.getFolder() + " Filename: " + recordConfig.getFilename() + ")" + " External storage: " + recordConfig.useExternalStorage() + ")");

            File file = FileManager.createFile(mContext,
                    recordConfig.getFolder(),
                    recordConfig.getFilename(),
                    recordConfig.useExternalStorage());
            if (file == null) {
                LogUtils.e(TAG, RecordingError.ERROR_CREATE.getMessage());
                return false;
            }

        MbtRecording recording = MbtJsonBuilder.convertEEGPacketsToRecording(
                    device.getNbChannels(),
                    recordConfig.getRecordInfo().setSPVersion(ContextSP.SP_VERSION),
                    recordConfig.getTimestamp(), eegPacketsBuffer,
                    device.getInternalConfig().getStatusBytes() > 0);
            LogUtils.d(TAG, "New recording created : " + recording.toString());

        device.setAcquisitionLocations(recordConfig.getAcquisitionLocations());
        LogUtils.d(TAG, "Device " + device.toString());

        String recordingPath = FileManager.storeRecordingInFile(mContext,
                    file,
                    recordConfig.getSubjectID(),
                    device,
                    recording,
                    recordConfig.getRecordingParameters(),
                    recordingsBuffer.size()+1,
                    recordConfig.getHeaderComments());

        LogUtils.d(TAG, "Recording stored in file: " + recordingPath);

        if (recordingPath != null) {
            recordingsBuffer.put(recordingPath, recordConfig.getRecordInfo().getRecordId());
            //Check if Recordings map is empty or not to update the number of recording in each recording JSON file
            if (recordingsBuffer.size() > 1)
                FileManager.updateJSONWithCurrentRecordNb(recordingsBuffer);
        }

        resetPacketsBuffer();

        if(!recordConfig.enableMultipleRecordings())
            recordingsBuffer = null;

        return recordingPath != null;
    }

    public void record(MbtEEGPacket eegPacket){
        if(eegPacketsBuffer != null) {
            LogUtils.d(TAG, "Record packet #"+(eegPacketsBuffer != null ? eegPacketsBuffer.size(): "0"));
            eegPacketsBuffer.add(eegPacket);
        }
    }

}
