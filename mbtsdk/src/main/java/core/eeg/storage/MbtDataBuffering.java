package core.eeg.storage;

import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import config.MbtConfig;
import core.eeg.MbtEEGManager;
import core.eeg.acquisition.MbtDataConversion;

import static config.MbtConfig.getEegBufferLengthClientNotif;
import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static features.MbtFeatures.getNbBytes;
import static features.MbtFeatures.getNbStatusBytes;
import static features.MbtFeatures.getRawDataBufferSize;
import static features.MbtFeatures.getRawDataBytesPerWholeChannelsSamples;
import static features.MbtFeatures.getRawDataPacketSize;
import static features.MbtFeatures.getSampleRate;
import static features.MbtFeatures.setNbStatusBytes;
import static features.MbtFeatures.setRawDataPacketSize;

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
    private ArrayList<MbtRawEEG> pendingRawData; // fixed-size buffer containing eeg data
    private ArrayList<MbtRawEEG> overflowBytes; // buffer containing overflowing eeg data (is used if pending data buffer is full)
    private ArrayList<MbtRawEEG> lostPacketInterpolator; // Data size + compression byte + 2 packet length bytes
    private ArrayList<Float> statusData;

    private int lostPacketInterpolatorSize;
    private boolean hasOverflow;
    private MbtEEGManager eegManager;

    private ArrayList<MbtEEGPacket> mbtEEGPacketsBuffer;

    public MbtDataBuffering(MbtEEGManager eegManagerController) {

        eegManager = eegManagerController;
        hasOverflow = false;

        lostPacketInterpolatorSize = eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE) ? (2 + getRawDataPacketSize()) : 138;

        overflowBytes = new ArrayList<>();//new byte[getRawDataPacketSize()];
        pendingRawData = new ArrayList<>();//new byte[getRawDataBufferSize()];
        initLostPacketsInterpolator();
        mbtEEGPacketsBuffer = new ArrayList<>();
    }

    /**
     * Stores a part of the EEG raw data inside the pendingRawData buffer when the maximum size of the buffer is reached
     * This part begins at the [srcPos] position and the number of components stored is equal to the length argument.
     * It is stored in the part of the destination array that begins at the [bufPos] position and ends at the [bufPos+length] position.
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data the source raw EEG data array acquired by the headset and transmitted by Bluetooth to the application (data is null is packets have been lost)
     * @param srcPos the beginning position of the source EEG raw data array
     * @param length the number of components from the source array to store into the pendingRawData buffer
     * @throws IllegalArgumentException is raised if the source EEG data array is null or is empty
     * @throws IndexOutOfBoundsException is raised if the number of elements to store is bigger than the size of the source array or bigger than the size of the destination array

     */
    public void storePendingDataInBuffer(@Nullable final ArrayList<MbtRawEEG> data, final int srcPos, final int length){

        if (data == null || data.size() == 0 )
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        /*if (srcPos + length > data.size() || bufPos + length > getRawDataBufferSize()) //check that array indexes are not out of bounds
            throw new IndexOutOfBoundsException("array index exception !");
*/

        pendingRawData.addAll((data != null) ? data.subList(srcPos,srcPos+length) : lostPacketInterpolator);
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
    public void storeOverflowDataInBuffer(final ArrayList<MbtRawEEG> data, final int srcPos, final int bufPos, final int length){

        if (data == null || data.size() == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        //check that array indexes are not out of bounds
        if (srcPos + getRawDataBufferSize() - bufPos + getRawDataPacketSize() - length > data.size() // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
                || getRawDataPacketSize() - length > getRawDataBufferSize())
                throw new IndexOutOfBoundsException("array index exception !");

        overflowBytes.clear();//todo check that clearing is right
        overflowBytes.addAll(data.subList(srcPos+getRawDataBufferSize()-bufPos,data.size()));
        //System.arraycopy(data, srcPos + getRawDataBufferSize() - bufPos, overflowBytes, 0, getRawDataPacketSize() - length);
        hasOverflow = true;
    }

    /**
     * Stores the newly created EEG packet in the Packets buffer
     * We wait to have a full packet buffer to send these EEG values to the UI.
     * @return true if the packet buffer is full (contains a number of data equals to eegBufferLengthNotification), false otherwise.
     */
    public ArrayList<MbtEEGPacket> storeEegPacketInPacketBuffer(final ArrayList<ArrayList<Float>> consolidatedEEG, ArrayList<Float> status) {
        ArrayList<MbtEEGPacket> fullBufferForClient = mbtEEGPacketsBuffer;

        if(mbtEEGPacketsBuffer.size()+1 == getBufferLengthClientNotif()){ //if the packet buffer is full, we notify the client via the MbtManager and reset the MbtEEGPacket buffer
            mbtEEGPacketsBuffer.clear(); //reset the packet buffer

        }
        mbtEEGPacketsBuffer.add(new MbtEEGPacket(consolidatedEEG, /*eegManager.computeEEGSignalQuality(consolidatedEEG, getSampleRate())*/null, status, System.currentTimeMillis())); //the data is stored in the buffer
        return fullBufferForClient;
    }

    /**
     * Stores the overflow buffer in the pending buffer to handle overflow data after the pending data has been handled
     * @param length is the length of the overflow buffer to copy in the pending buffer
     */
    public void storeOverflowDataInPendingBuffer(final int length) {

        if (overflowBytes == null || overflowBytes.size() == 0 || length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE eeg data !");
        if (getRawDataPacketSize() - (getRawDataBufferSize()) > (overflowBytes.size()*getNbBytes())  || getRawDataPacketSize() - (getRawDataBufferSize()) > getRawDataBufferSize()) // if beginning position + length > array length, access to array[beginning position + length] will throw index out of bounds exception
            throw new IndexOutOfBoundsException("array index exception !");

        pendingRawData.clear();
        pendingRawData.addAll(overflowBytes.subList(0,length));

    }

    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers, status and packet size
     * @param sampleRate the sample rate
     * @param samplePerNotif the number of samples per notification
     * @param nbStatusByte the number of bytes used for one eeg data
     */
    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int nbStatusByte){ // statusByteNb parameter should be the internal config value

        MbtConfig.setSamplePerNotification(samplePerNotif);
        setNbStatusBytes(nbStatusByte);
        setRawDataPacketSize(getRawDataBytesPerWholeChannelsSamples()*samplePerNotif);
        initLostPacketsInterpolator();
        pendingRawData = new ArrayList<>(getRawDataBufferSize()); //init the buffer that we will use for handle/convert EEG raw data
        overflowBytes = new ArrayList<>(getRawDataPacketSize()); //init the overflow buffer (buffer that will be used to store overflow data, while the data from pending buffer conversion is in progress)
        statusData = (getNbStatusBytes() > 0)? statusData = new ArrayList<>(sampleRate) : null;
    }

    /**
     * handleOverflowBuffer is called when the pending data has been handled and a cloned for launching the conversion into readable EEG values
     * Replaces the pending data with the overflowing data in the pending buffer so that the overflowing data can be handled
     * @return the current buffer position
     */
    public int handleOverflowDataBuffer(){

        Log.i(TAG, "handling Overflowing Data");
        int length = overflowBytes.size();
        storeOverflowDataInPendingBuffer(length);
        hasOverflow = false; //reset overflow state
        return length; //return the buffer position
    }

    /**
     * Gets the length of the buffer that contains the EEG packets
     * a notification is sent to {@link core.MbtManager} when the converted EEG data buffer is full, so that the client can have access to the user-readable EEG data
     * @return
     */
    public int getBufferLengthClientNotif(){
        return Math.max(getEegBufferLengthClientNotif(), getRawDataBufferSize()); //todo ajouter size buffer C++
    }

    /**
     * Gets the pendingRawData array
     * @return the pending EEG raw data buffer
     */
    public ArrayList<MbtRawEEG> getPendingRawData() {
        return pendingRawData;
    }

    /**
     * Gets the overflowBytes array
     * @return the overflow EEG data buffer
     */
    public ArrayList<MbtRawEEG> getOverflowBytes() {
        return overflowBytes;
    }

    /**
     * Gets the EEG packets buffer that contains the converted user-readable EEG data
     * @return the EEG packets buffer
     */
    public ArrayList<MbtEEGPacket> getmbtEEGPacketsBuffer() {
        return mbtEEGPacketsBuffer;
    }

    /**
     * Sets a new array to the overflowBytes array
     * @param overflowBytes is the new value for overflowBytes
     */
    public void setOverflowBytes(ArrayList<MbtRawEEG> overflowBytes) {
        this.overflowBytes = overflowBytes;
    }

    /**
     * Gets the lost EEG raw data packets buffer
     * It contains only 0XFF values
     * @return the lost EEG raw data packet buffer
     */
    public ArrayList<MbtRawEEG> getLostPacketInterpolator() {
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

    /**
     * Initialize the lost packets interpolator and fills it with the 0xFF default value
     */
    private void initLostPacketsInterpolator(){
        lostPacketInterpolator = new ArrayList<>(lostPacketInterpolatorSize);
        //for(int i = 0 ; i <lostPacketInterpolatorSize ; i++){
            lostPacketInterpolator.add(new MbtRawEEG((byte) 0xFF, null));
       // }
    }
}
