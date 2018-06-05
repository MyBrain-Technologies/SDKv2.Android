package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.HashMap;

import config.MbtConfig;
import core.MbtManager;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.signalprocessing.MBTComputeRelaxIndex;
import core.eeg.signalprocessing.MBTComputeStatistics;
import core.eeg.storage.MBTEEGPacket;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.storage.MbtDataBuffering;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.BluetoothEEGEvent;
import utils.AsyncUtils;

import static config.MbtConfig.sampleRate;
import static features.MbtFeatures.getBluetoothProtocol;
import static features.MbtFeatures.getNbChannels;


/**
 * MbtEEGManager contains all necessary methods to manage incoming EEG data from the MBT headset.
 * It is responsible for managing the communication between the different classes of the eeg package.
 * In chronological order, the incoming raw data are first transmitted to {@link core.eeg.acquisition.MbtDataAcquisition},
 * to be stored by {@link core.eeg.storage.MbtDataBuffering} in temporary buffers.
 * EEG data acquisition still continue until the buffers are full.
 * Then, raw data are converted into user-readable EEG values by {@link MbtDataConversion}.
 * Finally, a notification is sent to {@link core.MbtManager} when the converted EEG data buffer is full, so that the client can have access to the user-readable EEG data
 *
 * @author Etienne on 08/02/2018.
 * @version Sophie ZECRI 25/05/2018
 */

public final class MbtEEGManager {

    private static final String TAG = MbtEEGManager.class.getName();

    private Context mContext;

    private MbtDataAcquisition dataAcquisition;
    private MbtDataBuffering mbtDataBuffering;
    ArrayList<ArrayList<Float>> consolidatedEEG;


    private EventBusManager eventBusManager; // EventBus : MbtEEGManager is the subscriber for MbtDataAcquisition that will be notified for converting raw data

