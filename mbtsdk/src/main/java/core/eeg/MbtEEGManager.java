package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Arrays;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.storage.MbtDataConversion;
import core.eeg.storage.MbtDataBuffering;
import eventbus.EventBusManager;
import eventbus.events.EEGDataIsReady;
import eventbus.events.EEGDataAcquired;
import utils.AsyncUtils;


/**
 * Created by Etienne on 08/02/2018.
 * This class contains all necessary methods to manage incoming EEG data from the MBT headset
 * It is responsible for managing buffers size, conversion from raw packets to eeg values (voltages).
 */

public final class MbtEEGManager {

    private static final String TAG = MbtEEGManager.class.getName();

    private static int DEFAULT_SAMPLE_PER_PACKET = 4;
    private static int SAMPLE_PER_NOTIF = DEFAULT_SAMPLE_PER_PACKET;

    private static int BLE_RAW_DATA_INDEX_SIZE = 2;
    private static int BLE_RAW_DATA_SAMPLE_SIZE = 2;
    private static int BLE_RAW_DATA_NB_CHANNEL = 2;
    private static int BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = BLE_RAW_DATA_SAMPLE_SIZE * BLE_RAW_DATA_NB_CHANNEL;
    private static int BLE_NB_STATUS_BYTES = 0;
    public final static int BLE_NB_BYTES = 2;
    private static int BLE_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET*BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES + BLE_NB_STATUS_BYTES; //4 samples per packet 2 channels 2bytes data
    private static int BLE_RAW_DATA_BUFFER_SIZE = BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES * 250;

    //SPP values are kept final for the moment, should probably be modifiable in the future
    private static int SPP_RAW_DATA_INDEX_SIZE = 3;
    private static int SPP_RAW_DATA_SAMPLE_SIZE = 3;
    private static int SPP_RAW_DATA_NB_CHANNEL = 9;
    private static int SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = SPP_RAW_DATA_SAMPLE_SIZE * SPP_RAW_DATA_NB_CHANNEL;
    private static int SPP_NB_STATUS_BYTES = 3;
    public final static int SPP_NB_BYTES = 3;
    private static int SPP_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET*SPP_RAW_DATA_NB_CHANNEL*SPP_NB_STATUS_BYTES; //4 samples per packet 9 channels 3bytes data
    private static int SPP_RAW_DATA_BUFFER_SIZE = 6750;

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

    private EventBusManager eventBusManager;
    // EventBus : MbtEEGManager is the subscriber for MbtDataAcquisition that will be notified for converting raw data

