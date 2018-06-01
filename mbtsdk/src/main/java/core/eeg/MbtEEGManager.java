package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.HashMap;

import core.MbtManager;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.signalprocessing.MBTComputeRelaxIndex;
import core.eeg.signalprocessing.MBTComputeStatistics;
import core.eeg.signalprocessing.MBTEEGPacket;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import core.eeg.storage.MbtDataConversion;
import core.eeg.storage.MbtDataBuffering;
import eventbus.EventBusManager;
import eventbus.events.EEGDataIsReady;
import eventbus.events.EEGDataAcquired;
import features.MbtFeatures;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;


/**
 * MbtEEGManager contains all necessary methods to manage incoming EEG data from the MBT headset.
 * It is responsible for managing the communication between the different classes of the eeg package.
 * In chronological order, the incoming raw data are first transmitted to {@link core.eeg.acquisition.MbtDataAcquisition},
 * to be stored by {@link core.eeg.storage.MbtDataBuffering} in temporary buffers.
 * EEG data acquisition still continue until the buffers are full.
 * Then, raw data are converted into user-readable EEG values by {@link MbtDataConversion}.
 *
 * @author Etienne on 08/02/2018.
 * @version Sophie ZECRI 25/05/2018
 */

public final class MbtEEGManager {

    private static final String TAG = MbtEEGManager.class.getName();

    private static int DEFAULT_SAMPLE_PER_PACKET = 4;

    private final static int BLE_RAW_DATA_INDEX_SIZE = 2;
    private final static int BLE_RAW_DATA_SAMPLE_SIZE = 2;
    private final static int BLE_RAW_DATA_NB_CHANNEL = 2;
    private final static int BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = BLE_RAW_DATA_SAMPLE_SIZE * BLE_RAW_DATA_NB_CHANNEL;
    private final static int BLE_NB_STATUS_BYTES = 0;
    private final static int BLE_NB_BYTES = MbtFeatures.DEFAULT_MELOMIND_NB_BYTES;
    private final static int BLE_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET*BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES + BLE_NB_STATUS_BYTES; //4 samples per packet 2 channels 2bytes data
    private final static int BLE_RAW_DATA_BUFFER_SIZE = BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES * 250;

    private final static int SPP_RAW_DATA_INDEX_SIZE = 3;
    private final static int SPP_RAW_DATA_SAMPLE_SIZE = 3;
    private final static int SPP_RAW_DATA_NB_CHANNELS = 9;
    private final static int SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = SPP_RAW_DATA_SAMPLE_SIZE * SPP_RAW_DATA_NB_CHANNELS;
    private final static int SPP_NB_STATUS_BYTES = 3;
    private final static int SPP_NB_BYTES = MbtFeatures.DEFAULT_VPRO_NB_BYTES;
    private final static int SPP_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET*SPP_RAW_DATA_NB_CHANNELS*SPP_NB_STATUS_BYTES; //4 samples per packet 9 channels 3bytes data
    private final static int SPP_RAW_DATA_BUFFER_SIZE = 6750;

    private int RAW_DATA_INDEX_SIZE;
    private int RAW_DATA_SAMPLE_SIZE;
    private int RAW_DATA_NB_CHANNEL;
    private int RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
    private int NB_STATUS_BYTES;
    private int RAW_DATA_PACKET_SIZE;
    private int RAW_DATA_BUFFER_SIZE;

    private Context mContext;

    private MbtDataAcquisition dataAcquisition;
    private MbtDataBuffering mbtDataBuffering;
    private ArrayList<ArrayList<Float>> eegResult;

    private EventBusManager eventBusManager; // EventBus : MbtEEGManager is the subscriber for MbtDataAcquisition that will be notified for converting raw data

