package core.recording.localstorage;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import config.RecordConfig;
import core.Indus5Singleton;
import core.device.model.MbtDevice;
import core.eeg.acquisition.RecordingErrorData;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.storage.MbtEEGPacket;
import engine.clientevents.RecordingError;
import features.MbtFeatures;
import model.MbtRecording;
import model.Position3D;
import model.PpgFrame;
import timber.log.Timber;
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
     * Inertial Motion Sensor (IMS) - Accelerometer
     */
    private ArrayList<Position3D> imsBuffer;

    private ArrayList<Position3D> ppgBuffer;

    /**
     * Map that stores the path of the recording as a JSON file and its associated ID
     */
    private Map<String, String> recordingsBuffer;

    private RecordingErrorData recordingErrorData;

    private final Context mContext;

    public MbtRecordBuffering(Context mContext) {
        LogUtils.d(TAG, " Start recording ");
        this.mContext = mContext;
        recordingsBuffer = new HashMap<>();
        eegPacketsBuffer = new ArrayList<>();
        imsBuffer = new ArrayList<>();
    }

    public void resetPacketsBuffer(){
        eegPacketsBuffer = new ArrayList<>();
        imsBuffer = new ArrayList<>();
    }

    public void addErrorDataInfo(RecordingErrorData errorData) {
        recordingErrorData = errorData;
    }

    public boolean storeRecordBuffer(@NonNull MbtDevice device, @NonNull RecordConfig recordConfig) {
        Timber.i("storeRecordBuffer : " + recordConfig.getDirectory() + " " + recordConfig.getFilename());
        if (eegPacketsBuffer == null || eegPacketsBuffer.isEmpty()){
            LogUtils.w(TAG," JSON file not created: null or empty buffer of EEG packets");
            return false;
        }

        if (recordConfig == null || device == null){
            LogUtils.w(TAG," JSON file not created : null device or record config ");
            return false;
        }
        ArrayList<MbtEEGPacket> eegPacketsClone = (ArrayList<MbtEEGPacket>) eegPacketsBuffer.clone();

        ArrayList<Position3D> imsClone = new ArrayList<Position3D>(imsBuffer);

        resetPacketsBuffer();

        if(recordConfig.getDuration() > 0 && eegPacketsClone.size() > recordConfig.getDuration())
            eegPacketsClone = new ArrayList<>(eegPacketsClone.subList(0,recordConfig.getDuration()));

        LogUtils.d(TAG," Saving buffer of "+ eegPacketsClone.size()+ " eeg packets.");
        LogUtils.d(TAG," Saving buffer of "+ imsClone.size()+ " ims packets.");

        if (recordConfig.getFilename() == null)
            recordConfig.setFilename(FileManager.createFilename(recordConfig.getTimestamp(),
                    recordConfig.getProjectName(),
                    device.getProductName(),
                    recordConfig.getSubjectID(),
                    recordConfig.getCondition()));

        LogUtils.d(TAG, "JSON(Folder: " + recordConfig.getDirectory() + " Filename: " + recordConfig.getFilename() + ")" + " External storage: " + recordConfig.useExternalStorage() + ")");

        File file = FileManager.createFile(mContext,
                recordConfig.getDirectory(),
                recordConfig.getFilename(),
                recordConfig.useExternalStorage());
        if (file == null) {
            LogUtils.e(TAG, RecordingError.ERROR_CREATE.getMessage());
            return false;
        }

        MbtRecording recording;
        if (Indus5Singleton.INSTANCE.isIndus5() && recordConfig.isAccelerometerEnabled()) {
            recording = MbtJsonBuilder.convertDataToRecording(
                    device.getNbChannels(),
                    recordConfig.getRecordInfo().setSPVersion(ContextSP.SP_VERSION),
                    recordConfig.getTimestamp(),
                    eegPacketsClone,
                    imsClone,
                    MbtFeatures.getNbStatusBytes(null) > 0);
            Timber.d("setRecordingErrorData");
            recording.setRecordingErrorData(recordingErrorData);
        } else {
            recording = MbtJsonBuilder.convertEEGPacketsToRecording(
                    device.getNbChannels(),
                    recordConfig.getRecordInfo().setSPVersion(ContextSP.SP_VERSION),
                    recordConfig.getTimestamp(), eegPacketsClone,
                    device.getInternalConfig().getStatusBytes() > 0);
        }

        LogUtils.d(TAG, "New recording created : " + recording.toString());

        device.setAcquisitionLocations(recordConfig.getAcquisitionLocations());
        device.setReferencesLocations(recordConfig.getReferenceLocations());
        device.setGroundsLocation(recordConfig.getGroundLocations());
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

        if(!recordConfig.enableMultipleRecordings())
            recordingsBuffer = null;

        return recordingPath != null;
    }

    public void record(MbtEEGPacket eegPacket){
        if(eegPacketsBuffer != null) {
            eegPacketsBuffer.add(eegPacket);
        }
    }

    public void recordIMS(ArrayList<Position3D> positions){
        if(imsBuffer != null) {
            imsBuffer.addAll(positions);
        }
    }

    public void recordPpg(PpgFrame data){
        //TODO: on going ppg
    }

    public boolean isEegPacketsBufferEmpty() {
        return (eegPacketsBuffer == null || eegPacketsBuffer.isEmpty());
    }
}
