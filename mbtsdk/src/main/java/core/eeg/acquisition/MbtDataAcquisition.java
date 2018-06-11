package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.storage.MbtRawEEG;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static features.MbtFeatures.getNbBytes;
import static features.MbtFeatures.getNbStatusBytes;
import static features.MbtFeatures.getRawDataBufferSize;
import static features.MbtFeatures.getRawDataIndexSize;
import static features.MbtFeatures.getRawDataPacketSize;
import static features.MbtFeatures.getSamplePerNotification;
import static features.MbtFeatures.getSampleRate;


/**
 * MbtDataAcquisition is responsible for managing incoming EEG data acquired by the MBT headset and transmitted through Bluetooth communication to the application.
 *
 * @author Manon LETERME on 09/08/2016.
 * @version 2.0 Sophie ZECRI on 25/05/2018.
 */

public class MbtDataAcquisition {

    private final String TAG = MbtDataAcquisition.class.getName();

    private int startingIndex = -1;
    private int previousIndex = -1;

    private int rawDataPosition = 0;
    private int bufferPosition = 0;

    private MbtEEGManager eegManager;

    private ArrayList<MbtRawEEG> singleRawEEGList;
    private byte[] statusDataBytes;
    private static ArrayList<Float> statusData;


    public MbtDataAcquisition(MbtEEGManager eegManagerController) {

        this.eegManager = eegManagerController;
        statusData = (getNbStatusBytes() > 0) ? new ArrayList<Float>(getSampleRate()) : null;
    }

    /**
     * Processes and converts EEG raw data acquired from the Bluetooth-connected headset
     *
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public synchronized ArrayList<ArrayList<Float>> handleDataAcquired(@NonNull final byte[] data) {
        Log.d(TAG,"data acquired: "+Arrays.toString(data));

        //store the status in the statusDataBytes array and the EEG data in a list of MbtrawEEG
        statusDataBytes = (getNbStatusBytes() > 0) ? (Arrays.copyOfRange(data, getRawDataIndexSize(), getRawDataIndexSize() + getNbStatusBytes())) : null;
        singleRawEEGList = new ArrayList<>();

        for (int dataIndex = getRawDataIndexSize() + getNbStatusBytes(); dataIndex < data.length; dataIndex += getNbBytes()) { //init the list of raw EEG data (one raw EEG data is an object that contains a 2 (or 3) bytes data array and status
            byte[] bytesEEG = new byte[getNbBytes()];
            for (int i = 0; i < getNbBytes() ; i++){
                bytesEEG[i] = data[dataIndex+i];
            }
            singleRawEEGList.add(new MbtRawEEG(bytesEEG, null));
        }

        final int currentIndex = (previousIndex > 0) ? previousIndex + 1 : getBluetoothProtocol().equals(BLUETOOTH_LE) ? (singleRawEEGList.get(0).getBytesEEG()[0] & 0xff) << 8 | (singleRawEEGList.get(0).getBytesEEG()[1] & 0xff) : (singleRawEEGList.get(0).getBytesEEG()[1] & 0xff) << 8 | (singleRawEEGList.get(0).getBytesEEG()[2] & 0xff);
        if (previousIndex == -1) //taking care of the first index
            previousIndex = currentIndex - 1;
        final int indexDifference = currentIndex - previousIndex;
        if (indexDifference != 1)
            Log.e(TAG, "diff is " + indexDifference);

        for (int i = 0; i < indexDifference; i++) {
            handleAndConvertData(indexDifference, i);
        }
        previousIndex = currentIndex;
        return eegManager.getConsolidatedEEG();
    }

    /**
     * Returns 1 if the bit is set, otherwise returns 0
     * to fill the status data array
     *
     * @param tempStatus is the current byte to set
     * @param bit        is the index of the current byte
     * @return 1 to fill the status data array if the bit is set, otherwise returns 0
     */
    private static Float isBitSet(byte tempStatus, int bit) {
        return ((tempStatus & (1 << bit)) != 0) ? 1f : 0f;
    }

    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Resets the buffers, status, NbStatusBytes and rawDataPacketSize
     *
     * @param sampleRate     the sample rate
     * @param samplePerNotif the number of sample per notification
     * @param nbStatusByte   the number of bytes used for one eeg data
     */
    public void reconfigureBuffers(final int sampleRate, final byte samplePerNotif, final byte nbStatusByte) {
        eegManager.reconfigureBuffers(sampleRate, samplePerNotif, nbStatusByte);
        resetIndex();
        bufferPosition = 0;
    }

