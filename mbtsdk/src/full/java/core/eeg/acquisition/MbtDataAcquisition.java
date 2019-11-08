package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.storage.RawEEGSample;
import utils.BitUtils;
import utils.ConversionUtils;
import utils.LogUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static features.MbtFeatures.DEFAULT_SAMPLE_PER_PACKET;
import static features.MbtFeatures.DEFAULT_SPP_NB_STATUS_BYTES;
import static features.MbtFeatures.getEEGByteSize;
import static features.MbtFeatures.getNbStatusBytes;
import static features.MbtFeatures.getRawDataIndexSize;
import static features.MbtFeatures.getSamplePerNotification;

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

    public MbtDataAcquisition(@NonNull MbtEEGManager eegManager, @NonNull BtProtocol bluetoothProtocol) {
        this.protocol = bluetoothProtocol;
        this.eegManager = eegManager;
    }

    /**
     * Processes and converts EEG raw data acquired from the Bluetooth-connected headset
     *
     * @param data the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    @Nullable
    public synchronized void handleDataAcquired(@NonNull final byte[] data, int nbChannels) {
        singleRawEEGList = new ArrayList<>();

        if(data.length < getRawDataIndexSize(protocol))
            return;

        //1st step : check index
        final int currentIndex = (data[protocol == BLUETOOTH_LE ? 0 : 1] & 0xff) << 8 | (data[protocol == BLUETOOTH_LE ? 1 : 2] & 0xff); //index bytes are the 2 first bytes for BLE only

        if(previousIndex == -1){
            previousIndex = currentIndex -1;
        }

        final int indexDifference = currentIndex - previousIndex;

        //2nd step : Create interpolation packets if packet loss
        if(indexDifference != 1){
            LogUtils.e(TAG, "diff is " + indexDifference +" . Current index : " + currentIndex + " previousIndex : " + previousIndex);
            for (int i = 0; i < indexDifference; i++) {
                fillSingleDataEEGList(nbChannels, true, data);
            }
        }

        //3rd step : chunk byte[] input into RawEEGSample objects
        fillSingleDataEEGList(nbChannels, false, data);

        //4th step : store
        storeData();

        previousIndex = currentIndex;
    }


    /**
     * Fills the current {@link #singleRawEEGList} arraylist with new raw values from bluetooth. This method handles packet loss. In this case,
     * the {@link #singleRawEEGList} is filled with null values.
     * @param isInterpolationEEGSample whether or not the array list needs to be filled with interpolated {@link RawEEGSample} or null
     * @param input the bluetooth raw byte array.
     */
    private void fillSingleDataEEGList(int numberOfChannels, boolean isInterpolationEEGSample, byte[] input) {
        if (protocol.equals(BLUETOOTH_LE)){
            statusDataBytes = (getNbStatusBytes(protocol) > 0) ? (Arrays.copyOfRange(input, getRawDataIndexSize(protocol), getRawDataIndexSize(protocol) + getNbStatusBytes(protocol))) : null;
            fillBLESingleDataEEGList(numberOfChannels, isInterpolationEEGSample, input);
        }else if (protocol.equals(BLUETOOTH_SPP))
                fillSPPSingleDataEEGList(numberOfChannels, isInterpolationEEGSample, input);
        }

    private void fillSPPSingleDataEEGList(int numberOfChannels, boolean isInterpolationEEGSample, byte[] input){
        int count = 0;

        for (int dataIndex = getRawDataIndexSize(protocol);
             dataIndex < input.length;
             dataIndex += (getNbStatusBytes(protocol) + getEEGByteSize(protocol)*numberOfChannels)) { //init the list of raw EEG data (one raw EEG data is an object that contains a 2 (or 3) bytes data array and status

            if(getNbStatusBytes(protocol) > 0){
                if(statusDataBytes == null)
                    statusDataBytes = new byte[DEFAULT_SPP_NB_STATUS_BYTES * DEFAULT_SAMPLE_PER_PACKET];

                statusDataBytes = (Arrays.copyOfRange(input,
                        dataIndex,
                        dataIndex + getNbStatusBytes(protocol)));
            }

            if(isInterpolationEEGSample){
                singleRawEEGList.add(RawEEGSample.LOST_PACKET_INTERPOLATOR);
            }else{
                ArrayList<byte[]> channelsEEGs = new ArrayList<>();
                for(int channel = 0; channel < numberOfChannels; channel++){
                    byte[] bytesEEG = Arrays.copyOfRange(input,
                            dataIndex + getNbStatusBytes(protocol) + channel * getEEGByteSize(protocol),
                            dataIndex + getNbStatusBytes(protocol) + (channel+1) * getEEGByteSize(protocol));
                    channelsEEGs.add(bytesEEG);
                }
                singleRawEEGList.add(new RawEEGSample(channelsEEGs, generateSPPStatusData(count++)));
            }
        }
    }

    private void fillBLESingleDataEEGList(int numberOfChannels, boolean isInterpolationEEGSample, byte[] input){
        for (int dataIndex = getRawDataIndexSize(protocol) + getNbStatusBytes(protocol); dataIndex < input.length; dataIndex += getEEGByteSize(protocol)*numberOfChannels) { //init the list of raw EEG data (one raw EEG data is an object that contains a 2 (or 3) bytes data array and status
            if(isInterpolationEEGSample){
                singleRawEEGList.add(RawEEGSample.LOST_PACKET_INTERPOLATOR);
            }else{
                LinkedList<Float> statuses = generateBLEStatusData();
                ArrayList<byte[]> channelsEEGs = new ArrayList<>();
                for(int nbChannels = 0; nbChannels < numberOfChannels; nbChannels++){
                    byte[] bytesEEG = Arrays.copyOfRange(input, dataIndex + nbChannels*getEEGByteSize(protocol), dataIndex + (nbChannels+1)*getEEGByteSize(protocol));
                    channelsEEGs.add(bytesEEG);
                }
                if(statuses.getFirst() == null)
                    singleRawEEGList.add(RawEEGSample.LOST_PACKET_INTERPOLATOR);
                else
                    singleRawEEGList.add(new RawEEGSample(channelsEEGs, statuses.poll()));
            }
        }
    }

    /**
     * Fills the status data list corresponding to the EEG data array.
     * If the frames are consecutive, the status data list contains only 2 values : 0 or 1.
     * If the frames are not consecutive, it means that we have lost some EEG data packets :
     * in this case the status can't be determined so we affect the NaN value.
     * Nan is a constant holding a Not-a-Number value of type float.
     *
     */
    private LinkedList<Float> generateBLEStatusData() {
        // A status sample is encoded on 1 byte
        final int BYTE_SIZE = 8;

        LinkedList<Float> deserializedStatusData = new LinkedList<>();

        if(statusDataBytes == null) {
            for(int statusByte = 0; statusByte < getNbStatusBytes(protocol); statusByte++){
                deserializedStatusData.add(Float.NaN);
            }
            return deserializedStatusData;
        }


        // One status is retrieved from several chunks / status samples from a raw bluetooth packet, where the number of status samples match the number of EEG samples
        for(int statusByte = 0; statusByte < statusDataBytes.length; statusByte++){
            byte status = statusDataBytes[statusByte];

            //each byte contains 8 bits and each status sample matches the number of eeg sample
            //As one byte is 8 bits, 2 bytes are sent in the bluetooth notification
            for(int bit = 0;
                bit < ((getSamplePerNotification() - statusByte*BYTE_SIZE) < BYTE_SIZE ?
                        (getSamplePerNotification() - statusByte*BYTE_SIZE) : BYTE_SIZE);
                bit++){

                deserializedStatusData.add(BitUtils.isBitSet(status, bit));
            }
        }
        return deserializedStatusData;
    }

    /**
     * Fills the status data list corresponding to the EEG data array.
     * If the frames are consecutive, the status data list contains only 2 values : 0 or 1.
     * If the frames are not consecutive, it means that we have lost some EEG data packets :
     * in this case the status can't be determined so we affect the NaN value.
     * Nan is a constant holding a Not-a-Number value of type float.
     *
     */
    private Float generateSPPStatusData(int count) {
        if (singleRawEEGList != null) {

            return statusDataBytes == null ?
                    Float.NaN :
                    (ConversionUtils.booleanToFloat(
                            BitUtils.isBitSet(count < 8 ? // return 1f to fill the status data array if the bit is set, otherwise returns 0f
                                            statusDataBytes[0] : statusDataBytes[1],
                                    (byte) 1, count)));
        }
        statusDataBytes = null;
        return Float.NaN;
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
