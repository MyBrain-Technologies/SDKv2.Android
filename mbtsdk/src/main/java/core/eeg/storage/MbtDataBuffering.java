package core.eeg.storage;

import android.support.v4.app.NavUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.eeg.MbtEEGManager;
import core.eeg.acquisition.MbtDataConversion;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static features.MbtFeatures.getNbStatusBytes;
import static features.MbtFeatures.getRawDataBufferSize;
import static features.MbtFeatures.getRawDataPacketSize;

/**
 * MbtDataBuffering is responsible for storing and managing EEG raw data acquired in temporary buffers.
 * Notifies {@link MbtDataConversion} when the raw EEG data buffer is full, so that it can convert stored EEG raw data into user-readable data.
 *
 * @author Sophie ZECRI on 10/04/2018
 * @version 25/05/2018
 */
public class MbtDataBuffering {

    private static final String TAG = MbtDataBuffering.class.getName();

    // EEG Data & Values
    private byte[] pendingRawData; // fixed-size buffer containing eeg data
    private byte[] overflowBytes; // buffer containing overflowing eeg data (is used if pending data buffer is full)
    private byte[] lostPacketInterpolator; // Data size + compression byte + 2 packet length bytes
    private ArrayList<Float> statusData;

    private boolean hasOverflow;
    private MbtEEGManager eegManager;

    private ArrayList<MBTEEGPacket> mbtEegPacketsBuffer;

    public MbtDataBuffering(MbtEEGManager eegManagerController) {

        eegManager = eegManagerController;
        hasOverflow = false;

        int lostPacketInterpolatorSize = eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE)? 2 + getRawDataPacketSize(): 138;