    private MbtManager mbtManager;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController){
        this.mContext = context;
        this.mbtManager = mbtManagerController;
        this.eventBusManager = new EventBusManager(this); // register MbtEEGManager as a subscriber for receiving Event from MbtBluetooth via the Event Bus

        if (mbtManager.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_SPP)) {
            RAW_DATA_INDEX_SIZE = SPP_RAW_DATA_INDEX_SIZE;
            RAW_DATA_SAMPLE_SIZE = SPP_RAW_DATA_SAMPLE_SIZE;
            RAW_DATA_NB_CHANNEL = SPP_RAW_DATA_NB_CHANNEL;
            RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES =SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
            NB_STATUS_BYTES = SPP_NB_STATUS_BYTES;
            RAW_DATA_PACKET_SIZE = SPP_RAW_DATA_PACKET_SIZE;
            RAW_DATA_BUFFER_SIZE = SPP_RAW_DATA_BUFFER_SIZE;
        } else if (mbtManager.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE)) {
            RAW_DATA_INDEX_SIZE = BLE_RAW_DATA_INDEX_SIZE;
            RAW_DATA_SAMPLE_SIZE = BLE_RAW_DATA_SAMPLE_SIZE;
            RAW_DATA_NB_CHANNEL = BLE_RAW_DATA_NB_CHANNEL;
            RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES =BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
            NB_STATUS_BYTES = BLE_NB_STATUS_BYTES;
            RAW_DATA_PACKET_SIZE = BLE_RAW_DATA_PACKET_SIZE;
            RAW_DATA_BUFFER_SIZE = BLE_RAW_DATA_BUFFER_SIZE;
        }
        this.dataAcquisition = new MbtDataAcquisition(this);
        this.mbtDataBuffering = new MbtDataBuffering(this);

    }

    /**
     * Creates the eeg data output from a simple raw data array
     * 0xFFFF values are computed a NaN values
     * @param rawDataArray the raw data coming from BLE
     * This method is called by handleRawData method
     */
    public void convertRawDataToEEG(byte[] rawDataArray){
        eegResult = MbtDataConversion.convertRawDataToEEG(rawDataArray,mbtManager.getBluetoothProtocol(),getRawDataNbChannel());

    }

    public int getRawDataPacketSize() {
        return RAW_DATA_PACKET_SIZE;
    }

    public void setRawDataPacketSize(int rawDataPacketSize) {
        RAW_DATA_PACKET_SIZE = rawDataPacketSize;
    }

    public int getRawDataBufferSize() {
        return RAW_DATA_BUFFER_SIZE;
    }

    public int getRawDataIndexSize() {
        return RAW_DATA_INDEX_SIZE;
    }

    public int getRawDataBytesPerWholeChannelsSamples() {
        return RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
    }

    public int getNbStatusBytes() {
        return NB_STATUS_BYTES;
    }

    public void setNbStatusBytes(int nbStatusBytes) {
        NB_STATUS_BYTES = nbStatusBytes;
    }

    public void setSamplePerNotif(int samplePerNotif) {
        SAMPLE_PER_NOTIF = samplePerNotif;
    }

    public int getRawDataNbChannel() {
        return RAW_DATA_NB_CHANNEL;
    }

    public MbtManager getMbtManager() {
        return mbtManager;
    }

    public ArrayList<ArrayList<Float>> getEegResult() {
        return eegResult;
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data is the EEG raw data
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param pendingBufferLength is the length to copy
     */
    public void storePendingDataInBuffer(final byte[] data, final int srcPos, final int bufPos,final int pendingBufferLength){
        mbtDataBuffering.storePendingDataInBuffer(data,srcPos,bufPos,pendingBufferLength);
    }

    /**
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data is the EEG raw data
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param pendingBufferLength is the length of the pending buffer not to copy : this length is subtracted to get the length of the overflowing part of the data
     */
    public void storeOverflowDataInBuffer(final byte[] data, final int srcPos, final int bufPos, final int pendingBufferLength){
        mbtDataBuffering.storeOverflowDataInBuffer(data,srcPos,bufPos,pendingBufferLength);
    }

    /**
     * Replace the pending data by the overflowing data in the pending buffer after the pending data has been handled
     */
    public int handleOverflowDataBuffer(){
        return mbtDataBuffering.handleOverflowDataBuffer();
    }

    public byte[] getPendingRawData() {
        return MbtDataBuffering.getPendingRawData();
    }

    public boolean hasOverflow() {
       return mbtDataBuffering.hasOverflow();
    }

    public void setOverflow(boolean overflow){
        MbtDataBuffering.setOverflow(overflow);
    }

    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int statusByteNb){
        mbtDataBuffering.reconfigureBuffers(sampleRate,samplePerNotif,statusByteNb);
    }

    /**
     * onEvent is called when a EEGDataAcquired is posted
     * @param event contains a raw EEG data array
     */
    @Subscribe
    public void onEvent(EEGDataAcquired event){
        Log.i(TAG, "onEvent EEGDataAcquired received");
        dataAcquisition.handleDataAcquired(event.getData());
    }

    /**
     * Publish a EEGDataIsReady event to the event bus
     * @param status the status channel if present.
     * @param nbChannels the number of eeg channels
     * @param sampleRate the sample rate
     */
    public void notifyEEGDataIsReady(ArrayList<Float> status, int sampleRate, int nbChannels) {
        Log.d(TAG, "notify EEG Data Is Ready: "+eegResult.size()+"*"+eegResult.get(0).size());
        eventBusManager.postEvent(new EEGDataIsReady(eegResult, status, sampleRate, nbChannels));
    }

    public void deinit(){ //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        eventBusManager.registerOrUnregister(false,this);
    }

}