    private MbtManager mbtManager;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController){

        this.mContext = context;
        this.mbtManager = mbtManagerController;
        this.eventBusManager = new EventBusManager();
        this.eventBusManager.registerOrUnregister(true,this);// registers MbtEEGManager as a subscriber for receiving events from MbtBluetooth via the Event Bus
        this.dataAcquisition = new MbtDataAcquisition(this);
        this.mbtDataBuffering = new MbtDataBuffering(this);
    }

    /**
     * Converts the EEG raw data array into a user-readable EEG matrix
     * 0xFFFF values are computed a NaN values
     * @param rawData the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * This method is called by {@link core.eeg.acquisition.MbtDataAcquisition}.prepareAndConvertToEEG method
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> launchConversionToEEG(byte[] rawData){
        return consolidatedEEG = MbtDataConversion.convertRawDataToEEG(rawData, getBluetoothProtocol());
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param pendingBufferLength is the length to copy
     */
    public void storePendingDataInBuffer(final byte[] data, final int srcPos, final int bufPos,final int pendingBufferLength){
        mbtDataBuffering.storePendingDataInBuffer(data, srcPos, bufPos, pendingBufferLength);
    }

    /**
     * In case packet size is too large for buffer, the overflow EEG data is stored in an overflow buffer
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param pendingBufferLength is the length of the pending buffer not to copy : this length is subtracted to get the length of the overflowing part of the data
     */
    public void storeOverflowDataInBuffer(final byte[] data, final int srcPos, final int bufPos, final int pendingBufferLength){
        mbtDataBuffering.storeOverflowDataInBuffer(data, srcPos, bufPos, pendingBufferLength);
    }

    /**
     * Replace the pending data by the overflowing data in the pending buffer after the pending data has been handled
     */
    public int handleOverflowDataBuffer(){
        return mbtDataBuffering.handleOverflowDataBuffer();
    }

    /**
     * Reset the buffers arrays, status list, the number of status bytes and the raw Data Packet Size
     * @param sampleRate the sample rate
     * @param samplePerNotif the number of sample per notification
     * @param nbStatusBytes the number of bytes used for status data
     */
    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int nbStatusBytes){
        mbtDataBuffering.reconfigureBuffers(sampleRate,samplePerNotif,nbStatusBytes);
    }

    /**
     * Handles the raw EEG data acquired by the headset and transmitted to the application
     * onEvent is called by the Event Bus when a BluetoothEEGEvent event is posted
     * This event is published by {@link core.bluetooth.MbtBluetoothManager}:
     * this manager handles Bluetooth communication between the headset and the application and receive raw EEG data from the headset.
     * @param event contains data transmitted by the publisher : here it contains the raw EEG data array
     */
    @Subscribe
    public void onEvent(BluetoothEEGEvent event){ //warning : this method is used
        dataAcquisition.handleDataAcquired(event.getData());
    }

    /**
     * Convert the raw EEG data array into a readable EEG data matrix of float values
     * and notify that EEG data is ready to the User Interface
     * @param toDecodeBytes the EEG raw data array to convert
     * @param toDecodeStatus the status data corresponding to the EEG data array
     */
    public void convertToEEG(@NonNull final byte[] toDecodeBytes, @Nullable final ArrayList<Float> toDecodeStatus){
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "computing and sending to application");

                consolidatedEEG = MbtDataConversion.convertRawDataToEEG(toDecodeBytes, getBluetoothProtocol()); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

                ArrayList<Float> status = new ArrayList<>();
                switch(getBluetoothProtocol()){
                    case BLUETOOTH_LE:
                        status = toDecodeStatus;
                        break;
                    case BLUETOOTH_SPP:
                        status = consolidatedEEG.get(0); //the first list of the matrix is the status
                        consolidatedEEG.remove(0); //remove the first element of the EEG matrix
                        break;
                }
                ArrayList<MBTEEGPacket> mbteegPacketsBuffer = mbtDataBuffering.storeEegPacketInPacketBuffer(consolidatedEEG, status);// if the packet buffer is full, this method returns the non null packet buffer
                if(mbteegPacketsBuffer != null) //returns null while the buffer managed in the MbtDataBuffering class is not full
                    notifyEEGDataIsReady(mbteegPacketsBuffer, status, sampleRate, getNbChannels());//notify UI that eeg data are ready via callbacks called by the MbtManager
            }
        });
    }

    /**
     * Publishes a ClientReadyEEGEvent event to the Event Bus to notify the User Interface
     * @param status the status list
     * @param sampleRate the sample rate
     * @param nbChannels the number of EEG acquisition channels
     */
    public void notifyEEGDataIsReady(ArrayList<MBTEEGPacket> mbteegPackets, ArrayList<Float> status, int sampleRate, int nbChannels) {
        Log.d(TAG, "notify EEG Data Is Ready ");
        eventBusManager.postEvent(new ClientReadyEEGEvent(mbteegPackets, status, sampleRate, nbChannels));
    }

    /**
     * Initializes the main quality checker object in the JNI which will live throughout all session.
     * Should be destroyed at the end of the session
     */
    public String initQualityChecker() {
        return MBTSignalQualityChecker.initQualityChecker();
    }

    /**
     * Destroy the main quality checker object in the JNI at the end of the session.
     */
    public void deinitQualityChecker() {
        MBTSignalQualityChecker.deinitQualityChecker();
    }

    /**
     * Computes the result of the previously done session
     * @param bestChannel the best quality channel index
     * @param sampRate the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param packets the EEG packets containing EEG data, theirs status and qualities.
     * @return the result of the previously done session
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public HashMap<String, Float> computeStatistics(final int bestChannel, final int sampRate, final int packetLength, final MBTEEGPacket... packets){
        return MBTComputeStatistics.computeStatistics(bestChannel, sampRate, packetLength, packets);
    }

    /**
     * Computes the result of the previously done session
     * @param threshold the level above which the relaxation indexes are considered in a relaxed state (under this threshold, they are considered not relaxed)
     * @param snrValues the array that contains the relaxation indexes of the session
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public HashMap<String, Float> computeStatisticsSNR(final float threshold, final Float[] snrValues){
        return MBTComputeStatistics.computeStatisticsSNR(threshold, snrValues);
    }

    /**
     * Computes the quality for each provided channels
     * @param sampRate the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param channels the channel(s) to be computed
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float[] computeEEGSignalQuality(int sampRate, int packetLength, Float[] channels){
        return MBTSignalQualityChecker.computeQualitiesForPacketNew(sampRate,packetLength,channels);
    }

    /**
     * Computes the relaxation index using the provided <code>MBTEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     * @param sampRate the samprate of a channel (must be consistent)
     * @param calibParams the calibration paramters previously performed
     * the EEG packets containing EEG data, theirs status and qualities.
     * @return the relaxation index
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MBTEEGPacket... packets){
        return MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    /**
     * Resets the relaxation index.
     */
    public void reinitRelaxIndexVariables(){
        MBTComputeRelaxIndex.reinitRelaxIndexVariables();
    }
    /**
     * Get the pending EEG raw data buffer
     * @return an array containing the pending EEG raw data
     */
    public byte[] getPendingRawData() {
        return mbtDataBuffering.getPendingRawData();
    }

    /**
     * Gets the lost EEG raw data packets buffer
     * It contains only 0XFF values
     * @return the lost EEG raw data packet buffer
     */
    public byte[] getLostPacketInterpolator(){
        return mbtDataBuffering.getLostPacketInterpolator();
    }

    /**
     * Get the boolean value of hasOverflow
     * Return true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Return false if the pending data buffer size is lower than its total capacity
     * @return the boolean value of the overflow state
     */
    public boolean hasOverflow() {
        return mbtDataBuffering.hasOverflow();
    }

    /**
     * Set a boolean value to hasOverflow
     * Set true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Set false if the pending data buffer size is lower than its total capacity
     * @param hasOverflow the boolean value of the overflow state
     */
    public void setOverflow(boolean hasOverflow){
        mbtDataBuffering.setOverflow(hasOverflow);
    }

    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     * @return the MbtManager instance.
     */
    public MbtManager getMbtManager() {
        return mbtManager;
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> getConsolidatedEEG() {
        return consolidatedEEG;
    }

    /**
     * Unregister the MbtEEGManager class from the bus to avoid memory leak
     */
    public void deinit(){ //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        eventBusManager.registerOrUnregister(false,this);
    }

}
