package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import features.MbtFeatures;
import utils.AsyncUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static core.eeg.MbtEEGManager.getSamplePerPacket;


/**
 * MbtDataAcquisition is responsible for managing incoming EEG data acquired by the MBT headset and transmitted through Bluetooth communication to the application.
 *
 * @author Manon LETERME on 09/08/2016.
 * @version Sophie ZECRI on 25/05/2018.
 */

public class MbtDataAcquisition {

    private final String TAG = MbtDataAcquisition.class.getName();

    private static int startingIndex = -1;
    private static int previousIndex = -1;

    private static int rawDataPosition = 0;
    private static int bufferPosition = 0;

    private final int sampleRate;

    private MbtEEGManager eegManager;

    private static ArrayList<Float> statusData;

    public MbtDataAcquisition(MbtEEGManager eegManagerController) {

        this.eegManager = eegManagerController;
        this.sampleRate = MbtFeatures.getSampleRate();
        rawDataPosition = getRawDataIndexSize();

        if (getBluetoothProtocol().equals(BLUETOOTH_LE)) {
            setNbStatusBytes(MbtFeatures.getStatusSize());
            setRawDataPacketSize(getRawDataBytesPerWholeChannelsSamples() * getSamplePerPacket());
            statusData = (getNbStatusBytes() > 0)? new ArrayList<Float>(sampleRate) : null;//default nbStatusBytes=3 for SPP and default nbStatusBytes=0 for BLE
        }
    }

    /**
     * Processes and converts EEG raw data acquired from the Bluetooth-connected headset
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public synchronized void handleDataAcquired(@NonNull final byte[] data) {
        if ( !getBluetoothProtocol().equals(BLUETOOTH_LE) && !getBluetoothProtocol().equals(BLUETOOTH_SPP))
            return; // we don't receive any eeg data if any of these protocols are used
        final int currentIndex = (previousIndex>0)? previousIndex+1 : getBluetoothProtocol().equals(BLUETOOTH_LE)? (data[0] & 0xff) << 8 | (data[1] & 0xff) : (data[1] & 0xff) << 8 | (data[2] & 0xff); // masks the variable contained in data[0] & data[1] (or data[1] & data[2]) so it leaves only the value in the last 8 bits, and ignores all the rest of the bits.
        if (previousIndex == -1) //taking care of the first index
            previousIndex = currentIndex - 1;
        final int indexDifference = currentIndex - previousIndex;
        if (indexDifference != 1)
            Log.e(TAG, "diff is " + indexDifference);
        for (int i = 0; i < indexDifference; i++) {
            handleConsecutiveOrNonConsecutiveFrame(data, (byte) (indexDifference - i - 1) == 0); //if received frames are consecutive we store the eeg data in a pending buffer, if received frames are not consecutive, we store the eeg data in a lost packet buffer
            bufferPosition += getRawDataPacketSize();
            if (bufferPosition >= getRawDataBufferSize())  //the input eeg buffer is full ...
                handleFullPendingData(); // ... conversion to user-readable EEG values can be launched
        }
        previousIndex = currentIndex;
    }

    /**
     * Returns 1 if the bit is set, otherwise returns 0
     * to fill the status data array
     * @param b is the current byte to set
     * @param bit is the index of the current byte
     * @return 1 to fill the status data array if the bit is set, otherwise returns 0
     */
    public static Float isBitSet(byte b, int bit) {
        if ((b & (1 << bit)) != 0)
            return 1f;
        else
            return 0f;
    }