        overflowBytes = new byte[getRawDataPacketSize()];
        pendingRawData = new byte[getRawDataBufferSize()];
        lostPacketInterpolator = new byte[lostPacketInterpolatorSize];
        Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
        mbtEegPacketsBuffer = new ArrayList<>();
    }

    /**
     * Stores a part of the EEG raw data inside the pendingRawData buffer when the maximum size of the buffer is reached
     * This part begins at the [srcPos] position and the number of components stored is equal to the length argument.
     * It is stored in the part of the destination array that begins at the [bufPos] position and ends at the [bufPos+length] position.
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data the source raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param srcPos the beginning position of the source EEG raw data array
     * @param bufPos the beginning position of the destination array (the pendingRawData buffer)
     * @param length the number of components from the source array to store into the pendingRawData buffer
     * @throws IllegalArgumentException is raised if the source EEG data array is null or is empty
     * @throws IndexOutOfBoundsException is raised if the number of elements to store is bigger than the size of the source array or bigger than the size of the destination array

     */
    public void storePendingDataInBuffer(final byte[] data, final int srcPos, final int bufPos, final int length){

        if (data == null || data.length == 0 || length==0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        if (srcPos + length > data.length || bufPos + length > getRawDataBufferSize()) //check that array indexes are not out of bounds
            throw new IndexOutOfBoundsException("array index exception !");

        System.arraycopy(data, srcPos, pendingRawData, bufPos, length);
        Log.i(TAG, "storing pending data: "+Arrays.toString(Arrays.copyOfRange(data, srcPos, srcPos + length)));
    }

    /**
     * Stores a part of the EEG raw data inside the overflowBytes buffer in case packet size is too large for buffer
     * As the overflowing data part is the part of the data source array that has not been stored in the pending data buffer,
     * the beginning position is [srcPos + rawDataBufferSize - bufPos] and the number of components stored is equal to rawDataPacketSize - length.
     * This part is stored in the part of the destination array that begins at the first position ([0]) and ends at the [rawDataPacketSize - length] position.
     * @param data the source raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param srcPos is the beginning position of the source EEG data array from where the first part of the data has been stored into the pending data buffer.
     * @param bufPos is the beginning position of the destination array (the pending buffer) within the first part of the data has been stored
     * @param length is the number of components from the source array to store into the overflowBytes buffer
     * @throws IllegalArgumentException is raised if the source EEG data array is null or is empty
     * @throws IndexOutOfBoundsException is raised if the number of elements to store is bigger than the size of the source array or bigger than the size of the destination array
     */
    public void storeOverflowDataInBuffer(final byte[] data, final int srcPos, final int bufPos, final int length){

        if (data == null || data.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        //check that array indexes are not out of bounds
        if (srcPos + getRawDataBufferSize() - bufPos + getRawDataPacketSize() - length > data.length // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
                || getRawDataPacketSize() - length > getRawDataBufferSize())
                throw new IndexOutOfBoundsException("array index exception !");

        System.arraycopy(data, srcPos + getRawDataBufferSize() - bufPos, overflowBytes, 0, getRawDataPacketSize() - length);
        hasOverflow = true;
        Log.i(TAG, "Overflowing data stored in overflow buffer:" + Arrays.toString(Arrays.copyOfRange(data,srcPos + getRawDataBufferSize() - bufPos,getRawDataPacketSize() - length+srcPos + getRawDataBufferSize() - bufPos)));
    }

    /**
     * Stores the newly created EEG packet in the Packets buffer
     * We wait to have a full packet buffer to send these EEG values to the UI.
     * @return true if the packet buffer is full (contains a number of data equals to eegBufferLengthNotification), false otherwise.
     */
    public ArrayList<MBTEEGPacket> storeEegPacketInPacketBuffer(final ArrayList<ArrayList<Float>> consolidatedEEG, ArrayList<Float> status) {
        ArrayList<MBTEEGPacket> fullBuffer = null;

        if(mbtEegPacketsBuffer.size() == MbtConfig.getEegBufferLengthNotification()){ //if the packet buffer is full, we notify the UI via the MbtManager
            fullBuffer = mbtEegPacketsBuffer;
            mbtEegPacketsBuffer.clear(); //reset the packet buffer
        }
        mbtEegPacketsBuffer.add(new MBTEEGPacket(consolidatedEEG, null, status, System.currentTimeMillis())); //the data is stored in the buffer
        return fullBuffer;
    }


    /**
     * Stores the overflow buffer in the pending buffer to handle overflow data after the pending data has been handled
     * @param length is the length of the overflow buffer to copy in the pending buffer
     */
    public void storeOverflowDataInPendingBuffer(final int length) {

        if (overflowBytes == null || overflowBytes.length == 0 || length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        if (getRawDataPacketSize() - (getRawDataBufferSize()) > overflowBytes.length  // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
                || getRawDataPacketSize() - (getRawDataBufferSize()) > getRawDataBufferSize())
            throw new IndexOutOfBoundsException("array index exception !");

        System.arraycopy(overflowBytes, 0, pendingRawData, 0, length);
        Log.i(TAG, "Overflowing data stored in pending data buffer:" + Arrays.toString(overflowBytes));

    }

    /**
     * Reset the buffers, status, NbStatusBytes and rawDataPacketSize
     * @param sampleRate the sample rate
     * @param samplePerNotif the number of samples per notification
     * @param nbStatusByte the number of bytes used for one eeg data
     */
    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int nbStatusByte){ // statusByteNb parameter should be the internal config value

        lostPacketInterpolator = new byte[2 + getRawDataPacketSize()]; //init the lost packet buffer
        Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
        pendingRawData = new byte[getRawDataBufferSize()]; //init the buffer that we will use for handle/convert EEG raw data
        overflowBytes = new byte[getRawDataPacketSize()]; //init the overflow buffer (buffer that will be used to store overflow data, while the data from pending buffer conversion is in progress)
        statusData = (getNbStatusBytes() > 0)? statusData = new ArrayList<>(sampleRate) : null;
    }

    /**
     * handleOverflowBuffer is called when the pending data has been handled and a cloned for launching the conversion into readable EEG values
     * Replaces the pending data with the overflowing data in the pending buffer so that the overflowing data can be handled
     * @return the current buffer position
     */
    public int handleOverflowDataBuffer(){

        Log.i(TAG, "handling Overflowing Data");
        pendingRawData = new byte[getRawDataBufferSize()]; //TODO check if really mandatory
        int length = overflowBytes.length;
        storeOverflowDataInPendingBuffer(length);
        hasOverflow = false; //reset overflow state
        return length; //return the buffer position
    }

    /**
     * Gets the pendingRawData array
     * @return the pending EEG raw data buffer
     */
    public byte[] getPendingRawData() {
        return pendingRawData;
    }

    /**
     * Gets the overflowBytes array
     * @return the overflow EEG data buffer
     */
    public byte[] getOverflowBytes() {
        return overflowBytes;
    }

    /**
     * Gets the EEG packets buffer that contains the converted user-readable EEG data
     * @return the EEG packets buffer
     */
    public ArrayList<MBTEEGPacket> getMbtEegPacketsBuffer() {
        return mbtEegPacketsBuffer;
    }

    /**
     * Sets a new array to the overflowBytes array
     * @param overflowBytes is the new value for overflowBytes
     */
    public void setOverflowBytes(byte[] overflowBytes) {
        this.overflowBytes = overflowBytes;
    }

    /**
     * Gets the lost EEG raw data packets buffer
     * It contains only 0XFF values
     * @return the lost EEG raw data packet buffer
     */
    public byte[] getLostPacketInterpolator() {
        return lostPacketInterpolator;
    }

    /**
     * Get the boolean value of hasOverflow
     * Return true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Return false if the pending data buffer size is lower than its total capacity
     * @return the boolean value of the overflow state
     */
    public boolean hasOverflow() {
        return hasOverflow;
    }

    /**
     * Set a boolean value to hasOverflow
     * Set true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Set false if the pending data buffer size is lower than its total capacity
     * @param hasOverflow the boolean value of the overflow state
     */
    public void setOverflow(boolean hasOverflow) {
        this.hasOverflow = hasOverflow;
    }

    /**
     * Get the statusData list
     * @return the status corresponding to the EEG data array
     */
    public ArrayList<Float> getStatusData() {
        return statusData;
    }
}
