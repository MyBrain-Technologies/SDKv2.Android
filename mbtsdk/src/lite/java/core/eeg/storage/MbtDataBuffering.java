package core.eeg.storage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.eeg.MbtEEGManager;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import features.MbtFeatures;
import features.ScannableDevices;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
import utils.MatrixUtils;

import static config.MbtConfig.getEegBufferLengthClientNotif;

/**
 * MbtDataBuffering is responsible for storing and managing EEG raw data acquired in temporary buffers.
 * Notifies {@link MbtDataConversion} when the raw EEG data buffer is full, so that it can convert stored EEG raw data into user-readable data.
 *
 * @author Sophie ZECRI on 10/04/2018
 * @version 25/05/2018
 */
public class MbtDataBuffering {

    private static final String TAG = MbtDataBuffering.class.getName();

    /**
     * Buffer that will manage the EEG <b>RAW</b> data. It stores {@link RawEEGSample} objects.
     */
    private ArrayList<RawEEGSample> pendingRawData; // fixed-size buffer containing eeg data

    /**
     * Object that will manage the EEG <b>CONSOLIDATED</b> data. The buffer is internal and accessible using
     * {@link MbtEEGPacket#getChannelsData()} method.
     */
    private MbtEEGPacket mbtEEGPacketsBuffer;

    /**
     * Reference to the EEG module manager
     */
    private MbtEEGManager eegManager;



    public MbtDataBuffering(@NonNull MbtEEGManager eegManagerController) {
        eegManager = eegManagerController;
        pendingRawData = new ArrayList<>();
        mbtEEGPacketsBuffer = new MbtEEGPacket();
    }

    /**
     * Stores a part of the EEG raw data inside the pendingRawData buffer when the maximum size of the buffer is reached
     * This part begins at the [srcPos] position and the number of components stored is equal to the length argument.
     * It is stored in the part of the destination array that begins at the [bufPos] position and ends at the [bufPos+length] position.
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data the source raw EEG data array acquired by the headset and transmitted by Bluetooth to the application (data is null is packets have been lost)
     */
    public void storePendingDataInBuffer(@NonNull final ArrayList<RawEEGSample> data){
        pendingRawData.addAll(data);

        if(pendingRawData.size() >= MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE){
            notifyPendingRawDataBufferFull();
            resetPendingBuffer();
        }
    }

    /**
     * This method is called by {@link #storePendingDataInBuffer(ArrayList)} when the buffer size reaches the maximum
     * allowed size. It notifies EEG manager that the buffer is ready to be converted into consolidated EEG
     */
    @SuppressWarnings("unchecked")
    private void notifyPendingRawDataBufferFull() {
        final ArrayList<RawEEGSample> rawEEGtoConvert = (ArrayList<RawEEGSample>) pendingRawData.clone(); //the pending raw data is stored in toDecodeBytes to be converted in readable EEG values
        eegManager.convertToEEG(rawEEGtoConvert);

    }


    /**
     * This method is called by {@link #storePendingDataInBuffer(ArrayList)} when the buffer size reaches the maximum
     * allowed size. It notifies EEG manager that the buffer is ready to be converted into consolidated EEG
     */
    private void notifyClientEEGDataBufferFull(MbtEEGPacket packet) {
        eegManager.notifyEEGDataIsReady(packet);
    }



    /**
     * Stores the newly created EEG packet in the Packets buffer
     * We wait to have a full packet buffer to send these EEG values to the UI.
     * @return true if the packet buffer is full (contains a number of data equals to eegBufferLengthNotification), false otherwise.
     */
    public void storeConsolidatedEegInPacketBuffer(@NonNull final ArrayList<ArrayList<Float>> consolidatedEEG, @NonNull ArrayList<Float> status) {

        int maxElementsToAppend = getBufferLengthClientNotif() - mbtEEGPacketsBuffer.getChannelsData().size();

        if(maxElementsToAppend > consolidatedEEG.size()){
            mbtEEGPacketsBuffer.getChannelsData().addAll(consolidatedEEG);
            if(mbtEEGPacketsBuffer.getStatusData() != null)
                mbtEEGPacketsBuffer.getStatusData().addAll(status);
        }else{
            if(maxElementsToAppend > 0){
                mbtEEGPacketsBuffer.getChannelsData().addAll(consolidatedEEG.subList(0, maxElementsToAppend));
                if(mbtEEGPacketsBuffer.getStatusData() != null)
                    mbtEEGPacketsBuffer.getStatusData().addAll(status.subList(0, (status.size()>= maxElementsToAppend) ? maxElementsToAppend : status.size()));

                notifyClientEEGDataBufferFull(new MbtEEGPacket(mbtEEGPacketsBuffer));

                //Reset the packet buffer and store overflow data
                mbtEEGPacketsBuffer = new MbtEEGPacket( new ArrayList<>(consolidatedEEG.subList(maxElementsToAppend, consolidatedEEG.size())),
                        status.size() != 0 ?
                                ( new ArrayList<>(status.subList(maxElementsToAppend, status.size() >= consolidatedEEG.size() ? consolidatedEEG.size() : status.size() ))) : null );
            }
        }
    }


    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers, status and packet size
     */
    public void reinitBuffers(){

        pendingRawData.clear(); //init the buffer that we will use for handle/convert EEG raw data //TODO see if mandatory to reinit this buffer
        mbtEEGPacketsBuffer = new MbtEEGPacket();

    }

    /**
     * Gets the length of the buffer that contains the EEG packets
     * a notification is sent to {@link core.MbtManager} when the converted EEG data buffer is full, so that the client can have access to the user-readable EEG data
     * @return
     */
    private int getBufferLengthClientNotif(){
        return Math.max(getEegBufferLengthClientNotif(), MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE); //todo ajouter size buffer C++
    }

    /**
     * getter for unit tests
     * @return
     */
    public ArrayList<RawEEGSample> getPendingRawData() {
        return pendingRawData;
    }

    public MbtEEGPacket getTestMbtEEGPacketsBuffer() {
        return mbtEEGPacketsBuffer;
    }

    public void setTestPendingRawData(ArrayList<RawEEGSample> pendingRawData) {
        this.pendingRawData = pendingRawData;
    }

    public void setTestMbtEEGPacketsBuffer(MbtEEGPacket mbtEEGPacketsBuffer) {
        this.mbtEEGPacketsBuffer = mbtEEGPacketsBuffer;
    }

    /**
     * Clear the pending buffer
     */
    private void resetPendingBuffer(){
        pendingRawData.clear();
    }
}
