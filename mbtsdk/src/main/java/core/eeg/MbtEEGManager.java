package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.signalprocessing.MBTEEGPacket;
import core.eeg.storage.MbtHandleData;
import core.eeg.storage.MbtBuffering;


/**
 * Created by Etienne on 08/02/2018.
 * This class contains all necessary methods to manage incoming EEG data from the MBT headset
 * It is responsible for managing buffers size, conversion from raw packets to eeg values (voltages).
 */

public final class MbtEEGManager implements Observer {

    private static final String TAG = MbtEEGManager.class.getName();

    private static int DEFAULT_SAMPLE_PER_PACKET = 4;
    private static int SAMPLE_PER_NOTIF = DEFAULT_SAMPLE_PER_PACKET;

    private static int BLE_RAW_DATA_INDEX_SIZE = 2;
    private static int BLE_RAW_DATA_SAMPLE_SIZE = 2;
    private static int BLE_RAW_DATA_NB_CHANNEL = 2;
    private static int BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = BLE_RAW_DATA_SAMPLE_SIZE * BLE_RAW_DATA_NB_CHANNEL;
    private static int BLE_NB_STATUS_BYTES = 0;
    private static int BLE_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET*BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES + BLE_NB_STATUS_BYTES; //4 samples per packet 2 channels 2bytes data
    private static int BLE_RAW_DATA_BUFFER_SIZE = BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES * 250;

    //SPP values are kept final for the moment, should probably be modifiable in the future
    private static int SPP_RAW_DATA_INDEX_SIZE = 3;
    private static int SPP_RAW_DATA_SAMPLE_SIZE = 3;
    private static int SPP_RAW_DATA_NB_CHANNEL = 9;
    private static int SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = SPP_RAW_DATA_SAMPLE_SIZE * SPP_RAW_DATA_NB_CHANNEL;
    private static int SPP_NB_STATUS_BYTES = 3;
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
    private MbtBuffering mbtBuffering;
    private ArrayList<ArrayList<Float>> eegResult;
    private ArrayList<MBTEEGPacket> mbteegPackets;

    private MbtManager mbtManager;

    public MbtEEGManager(@NonNull Context context){
        mContext = context;
        mbtManager = new MbtManager(mContext);
        this.dataAcquisition =new MbtDataAcquisition(this);

        mbtBuffering = new MbtBuffering(this);

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

        mbtBuffering.addObserver(this);

    }

    /**
     * convert from raw packets to eeg values (voltages)
     * (from byte table to Float matrix)
     * @param data : input raw data sent from the device via bluetooth
     */
    public void handleRawData(@NonNull final byte[] data) {
        dataAcquisition.handleData(data);
    }

    /**
     * Creates the eeg data output from a simple raw data array
     * 0xFFFF values are computed a NaN values
     * @param rawDataArray the raw data coming from BLE
     */
    public void convertRawDataToEEG(byte[] rawDataArray){
        eegResult = MbtHandleData.convertRawDataToEEG(rawDataArray,mbtManager.getBluetoothProtocol(),getRawDataNbChannel());
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

    public int getSamplePerNotif() {
        return SAMPLE_PER_NOTIF;
    }

    public void setSamplePerNotif(int samplePerNotif) {
        SAMPLE_PER_NOTIF = samplePerNotif;
    }

    public int getRawDataNbChannel() {
        return RAW_DATA_NB_CHANNEL;
    }

    @Override
    public void update(Observable o, Object obj) {
        if(o instanceof MbtBuffering){
            notifyBufferIsFullReceived();
        }
    }

    public void notifyBufferIsFullReceived(){
        //convertRawDataToEEG(toDecodeBytes);
    }

    public ArrayList<ArrayList<Float>> getEegResult() {
        return eegResult;
    }

    public void storePendingBuffer(final byte[] data, final int bufPos, final int src_pos, final int length){
        mbtBuffering.storePendingBuffer(data,bufPos,src_pos,length);
    }

    public void storeOverflowBuffer(final byte[] data, final int bufPos, final int src_pos){
        mbtBuffering.storeOverflowBuffer(data,bufPos,src_pos);
    }

    public void handleOverflowBuffer(){
        mbtBuffering.handleOverflowBuffer();
    }

    public MbtManager getMbtManager() {
        return mbtManager;
    }

    public ArrayList<MBTEEGPacket> getMbteegPackets() {
        return mbteegPackets;
    }

    public byte[] getPendingRawData() {
        return mbtBuffering.getPendingRawData();
    }

    public boolean hasOverflow() {
       return mbtBuffering.hasOverflow();
    }

    public void setFirstBufferFull(boolean isFull) {
        mbtBuffering.setFirstBufferFull(isFull);
    }

    public void setSecondBufferFull(boolean isFull) {
        mbtBuffering.setSecondBufferFull(isFull);
    }

    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int statusByteNb){
        mbtBuffering.reconfigureBuffers(sampleRate,samplePerNotif,statusByteNb);
    }
}