    private MbtManager mbtManager;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController){

        this.mContext = context;
        this.mbtManager = mbtManagerController;
        this.eventBusManager = new EventBusManager(this); // registers MbtEEGManager as a subscriber for receiving events from MbtBluetooth via the Event Bus
        if (MbtFeatures.getBluetoothProtocol().equals(BLUETOOTH_LE)){
            RAW_DATA_INDEX_SIZE = BLE_RAW_DATA_INDEX_SIZE;
            RAW_DATA_SAMPLE_SIZE = BLE_RAW_DATA_SAMPLE_SIZE;
            RAW_DATA_NB_CHANNEL = BLE_RAW_DATA_NB_CHANNEL;
            RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES =BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
            NB_STATUS_BYTES = BLE_NB_STATUS_BYTES;
            RAW_DATA_PACKET_SIZE = BLE_RAW_DATA_PACKET_SIZE;
            RAW_DATA_BUFFER_SIZE = BLE_RAW_DATA_BUFFER_SIZE;
        }else if(MbtFeatures.getBluetoothProtocol().equals(BLUETOOTH_SPP)){
            RAW_DATA_INDEX_SIZE = SPP_RAW_DATA_INDEX_SIZE;
            RAW_DATA_SAMPLE_SIZE = SPP_RAW_DATA_SAMPLE_SIZE;
            RAW_DATA_NB_CHANNEL = SPP_RAW_DATA_NB_CHANNELS;
            RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES =SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
            NB_STATUS_BYTES = SPP_NB_STATUS_BYTES;
            RAW_DATA_PACKET_SIZE = SPP_RAW_DATA_PACKET_SIZE;
            RAW_DATA_BUFFER_SIZE = SPP_RAW_DATA_BUFFER_SIZE;
        }

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
        return eegResult = MbtDataConversion.convertRawDataToEEG(rawData, MbtFeatures.getBluetoothProtocol());
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
     * onEvent is called by the Event Bus when a EEGDataAcquired event is posted
     * This event is published by {@link core.bluetooth.MbtBluetoothManager}:
     * this manager handles Bluetooth communication between the headset and the application and receive raw EEG data from the headset.
     * @param event contains data transmitted by the publisher : here it contains the raw EEG data array
     */
    @Subscribe
    public void onEvent(EEGDataAcquired event){ //warning : this method is used
        dataAcquisition.handleDataAcquired(event.getData());
    }

    /**
     * Publishes a EEGDataIsReady event to the Event Bus to notify the User Interface
     * @param status the status list
     * @param sampleRate the sample rate
     * @param nbChannels the number of EEG acquisition channels
     */
    public void notifyEEGDataIsReady(ArrayList<Float> status, int sampleRate, int nbChannels) {
        Log.d(TAG, "notify EEG Data Is Ready ");
        eventBusManager.postEvent(new EEGDataIsReady(eegResult, status, sampleRate, nbChannels));
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
        return MbtDataBuffering.getPendingRawData();
    }

    /**
     * Gets the lost EEG raw data packets buffer
     * It contains only 0XFF values
     * @return the lost EEG raw data packet buffer
     */
    public byte[] getLostPacketInterpolator(){
        return MbtDataBuffering.getLostPacketInterpolator();
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
        MbtDataBuffering.setOverflow(hasOverflow);
    }

    /**
     * Gets the raw data packet size
     * @return the raw data packet size
     */
    public int getRawDataPacketSize() {
        return RAW_DATA_PACKET_SIZE;
    }

    /**
     * Sets a value to the raw data packet size
     * @param rawDataPacketSize the raw data packet size
     */
    public void setRawDataPacketSize(int rawDataPacketSize) {
        RAW_DATA_PACKET_SIZE = rawDataPacketSize;
    }

    /**
     * Gets the raw data buffer size
     * @return the raw data buffer size
     */
    public int getRawDataBufferSize() {
        return RAW_DATA_BUFFER_SIZE;
    }

    /**
     * Gets the raw data index size
     * @return the raw data index size
     */
    public int getRawDataIndexSize() {
        return RAW_DATA_INDEX_SIZE;
    }

    /**
     * Gets the number of bytes of a EEG raw data per whole channels samples
     * @return the number of bytes of a EEG raw data per whole channels samples
     */
    public int getRawDataBytesPerWholeChannelsSamples() {
        return RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
    }

    /**
     * Gets the number of bytes corresponding to one EEG data
     * @return the number of bytes corresponding to one EEG data
     */
    public int getNbStatusBytes() {
        return NB_STATUS_BYTES;
    }

    /**
     * Gets the number of bytes for a EEG raw data in case the Bluetooth protocol used is Bluetooth Low Energy
     * @return the number of bytes for a EEG raw data in case the Bluetooth protocol used is Bluetooth Low Energy
     */
    public static int getBleNbBytes() {
        return BLE_NB_BYTES;
    }

    /**
     * Gets the number of bytes for a EEG raw data in case the Bluetooth protocol used is Bluetooth Serial Port Profile
     * @return the number of bytes for a EEG raw data in case the Bluetooth protocol used is Bluetooth Low Energy
     */
    public static int getSppNbBytes() {
        return SPP_NB_BYTES;
    }

    /**
     * Gets the number of samples per packet
     * @return the number of samples per packet
     */
    public static int getSamplePerPacket() {
        return DEFAULT_SAMPLE_PER_PACKET;
    }

    /**
     * Sets a value to the number of bytes for status data
     * @param nbStatusBytes the number of bytes for status data
     */
    public void setNbStatusBytes(int nbStatusBytes) {
        NB_STATUS_BYTES = nbStatusBytes;
    }

    /**
     * Sets a value to the number of samples per EEG data packet
     * @param samplePerPacket the number of samples per EEG data packet
     */
    public void setSamplePerPacket(int samplePerPacket) {
        DEFAULT_SAMPLE_PER_PACKET = samplePerPacket;
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
    public ArrayList<ArrayList<Float>> getEegResult() {
        return eegResult;
    }

    /**
     * Unregister the MbtEEGManager class from the bus to avoid memory leak
     */
    public void deinit(){ //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        eventBusManager.registerOrUnregister(false,this);
    }

}