    /**
     * Fills the status data list corresponding to the EEG data array.
     * If the frames are consecutive, the status data list contains only 2 values : 0 or 1.
     * If the frames are not consecutive, it means that we have lost some EEG data packets :
     * in this case the status can't be determined so we affect the NaN value.
     * Nan is a constant holding a Not-a-Number value of type float.
     *
     * @param isConsecutive is true if received frames are consecutive (no lost data packets), false otherwise
     */
    private void generateStatusData(final boolean isConsecutive) {

        ArrayList<Float> currentStatusList = new ArrayList<>(); //current status for 1 EEG data (composed of 2 or 3 status)
        if (getBluetoothProtocol().equals(BLUETOOTH_LE) && singleRawEEGList != null) {
            for (int i = 0; i < getNbStatusBytes(); i++) { //for each status byte (0 BLE default or 3 bytes SPP)
                if (getSamplePerNotification() - i * 8 > 0) {
                    byte currentStatus = statusDataBytes[i]; //status bytes have been stored in the statusDataBytes array at the beginning / when data are acquired
                    for (int currentBit = 0; currentBit < ((getSamplePerNotification() - i * 8 < 8) ? (getSamplePerNotification() - i * 8) : 8); currentBit++) {
                        currentStatusList.add((isConsecutive) ? isBitSet(currentStatus, currentBit) : Float.NaN);
                        statusData.add((isConsecutive) ? isBitSet(currentStatus, currentBit) : Float.NaN);
                    }
                }
            }
        } else
            currentStatusList.add(null);

        for (MbtRawEEG singleRawEEG : singleRawEEGList) {
            if (singleRawEEG.getStatus() == null)
                singleRawEEG.setStatus(currentStatusList);
        }
    }

    /**

     * Lost EEG data packets array contains only the value 0XFF.
     * This array is used to know exactly which packets have been lost during the acquisition
     * This way, a 0XFF value in the pending data buffer can be consider as a lost EEG data packet
     */

    /**
     * Stores EEG raw data received from Bluetooth Device in the pending buffer.
     * The lost EEG raw data packets are identified in the pending buffer .
     * In case packet size is too large for buffer, the overflowing EEG data is stored in an overflow buffer
     *
     * @param isConsecutive is true if received frames are consecutive (no lost data packets), false otherwise
     */
    private void storeEEGDataInBuffers(boolean isConsecutive) {
        Log.i(TAG, "storing EEG data In Buffers");
        int lengthToStore = getRawDataPacketSize();
        //In case packet size is too large for buffer, we only take first part in pending buffer. Second part is stored in overflow buffer for future use
        if (bufferPosition + getRawDataPacketSize() > getRawDataBufferSize()) { // check that the maximum size is not reached if we can add a packet to the buffer
            setOverflow(true); //notify the eeg manager that the buffer is full
            lengthToStore = getRawDataBufferSize() - bufferPosition;
            eegManager.storeOverflowDataInBuffer(singleRawEEGList, rawDataPosition, bufferPosition, lengthToStore);//we store the overflowing part in the overflow buffer
        }
        eegManager.storePendingDataInBuffer(isConsecutive ? singleRawEEGList : null, isConsecutive ? rawDataPosition : 0, lengthToStore); //we store the pending buffer in both case (overflow or no overflow)
    }

    /**
     * Handles the pending data buffer when this buffer is full of EEG data :
     * generates the status data corresponding to the EEG data,
     * stores overflow data in the pending buffer
     * and finally launches the conversion from raw EEG data into readable EEG values
     */
    private void handleFullPendingData() {
        Log.i(TAG, "handling Pending data buffer");
        handleOverflowStatus();
        final ArrayList<MbtRawEEG> rawEEGtoConvert = (ArrayList<MbtRawEEG>) getPendingRawData().clone(); //the pending raw data is stored in toDecodeBytes to be converted in readable EEG values
        bufferPosition = (hasOverflow()) ? eegManager.handleOverflowDataBuffer() : 0; //handleOverflowBuffer return overflow buffer size
        eegManager.convertToEEG(rawEEGtoConvert, statusData);
    }

    /**
     * * @param indexDifference determines if the frames are consecutive or not
     *
     * @param i the current index difference while looping on all the index difference
     */
    private void handleAndConvertData(final int indexDifference, final int i) {
        handleConsecutiveOrNonConsecutiveFrame((byte) (indexDifference - i - 1) == 0); //if received frames are consecutive we store the eeg data in a pending buffer, if received frames are not consecutive, we store the eeg data in a lost packet buffer
        bufferPosition += getRawDataPacketSize();
        if (hasOverflow())  //the input eeg buffer is full ...
            handleFullPendingData(); // ... conversion to user-readable EEG values can be launched
    }

