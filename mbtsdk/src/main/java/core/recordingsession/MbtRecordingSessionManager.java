package core.recordingsession;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import core.BaseModuleManager;
import core.MbtManager;
import core.eeg.storage.MBTEEGPacket;
import features.MbtFeatures;
import model.Comment;
import model.MbtRecording;
import model.RecordInfo;
import utils.MbtJsonUtils;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtRecordingSessionManager extends BaseModuleManager{

    private ArrayList<MBTEEGPacket> mbteegPackets;
    private ArrayList<Comment> comments;

    private MbtRecording currentRecordInfo;
    private boolean recordJSON = false;

    private RecordInfo recordInfo;
    private long recordTimestamp;

    private boolean isRecording = false;

    public MbtRecordingSessionManager(@NonNull Context context , MbtManager mbtManager){
        super(context, mbtManager);
    }

    /**
     * Initializes a new record session
     * for saving EEG data during a period
     * that ends when stopRecord method is called.
     * @param recordingType the type of record if explicit
     */
    public void startRecord(final RecordInfo.RecordingType recordingType) {
        if(recordInfo != null)
            recordInfo.setRecordingType(recordingType);
        startRecord();

    }

    /**
     * Starts a new record session
     * for saving EEG data during a period
     * that ends when stopRecord method is called.
     */
    public void startRecord() {
        recordTimestamp = System.currentTimeMillis();
        mbteegPackets = new ArrayList<>();
        comments = new ArrayList<>();
        recordJSON = true;
        isRecording = true;
    }

    /**
     * Stops current record and converts saved data into a new <b>MbtRecording</b> object
     */
    public void stopRecord() {
        recordJSON = false;
        isRecording = false;
        currentRecordInfo = MbtJsonUtils.convertEEGPacketListToRecordings(recordInfo, recordTimestamp, mbteegPackets, MbtFeatures.getNbBytes() > 0);
    }

    /**
     * Save the current recording into a JSON File
     */
    public void saveRecord() {
        //todo
       }

    /**
     * Gets the last EEG data recording
     * @return the last EEG data recording
     */
    public MbtRecording getCurrentRecording() {
        return currentRecordInfo;
    }

    /**
     * Sends the created JSON files to the server that contains EEG recorded data
     */
    public void sendJSONtoServer() {
        //todo
    }
}
