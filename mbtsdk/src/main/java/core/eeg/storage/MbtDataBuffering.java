package core.eeg.storage;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import core.eeg.MbtEEGManager;


public class MbtDataBuffering {
    private static final String TAG = MbtDataBuffering.class.getName();

    private static byte[] oveflowBytes;
    private static byte[] pendingRawData;

    private static boolean hasOverflow;
    private static ArrayList<Float> statusData;
    private MbtEEGManager eegManager;

    // EEG Data & Values
    private static byte[] lostPacketInterpolator; // Data size + compression byte + 2 packet length bytes

    public MbtDataBuffering(MbtEEGManager eegManagerController) {

        this.eegManager = eegManagerController;
        int overflowBytesSize = 0;
        switch (eegManager.getMbtManager().getBluetoothProtocol()){
            case BLUETOOTH_LE:
                overflowBytesSize = eegManager.getRawDataPacketSize();
                break;
            case BLUETOOTH_SPP:
                overflowBytesSize = eegManager.getRawDataPacketSize()/2;
                break;
        }
        oveflowBytes = new byte[overflowBytesSize];
        pendingRawData = new byte[eegManager.getRawDataBufferSize()];
        hasOverflow = false;
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data is the EEG raw data
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     * @param bufLength is the length to copy
     */
    public void storePendingDataInBuffer(final byte[] data, final int srcPos, final int bufPos, final int bufLength){

        if (data == null || data.length == 0 || bufLength==0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        if (srcPos + bufLength > data.length || bufPos + bufLength > eegManager.getRawDataBufferSize()) //check that array indexes are not out of bounds
            throw new IndexOutOfBoundsException("array index exception !");
        System.arraycopy(data, srcPos, pendingRawData, bufPos, bufLength);
        Log.i(TAG, "storing pending data: "+Arrays.toString(Arrays.copyOfRange(data, srcPos, srcPos + bufLength)));
    }

    /**
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data is the EEG raw data
     * @param bufPos is the beginning position of the data source array
     * @param srcPos is the beginning position of the buffer list where the data are copied
     */
    public void storeOverflowDataInBuffer(final byte[] data, final int srcPos, final int bufPos, final int bufLength){

        if (data == null || data.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        //check that array indexes are not out of bounds
        if (srcPos + (eegManager.getRawDataPacketSize() - (eegManager.getRawDataBufferSize() - bufPos)) > data.length // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
                || bufPos + (eegManager.getRawDataPacketSize() - (eegManager.getRawDataBufferSize() - bufPos)) > eegManager.getRawDataBufferSize())  // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
                throw new IndexOutOfBoundsException("array index exception !");

        System.arraycopy(data, srcPos + eegManager.getRawDataBufferSize() - bufPos, oveflowBytes, 0, eegManager.getRawDataPacketSize() - bufLength);
        hasOverflow = true;
        Log.i(TAG, "storing overflow data:" + Arrays.toString(Arrays.copyOfRange(data,srcPos + eegManager.getRawDataBufferSize() - bufPos,eegManager.getRawDataPacketSize() - bufLength+srcPos + eegManager.getRawDataBufferSize() - bufPos)));
    }

    /**
     * Stores the overflow buffer in the pending buffer to handle overflow data after the pending data has been handled
     * @param length is the length of the overflow buffer to copy in the pending buffer
     */
    public void storeOverflowDataInPendingBuffer(final int length) {

        if (oveflowBytes == null || oveflowBytes.length == 0 || length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        if (eegManager.getRawDataPacketSize() - (eegManager.getRawDataBufferSize()) > oveflowBytes.length  // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
                || eegManager.getRawDataPacketSize() - (eegManager.getRawDataBufferSize()) > eegManager.getRawDataBufferSize())// if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
            throw new IndexOutOfBoundsException("array index exception !");

        System.arraycopy(oveflowBytes, 0, pendingRawData, 0, length);
        hasOverflow = false;
        Log.i(TAG, "storing Overflowing data in pending data buffer:" + Arrays.toString(oveflowBytes));

    }

    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int statusByteNb){ // statusByteNb parameter should be the internal config value

        eegManager.setNbStatusBytes(statusByteNb); //init NB_STATUS_BYTES (BLE default value is 0 et SPP default value is 3)
        eegManager.setRawDataPacketSize(eegManager.getRawDataBytesPerWholeChannelsSamples() * samplePerNotif);
        lostPacketInterpolator = new byte[2 + eegManager.getRawDataPacketSize()]; //init the lost packet buffer
        Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
        pendingRawData = new byte[eegManager.getRawDataBufferSize()]; //init the buffer that we will use for handle/convert EEG raw data
        oveflowBytes = new byte[eegManager.getRawDataPacketSize()]; //init the overflow buffer (buffer that will be used to store overflow data, while the data from pending buffer conversion is in progress)

        if(eegManager.getNbStatusBytes() > 0)
            statusData = new ArrayList<>(sampleRate);
        else
            statusData = null;
    }

    /**
     * handleOverflowBuffer is called when the pending data has been handled and
     * Replace the pending data by the overflowing data in the pending buffer after the
     */
    public int handleOverflowDataBuffer(){

        Log.i(TAG, "handling Overflow Data");
        pendingRawData = new byte[eegManager.getRawDataBufferSize()];
        Log.i(TAG, "overflow detected");
        storeOverflowDataInPendingBuffer(eegManager.getRawDataPacketSize() /2);
        hasOverflow = false; //reset overflow state
        return eegManager.getRawDataPacketSize() / 2;
    }

    public static byte[] getPendingRawData() {
        return pendingRawData;
    }

    public static byte[] getOveflowBytes() {
        return oveflowBytes;
    }

    public static void setOveflowBytes(byte[] oveflowBytes) {
        MbtDataBuffering.oveflowBytes = oveflowBytes;
    }

    public static byte[] getLostPacketInterpolator() {
        return lostPacketInterpolator;
    }

    public boolean hasOverflow() {
        return hasOverflow;
    }

    public static void setOverflow(boolean hasOverflow) {
        MbtDataBuffering.hasOverflow = hasOverflow;
    }

    public static ArrayList<Float> getStatusData() {
        return statusData;
    }
}