    /**
     * Handles consecutive or non consecutive frame :
     * stores the EEG raw data in specific buffers according to their nature/kind
     * If the frames are consecutive, the received EEG data are stored into the pending buffer
     * If the frames are not consecutive, instead of storing the received EEG data, we store an array filled with 0XFF value that has the size of the missing lost EEG data packet.
     *
     * @param isConsecutive is true if received frames are consecutive (no lost data packets), false otherwise
     */
    private void handleConsecutiveOrNonConsecutiveFrame(final boolean isConsecutive) {
        if (statusData != null) //status is initialized if nbStatusBytes > 0
            generateStatusData(isConsecutive);
        storeEEGDataInBuffers(isConsecutive);
    }

    /**
     * Fills the toDecodeStatus array with the statusData values
     * If the statusData list size is bigger than the sampleRate, it means that we have overflow status.
     * In this case, the overflowing status data are restored into the statusData list.
     *
     * @return the filled toDecodeStatus list
     */
    private void handleOverflowStatus() {
        ArrayList<Float> toDecodeStatus = statusData;
        if(toDecodeStatus!=null){
            int size = toDecodeStatus.size();

            if (getBluetoothProtocol().equals(BLUETOOTH_LE) && getNbStatusBytes() > 0) {
                statusData = new ArrayList<Float>(); //reset the statusdata list
                if (toDecodeStatus != null && size > getSampleRate()) {//if status overflow, we transfer overflow value status from rawEEG status to statusData list
                    for (int i = getSampleRate(); i < size; i++) {
                        statusData.add(toDecodeStatus.get(i)); //re add overflow status inside statusData
                    }
                    for (int i = 0; i < size - getSampleRate(); i++) {
                        toDecodeStatus.remove(toDecodeStatus.size() - 1); //remove overflow status in toDecodeStatus (remove the last (size - sampleRate) elements of the list)
                    }
                }
            }
        }
    }

    /**
     * Gets the Bluetooth protocol used to transmit data from the headset to the application
     *
     * @return the Bluetooth protocol used to transmit data from the headset to the application
     */
    private BtProtocol getBluetoothProtocol() {
        return getMbtManager().getBluetoothProtocol();
    }

    /**
     * Reset the starting and previous indexes to -1
     */
    private void resetIndex() {
        startingIndex = -1;
        previousIndex = -1;
    }

    /**
     * Get the starting index for scanning the raw EEG data array
     *
     * @return the starting index
     */
    public int getStartingIndex() {
        return startingIndex;
    }

    /**
     * Get the previous index for scanning the raw EEG data array
     *
     * @return the previous index
     */
    public int getPreviousIndex() {
        return previousIndex;
    }

    /**
     * Get the starting position of the buffer array to store
     *
     * @return the starting position of the buffer array to store
     */
    public int getbufferPosition() {
        return bufferPosition;
    }

    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     *
     * @return the MbtManager instance.
     */
    private MbtManager getMbtManager() {
        return eegManager.getMbtManager();
    }

    /**
     * Gets the user-readable EEG data matrix
     *
     * @return the converted EEG data matrix that contains readable values for any user
     */
    private ArrayList<ArrayList<Float>> getConsolidatedEEG() {
        return eegManager.getConsolidatedEEG();
    }

    /**
     * Get the boolean value of hasOverflow
     * Return true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Return false if the pending data buffer size is lower than its total capacity
     *
     * @return the boolean value of the overflow state
     */
    private boolean hasOverflow() {
        return eegManager.hasOverflow();
    }

    /**
     * Set a boolean value to hasOverflow
     * Set true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Set false if the pending data buffer size is lower than its total capacity
     *
     * @param hasOverflow the boolean value of the overflow state
     */
    private void setOverflow(boolean hasOverflow) {
        eegManager.setOverflow(hasOverflow);
    }

    /**
     * Get the pending EEG raw data buffer to convert
     *
     * @return an array containing the pending EEG raw data to convert
     */
    private ArrayList<MbtRawEEG> getPendingRawData() {
        return eegManager.getPendingRawData();
    }

    /**
     * Get the lost packets of EEG raw data buffer to convert
     *
     * @return an array containing the lost packets of EEG raw data buffer to convert
     */
    private ArrayList<MbtRawEEG> getLostPacketInterpolator() {
        return eegManager.getLostPacketInterpolator();
    }
}
