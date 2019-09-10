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
    private ArrayList<MbtEEGPacket> eegPackets;

    /**
     * Map that stores the path of the recording as a JSON file and its associated ID
     */
    private Map<String, String> savedRecordings;

    private final Context mContext;

    public MbtRecordBuffering(Context mContext) {
        LogUtils.d(TAG, " Start recording ");
        this.mContext = mContext;
        savedRecordings = new HashMap<>();
        eegPackets = new ArrayList<>();
    }

    public void resetBuffer(){
        eegPackets = new ArrayList<>();
    }

    public boolean storeRecordBuffer(@NonNull MbtDevice device, @NonNull RecordConfig recordConfig) {
        if(eegPackets == null || recordConfig == null || device == null)
            return false;

        LogUtils.d(TAG," Saving buffer of "+eegPackets.size()+ " packets.");

            if (recordConfig.getFilename() == null)
                recordConfig.setFilename(FileManager.createFilename(recordConfig.getTimestamp(),
                        recordConfig.getProjectName(),
                        device.getProductName(),
                        recordConfig.getSubjectId(),
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
                    recordConfig.getTimestamp(), eegPackets,
                    device.getInternalConfig().getStatusBytes() > 0);
            LogUtils.d(TAG, "New recording created : " + recording.toString());

            String recordingPath = FileManager.storeRecordingInFile(mContext,
                    file,
                    recordConfig.getSubjectId(),
                    device,
                    recording,
                    recordConfig.getRecordingParameters(),
                    savedRecordings.size()+1,
                    recordConfig.getHeaderComments());

            LogUtils.d(TAG, "Recording stored in file: " + recordingPath);

            if (recordingPath != null) {
                savedRecordings.put(recordingPath, recordConfig.getRecordInfo().getRecordId());
                //Check if Recordings map is empty or not to update the number of recording in each recording JSON file
                if (savedRecordings.size() > 1)
                    FileManager.updateJSONWithCurrentRecordNb(savedRecordings);
            }

        eegPackets = null;

        if(!recordConfig.enableMultipleRecordings())
            savedRecordings = null;

        return true;
    }

    public void record(MbtEEGPacket eegPacket){
        if(eegPackets != null) {
            LogUtils.d(TAG, "Record packet #"+(eegPackets != null ? eegPackets.size(): "0"));
            eegPackets.add(eegPacket);
        }
    }

    public ArrayList<MbtEEGPacket> getEegPackets() {
        return eegPackets;
    }

    public void setEegPackets(ArrayList<MbtEEGPacket> eegPackets) {
        this.eegPackets = eegPackets;
    }

    public Map<String, String> getSavedRecordings() {
        return savedRecordings;
    }

    public void setSavedRecordings(Map<String, String> savedRecordings) {
        this.savedRecordings = savedRecordings;
    }
}
