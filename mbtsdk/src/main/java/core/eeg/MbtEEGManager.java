package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.signalprocessing.MBTComputeRelaxIndex;
import core.eeg.signalprocessing.MBTComputeStatistics;
import core.eeg.storage.MbtEEGPacket;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.storage.MbtDataBuffering;
import core.eeg.storage.MbtRawEEG;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.BluetoothEEGEvent;
import utils.AsyncUtils;

import static config.MbtConfig.getEegBufferLengthClientNotif;
import static config.MbtConfig.getSampleRate;
import static core.eeg.signalprocessing.MBTSignalQualityChecker.computeQualitiesForPacketNew;
import static features.ScannableDevices.VPRO;


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

    private MbtManager mbtManager;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController){

        this.mContext = context;
        this.mbtManager = mbtManagerController;
        EventBusManager.registerOrUnregister(true,this);// registers MbtEEGManager as a subscriber for receiving events from MbtBluetooth via the Event Bus
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
    public ArrayList<ArrayList<Float>> launchConversionToEEG(final ArrayList<MbtRawEEG> rawData){
        return consolidatedEEG = MbtDataConversion.convertRawDataToEEG(rawData, getBluetoothProtocol());
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param rawEEGdata the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param pendingBufferLength is the length to copy
     */
    public void storePendingDataInBuffer(@Nullable final ArrayList<MbtRawEEG> rawEEGdata, final int srcPos, final int pendingBufferLength){
        mbtDataBuffering.storePendingDataInBuffer(rawEEGdata, srcPos, pendingBufferLength);
    }

    /**
     * In case packet size is too large for buffer, the overflow EEG data is stored in an overflow buffer
     * @param rawEEGdata the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param pendingBufferLength is the length of the pending buffer not to copy : this length is subtracted to get the length of the overflowing part of the data
     */
    public void storeOverflowDataInBuffer(final ArrayList<MbtRawEEG> rawEEGdata, final int srcPos, final int bufPos, final int pendingBufferLength){
        mbtDataBuffering.storeOverflowDataInBuffer(rawEEGdata, srcPos, bufPos, pendingBufferLength);
    }

    /**
     * Replace the pending data by the overflowing data in the pending buffer after the pending data has been handled
     */
    public int handleOverflowDataBuffer(){
        return mbtDataBuffering.handleOverflowDataBuffer();
    }

    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers arrays, status list, the number of status bytes and the packet Size
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
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(BluetoothEEGEvent event){ //warning : this method is used
        dataAcquisition.handleDataAcquired(event.getData());
    }

    /**
     * Convert the raw EEG data array into a readable EEG data matrix of float values
     * and notify that EEG data is ready to the User Interface
     * @param toDecodeRawEEG the EEG raw data array to convert
     * @param toDecodeStatus the status data
     */
    public void convertToEEG(@NonNull final ArrayList<MbtRawEEG> toDecodeRawEEG, @Nullable final ArrayList<Float> toDecodeStatus){
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "computing and sending to application");

                consolidatedEEG = MbtDataConversion.convertRawDataToEEG(toDecodeRawEEG, getBluetoothProtocol()); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

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
                ArrayList<MbtEEGPacket> mbtEEGPacketsBuffer = mbtDataBuffering.storeEegPacketInPacketBuffer(consolidatedEEG, status);// if the packet buffer is full, this method returns the non null packet buffer
                if(mbtEEGPacketsBuffer.size() >= getEegBufferLengthClientNotif()) //if the client buffer is full
                    notifyEEGDataIsReady(mbtEEGPacketsBuffer.get(0));//notify UI that eeg data are ready via callbacks //todo merge with etienne version
            }
        });
    }

    /**
     * Publishes a ClientReadyEEGEvent event to the Event Bus to notify the client that the EEG raw data have been converted.
     * The event returns a list of MbtEEGPacket object, that contains the EEG data, and their associated qualities and status
     * @param eegPackets the object that contains EEG packets ready to use for the client.
     */
    public void notifyEEGDataIsReady(MbtEEGPacket eegPackets) {
        Log.d(TAG, "notify EEG Data Is Ready ");
        EventBusManager.postEvent(new ClientReadyEEGEvent(eegPackets));
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
     * @param packets the EEG packets containing the EEG data matrix, their associated status and qualities.
     * @return the result of the previously done session
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public HashMap<String, Float> computeStatistics(final int bestChannel, final int sampRate, final int packetLength, final MbtEEGPacket... packets){
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
     * @param consolidatedEEG the user-readable EEG data matrix
     * @param packetLength how long is a packet (time x samprate)
     * The Melomind headset has 2 channels and the VPRO headset has 9 channels.
     * @return an array that contains the quality of each EEG acquisition channels
     * This array contains 2 qualities (items) if the headset used is MELOMIND.
     * This array contains 9 qualities (items) if the headset used is VPRO.
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public ArrayList<Float> computeEEGSignalQuality(ArrayList<ArrayList<Float>> consolidatedEEG, int packetLength){

        float[] qualities;
        Float[] channel1 = new Float[getSampleRate()];
        Float[] channel2 = new Float[getSampleRate()];
        //MELOMIND & VPRO headset have at least 2 channels
        Float[] channel3, channel4, channel5, channel6, channel7, channel8, channel9 = null;
        //For the VPRO, the others channels are initialized after
        consolidatedEEG.get(0).toArray(channel1);
        consolidatedEEG.get(1).toArray(channel2);

        if(MbtConfig.getScannableDevices().equals(VPRO)) {

            channel3 = new Float[getSampleRate()];
            channel4 = new Float[getSampleRate()];
            channel5 = new Float[getSampleRate()];
            channel6 = new Float[getSampleRate()];
            channel7 = new Float[getSampleRate()];
            channel8 = new Float[getSampleRate()];
            channel9 = new Float[getSampleRate()];

            consolidatedEEG.get(2).toArray(channel3);
            consolidatedEEG.get(3).toArray(channel4);
            consolidatedEEG.get(4).toArray(channel5);
            consolidatedEEG.get(5).toArray(channel6);
            consolidatedEEG.get(6).toArray(channel7);
            consolidatedEEG.get(7).toArray(channel8);
            consolidatedEEG.get(8).toArray(channel9);

            qualities = computeQualitiesForPacketNew(getSampleRate(),packetLength, channel1, channel2, channel3, channel4, channel5, channel6, channel7, channel8, channel9);
        }else //MELOMIND headset
            qualities = computeQualitiesForPacketNew(getSampleRate(),packetLength, channel1, channel2);
        return (ArrayList<Float>) Arrays.asList(ArrayUtils.toObject(qualities)); //convert float array into Float Arraylist
    }

    /**
     * Computes the relaxation index using the provided <code>MbtEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     * @param sampRate the samprate of a channel (must be consistent)
     * @param calibParams the calibration paramters previously performed
     * the EEG packets containing EEG data, theirs status and qualities.
     * @return the relaxation index
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MbtEEGPacket... packets){
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
    public ArrayList<MbtRawEEG> getPendingRawData() {
        return mbtDataBuffering.getPendingRawData();
    }

    /**
     * Gets the lost EEG raw data packets buffer
     * It contains only 0XFF values
     * @return the lost EEG raw data packet buffer
     */
    public ArrayList<MbtRawEEG> getLostPacketInterpolator(){
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
     * Gets the bluetooth protocolfor transmitting data from the headset to the application.
     * @return the bluetooth protocol used for transmitting data from the headset to the application.
     */
    public BtProtocol getBluetoothProtocol() {
        return mbtManager.getBluetoothProtocol();
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> getConsolidatedEEG() {
        return consolidatedEEG;
    }

    /**
     * Gets the instance of MbtDataAcquisition
     * @return the instance of MbtDataAcquisition
     */
    public MbtDataAcquisition getDataAcquisition() {
        return dataAcquisition;
    }

    /**
     * Unregister the MbtEEGManager class from the bus to avoid memory leak
     */
    public void deinit(){ //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        EventBusManager.registerOrUnregister(false,this);
    }

}
