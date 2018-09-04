package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.storage.RawEEGSample;
import features.MbtFeatures;
import utils.LogUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static features.MbtFeatures.DEFAULT_BLE_NB_STATUS_BYTES;
import static features.MbtFeatures.getEEGByteSize;
import static features.MbtFeatures.getNbChannels;
import static features.MbtFeatures.getNbStatusBytes;
import static features.MbtFeatures.getRawDataIndexSize;

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

    private MbtEEGManager eegManager;

    private ArrayList<RawEEGSample> singleRawEEGList;
    @Nullable
    private byte[] statusDataBytes;


    private BtProtocol protocol;

    public MbtDataAcquisition(@NonNull MbtEEGManager eegManagerController, @NonNull BtProtocol bluetoothProtocol) {
        this.protocol = bluetoothProtocol;
        this.eegManager = eegManagerController;
    }

    /**
     * Processes and converts EEG raw data acquired from the Bluetooth-connected headset
     *
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    @Nullable
    public synchronized void handleDataAcquired(@NonNull final byte[] data) {
        Log.e(TAG, " raw |"+Arrays.toString(data));//todo remove after tests

        singleRawEEGList = new ArrayList<>();

        if(data.length < getRawDataIndexSize())
            return;

        //1st step : check index
        final int currentIndex = (data[0] & 0xff) << 8 | (data[1] & 0xff); //index bytes are the 2 first bytes for BLE only

        if(previousIndex == -1){
            previousIndex = currentIndex -1;
        }

        final int indexDifference = currentIndex - previousIndex;

        //2nd step : Create interpolation packets if packet loss
        if(indexDifference != 1){
            LogUtils.e(TAG, "diff is " + indexDifference);
            for (int i = 0; i < indexDifference; i++) {
                fillSingleDataEEGList(true, data);
            }
        }

        //3rd step : chunk byte[] input into RawEEGSample objects
        statusDataBytes = (getNbStatusBytes() > 0) ? (Arrays.copyOfRange(data, getRawDataIndexSize(), getRawDataIndexSize() + getNbStatusBytes())) : null;

        fillSingleDataEEGList(false, data);

        //4th step : store and convert
        storeData();

        previousIndex = currentIndex;
    }



    /**
     * Fills the current {@link #singleRawEEGList} arraylist with new raw values from bluetooth. This method handles packet loss. In this case,
     * the {@link #singleRawEEGList} is filled with null values.
     * @param isInterpolationEEGSample whether or not the array list needs to be filled with interpolated {@link RawEEGSample} or null
     * @param input the bluetooth raw byte array.
     */
    private void fillSingleDataEEGList(boolean isInterpolationEEGSample, byte[] input){
        int count = 0;
        for (int dataIndex = getRawDataIndexSize() + getNbStatusBytes(); dataIndex < input.length; dataIndex += getEEGByteSize()*getNbChannels()) { //init the list of raw EEG data (one raw EEG data is an object that contains a 2 (or 3) bytes data array and status
            if(isInterpolationEEGSample){
                singleRawEEGList.add(RawEEGSample.LOST_PACKET_INTERPOLATOR);
            }else{
                ArrayList<byte[]> channelsEEGs = new ArrayList<>();
                for(int nbChannels = 0; nbChannels < getNbChannels(); nbChannels++){
                    byte[] bytesEEG = Arrays.copyOfRange(input, dataIndex + nbChannels*getEEGByteSize(), dataIndex + (nbChannels+1)*getEEGByteSize());
                    channelsEEGs.add(bytesEEG);
                }
                singleRawEEGList.add(new RawEEGSample(channelsEEGs, generateStatusData(count++)));
            }
        }
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
     * Fills the status data list corresponding to the EEG data array.
     * If the frames are consecutive, the status data list contains only 2 values : 0 or 1.
     * If the frames are not consecutive, it means that we have lost some EEG data packets :
     * in this case the status can't be determined so we affect the NaN value.
     * Nan is a constant holding a Not-a-Number value of type float.
     *
     */
    private Float generateStatusData(int count) {
        if (MbtFeatures.getBluetoothProtocol() == BLUETOOTH_LE && singleRawEEGList != null) {

            return statusDataBytes == null ? Float.NaN : (isBitSet(count < 8 ? statusDataBytes[0] : statusDataBytes[1], count));
        }
        return Float.NaN; //TODO handle SPP
    }


    /**
     * Stores the new EEG raw data received from Bluetooth Device in a pending buffer.
     */
    private void storeData() {
        eegManager.storePendingDataInBuffer(singleRawEEGList); //we store the data in the pending buffer

    }

    /**
     * Reset the starting and previous indexes to -1
     */
    public void resetIndex() {
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
     * getter for unit tests
     */
    public MbtEEGManager getTestEegManager() {
        return eegManager;
    }

    /**
     * getter for unit tests
     */
    public ArrayList<ArrayList<Float>> getTestEegMatrix(){
        return eegManager.getConsolidatedEEG();
    }

    public ArrayList<RawEEGSample> getTestRawEEGSample(){
        return singleRawEEGList;
    }

    public ArrayList<RawEEGSample> getTestSingleRawEEGList() {
        return singleRawEEGList;
    }

    public void setTestPreviousIndex(int previousIndex) {
        this.previousIndex = previousIndex;
    }

    public void setTestSingleRawEEGList(ArrayList<RawEEGSample> singleRawEEGList) {
        this.singleRawEEGList = singleRawEEGList;
    }

    @Nullable
    public byte[] getTestStatusDataBytes() {
        return statusDataBytes;
    }
}