    /**
     * Resets the buffers, status, NbStatusBytes and rawDataPacketSize
     * @param sampleRate the sample rate
     * @param samplePerNotif the number of sample per notification
     * @param nbStatusByte the number of bytes used for one eeg data
     */
    void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int nbStatusByte) {
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
     * @param isConsecutive is true if received frames are consecutive (no lost data packets), false otherwise
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    private void generateStatusData(boolean isConsecutive, final byte[] data) {
        if (getBluetoothProtocol().equals(BLUETOOTH_LE)) {
            for (int j = 0; j < getNbStatusBytes(); j++) {
                byte tempStatus = data[getRawDataIndexSize() + j];
                for (int k = 0; k < (getSamplePerPacket() - j * 8 < 8 ? getSamplePerPacket() - j * 8 : 8); k++) {
                    if (statusData != null)
                        statusData.add((isConsecutive)? isBitSet(tempStatus, k) : Float.NaN); //NaN is a constant holding a Not-a-Number value of type float.
                }
            }
        }
    }

    /**
     * Stores the lost EEG raw data packets in the pending data buffer so that missing data are identified.
     * Lost EEG data packets array contains only the value 0XFF.
     * This array is used to know exactly which packets have been lost during the acquisition
     * This way, a 0XFF value in the pending data buffer can be consider as a lost EEG data packet
     */
    private void storeEEGDataLostPacket() {
        Log.i(TAG, "storing lost EEG data");
        eegManager.storePendingDataInBuffer(getLostPacketInterpolator(),0, bufferPosition, ((bufferPosition +getRawDataPacketSize())>getRawDataBufferSize()) ? getRawDataBufferSize()-bufferPosition : getRawDataPacketSize());
    }

    /**
     * Stores EEG raw data received from Bluetooth Device in the pending buffer.
     * In case packet size is too large for buffer, the overflowing EEG data is stored in an overflow buffer
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    private void storeEEGDataInBuffers(final byte[] data){
        Log.i(TAG, "storing EEG data In Buffers");

        int pendingBufferLength = getRawDataPacketSize();//if we have no overflow, the pending buffer has the same size as the buffer
        //In case packet size is too large for buffer, we only take first part in pending buffer. Second part is stored in overflow buffer for future use
        if (bufferPosition + getRawDataPacketSize() > getRawDataBufferSize()) { // check that the maximum size is not reached if we can add a packet to the buffer
            setOverflow(true); //notify the eeg manager that the buffer is full
            pendingBufferLength = getRawDataBufferSize() - bufferPosition;
            eegManager.storeOverflowDataInBuffer(data, rawDataPosition, bufferPosition, pendingBufferLength);//we store the overflowing part in the overflow buffer
        }
        eegManager.storePendingDataInBuffer(data, rawDataPosition, bufferPosition, pendingBufferLength); //we store the pending buffer in both case (overflow or no overflow)
    }

    /**
     * Handles the pending data buffer when this buffer is full of EEG data :
     * generates the status data corresponding to the EEG data,
     * stores overflow data in the pending buffer
     * and finally launches the conversion from raw EEG data into readable EEG values
     */
    private void handleFullPendingData(){
        Log.i(TAG, "handling Pending data buffer");
        final ArrayList<Float> toDecodeStatus = generateToDecodeStatus();
        final byte[] toDecodeBytes = getPendingRawData().clone(); //the pending raw data is stored in toDecodeBytes to be converted in readable EEG values
        bufferPosition = (hasOverflow())? eegManager.handleOverflowDataBuffer() : 0; //handleOverflowBuffer return rawDataPacketSize/2
        prepareAndConvertToEEG(toDecodeBytes, toDecodeStatus);
    }

    /**
     * Handles consecutive or non consecutive frame :
     * stores the EEG raw data in specific buffers according to their nature/kind
     * If the frames are consecutive, the received EEG data are stored into the pending buffer
     * If the frames are not consecutive, instead of storing the received EEG data, we store an array filled with 0XFF value that has the size of the missing lost EEG data packet.
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param isConsecutive is true if received frames are consecutive (no lost data packets), false otherwise
     */
    private void handleConsecutiveOrNonConsecutiveFrame(final byte[] data, boolean isConsecutive) {
        generateStatusData(isConsecutive, data);
        if ((isConsecutive))
            storeEEGDataInBuffers(data);
         else
            storeEEGDataLostPacket();
    }

    /**
     * Fills the toDecodeStatus array with the statusData values
     * If the statusData list size is bigger than the sampleRate, it means that we have overflow status.
     * In this case, the overflowing status data are restored into the statusData list.
     * @return the filled toDecodeStatus list
     */
    private ArrayList<Float> generateToDecodeStatus(){
        ArrayList<Float> toDecodeStatus = statusData;
        if(getBluetoothProtocol().equals(BLUETOOTH_LE)){
            if(getNbStatusBytes() > 0){
                statusData = new ArrayList<>(); //reinit status raw container
                if(toDecodeStatus != null){
                    if(toDecodeStatus.size() > sampleRate){ //transfer overflow value status from toDecodeStatus to statusData
                        int size = toDecodeStatus.size();
                        for(int i = sampleRate; i < size; i++){
                            statusData.add(toDecodeStatus.get(i)); //re add overflow status inside statusData
                        }
                        for(int i = 0; i < size - sampleRate ; i++){
                            toDecodeStatus.remove(toDecodeStatus.size()-1); //remove overflow status in toDecodeStatus
                        }
                    }
                }
            }
        }
        return toDecodeStatus;
    }

    /**
     * Convert the raw EEG data array into a readable EEG data matrix of float values
     * and notify that EEG data is ready to the User Interface
     * @param toDecodeBytes the EEG raw data array to convert
     * @param toDecodeStatus the status data corresponding to the EEG data array
     */
    private void prepareAndConvertToEEG(final byte[] toDecodeBytes, final ArrayList<Float> toDecodeStatus ){
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "computing and sending to application");

                eegManager.launchConversionToEEG(toDecodeBytes); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

                ArrayList<Float> status = new ArrayList<>();
                switch(getBluetoothProtocol()){
                    case BLUETOOTH_LE:
                        status = toDecodeStatus;
                        break;
                    case BLUETOOTH_SPP:
                        status = getEegResult().get(0); //the first list of the matrix is the status
                        getEegResult().remove(0); //remove the first element of the EEG matrix
                        break;
                }
                eegManager.notifyEEGDataIsReady(status, sampleRate, MbtFeatures.getNbChannels());//notify UI that eeg data are ready
            }
        });
    }

    /**
     * Gets the Bluetooth protocol used to transmit data from the headset to the application
     * @return the Bluetooth protocol used to transmit data from the headset to the application
     */
    private BtProtocol getBluetoothProtocol(){
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
     * @return the starting index
     */
    public static int getStartingIndex() {
        return startingIndex;
    }

    /**
     * Get the previous index for scanning the raw EEG data array
     * @return the previous index
     */
    public static int getPreviousIndex() {
        return previousIndex;
    }

    /**
     * Get the starting position of the buffer array to store
     * @return the starting position of the buffer array to store
     */
    public static int getbufferPosition() {
        return bufferPosition;
    }

    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     * @return the MbtManager instance.
     */
    private MbtManager getMbtManager(){
        return eegManager.getMbtManager();
    }

    /**
     * Gets the number of bytes corresponding to one EEG data
     * @return the number of bytes corresponding to one EEG data
     */
    private int getNbStatusBytes(){
        return eegManager.getNbStatusBytes();
    }

    /**
     * Gets the raw data buffer size
     * @return the raw data buffer size
     */
    private int getRawDataBufferSize(){
        return eegManager.getRawDataBufferSize();
    }

    /**
     * Gets the raw data packet size
     * @return the raw data packet size
     */
    private int getRawDataPacketSize(){
        return eegManager.getRawDataPacketSize();
    }

    /**
     * Gets the raw data index size
     * @return the raw data index size
     */
    private int getRawDataIndexSize(){
        return eegManager.getRawDataIndexSize();
    }

    /**
     * Gets the number of bytes of a EEG raw data per whole channels samples
     * @return the number of bytes of a EEG raw data per whole channels samples
     */
    private int getRawDataBytesPerWholeChannelsSamples(){
        return eegManager.getRawDataBytesPerWholeChannelsSamples();
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the converted EEG data matrix that contains readable values for any user
     */
    private ArrayList<ArrayList<Float>> getEegResult(){
        return eegManager.getEegResult();
    }

    /**
     * Get the boolean value of hasOverflow
     * Return true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Return false if the pending data buffer size is lower than its total capacity
     * @return the boolean value of the overflow state
     */
    private boolean hasOverflow(){
        return eegManager.hasOverflow();
    }

    /**
     * Set a boolean value to hasOverflow
     * Set true if the pending data buffer is full of EEG data
     * The pending data buffer will be full if the incoming EEG data array size is bigger than the EEG raw data pending buffer size
     * Set false if the pending data buffer size is lower than its total capacity
     * @param hasOverflow the boolean value of the overflow state
     */
    private void setOverflow(boolean hasOverflow){
        eegManager.setOverflow(hasOverflow);
    }

    /**
     * Get the pending EEG raw data buffer to convert
     * @return an array containing the pending EEG raw data to convert
     */
    private byte[] getPendingRawData(){
        return eegManager.getPendingRawData();
    }

    /**
     * Get the lost packets of EEG raw data buffer to convert
     * @return an array containing the lost packets of EEG raw data buffer to convert
     */
    private byte[] getLostPacketInterpolator(){
        return eegManager.getLostPacketInterpolator();
    }

    /**
     * Sets a value to the number of bytes for status data
     * @param nbStatusBytes the number of bytes for status data
     */
    private void setNbStatusBytes(int nbStatusBytes){
        eegManager.setNbStatusBytes(nbStatusBytes);
    }

    /**
     * Sets a value to the raw data packet size
     * @param rawDataPacketSize the raw data packet size
     */
    private void setRawDataPacketSize(int rawDataPacketSize){
        eegManager.setRawDataPacketSize(rawDataPacketSize);
    }

}