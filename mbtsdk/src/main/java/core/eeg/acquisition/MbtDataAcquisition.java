package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import features.MbtFeatures;
import utils.AsyncUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static features.MbtFeatures.getSamplePerNotif;

/**
 * MbtDataAcquisition.java
 * mybraintech.com.mbtsdk.core
 *
 *  Created by Manon LETERME on 09/08/2016
 *  Copyright (c) 2016 myBrain Technologies. All rights reserved.
 *  Update : ---
 */

public class MbtDataAcquisition {

    private final String TAG = MbtDataAcquisition.class.getName();

    // EEG Data & Values
    private static int startingIndex = -1;
    private static int previousIndex = -1;

    private static int srcPos = 0;
    private static int bufPos = 0;

    private final int sampleRate;
    private final int nbChannels;

    private MbtEEGManager eegManager;
    // EventBus : MbtDataAccquisition is
    // (acquiredata) the subscriber for MbtBluetoothManager and will be notified for converting raw data
    // (EEGDataIsReady) the publisher and MbtBluetoothManager is the subscriber that will be notified when a new EEG data is ready (raw byte table has been converted to Float matrix)

    private static ArrayList<Float> statusData;

    public MbtDataAcquisition(MbtEEGManager eegManagerController) {

        this.eegManager = eegManagerController;
        eegManager.setSamplePerNotif(getSamplePerNotif());
        this.nbChannels = MbtFeatures.getNbChannels();
        this.sampleRate = MbtFeatures.getSampleRate();
        srcPos = eegManager.getRawDataIndexSize();

        if (eegManager.getMbtManager().getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE)) {
            eegManager.setNbStatusBytes(MbtFeatures.getStatusSize());
            eegManager.setRawDataPacketSize(eegManager.getRawDataBytesPerWholeChannelsSamples() * getSamplePerNotif());
            if (eegManager.getNbStatusBytes() > 0) //default nbStatusBytes=3 for SPP and default nbStatusBytes=0 for BLE
                statusData = new ArrayList<>(sampleRate);
            else
                statusData = null;
        }
    }

    /**
     * Process and convert EEG raw data acquired from Bluetooth device
     * @param data the raw eeg data array received
     */
    public synchronized void handleDataAcquired(@NonNull final byte[] data) {
        if (!eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE) && !eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_SPP))
            return; // we don't receive any eeg data if any of these protocols are used
        final int currentIndex = (previousIndex>0)? previousIndex+1 : (eegManager.getMbtManager().getMbtBluetoothManager().getBtProtocol()).equals(BtProtocol.BLUETOOTH_LE)? (data[0] & 0xff) << 8 | (data[1] & 0xff) : (data[1] & 0xff) << 8 | (data[2] & 0xff);
        // “& 0xff” masks the variable contained in data[0] & data[1] (or data[1] & data[2]) so it leaves only the value in the last 8 bits, and ignores all the rest of the bits.
        //taking care of the first index
        if (previousIndex == -1)
            previousIndex = currentIndex - 1;

        final int indexDifference = currentIndex - previousIndex;
        if (indexDifference != 1)
            Log.e(TAG, "diff is " + indexDifference);

        for (int i = 0; i < indexDifference; i++) {
            handleConsecutiveOrNonConsecutiveFrame(data, (byte) (indexDifference - i - 1) == 0); //if received frames are consecutive we store the eeg data in a pending buffer, if received frames are not consecutive, we store the eeg data in a lost packet buffer
            bufPos += eegManager.getRawDataPacketSize();

            if (bufPos >= eegManager.getRawDataBufferSize()) { //the input eeg buffer is full so it contains enough data to compute a new MBTEEGPacket
                handleFullPendingData();
            }
        }
        previousIndex = currentIndex;
    }

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
        bufPos = 0;
    }

    /**
     * Generates the status data
     * @param isConsecutive is true if received frames are consecutive, false otherwise
     * @param data EEG raw data array received from Bluetooth device
     */
    private void generateStatusData(boolean isConsecutive, final byte[] data) {
        if (eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE)) {
            for (int j = 0; j < eegManager.getNbStatusBytes(); j++) {
                byte tempStatus = 0;
                if (isConsecutive) {
                    tempStatus = data[eegManager.getRawDataIndexSize() + j];
                }

                for (int k = 0; k < (getSamplePerNotif() - j * 8 < 8 ? getSamplePerNotif() - j * 8 : 8); k++) {
                    if (statusData != null) {
                        if (isConsecutive) {
                            statusData.add(isBitSet(tempStatus, k));
                        } else {
                            statusData.add(Float.NaN);
                        }
                    }
                }
            }
        }
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     */
    private void storeEEGDataLostPacket() {
        Log.i(TAG, "storing lost EEG data");
        eegManager.storePendingDataInBuffer(eegManager.getLostPacketInterpolator(),0,bufPos,(bufPos +eegManager.getRawDataPacketSize())>eegManager.getRawDataBufferSize()?eegManager.getRawDataBufferSize()-bufPos :eegManager.getRawDataPacketSize());
    }

    /**
     *  Stores EEG raw data received from Bluetooth Device in a pending buffer.
     *  In case packet size is too large for buffer, the overflowing EEG data is stored in an overflow buffer
     * @param data EEG raw data array received from Bluetooth Device
     */
    private void storeEEGDataInBuffers(final byte[] data){
        Log.i(TAG, "storing EEG data In Buffers");

        int pendingBufferLength = eegManager.getRawDataPacketSize();//if we have no overflow, the pending buffer has the same size as the buffer
        //In case packet size is too large for buffer, we only take first part in pending buffer. Second part is stored in overflow buffer for future use
        if (bufPos + eegManager.getRawDataPacketSize() > eegManager.getRawDataBufferSize()) { // check that the maximum size is not reached if we can add a packet to the buffer
            eegManager.setOverflow(true); //notify the eeg manager that the buffer is full
            pendingBufferLength = eegManager.getRawDataBufferSize() - bufPos;
            eegManager.storeOverflowDataInBuffer(data,srcPos,bufPos,pendingBufferLength);//we store the overflowing part in the overflow buffer
        }
        eegManager.storePendingDataInBuffer(data,srcPos,bufPos,pendingBufferLength); //we store the pending buffer in both case (overflow or no overflow)
    }

    /**
     * Handle the pending data buffer when this buffer is full of EEG data:
     * Generates the status data, store overflow data buffer in the pending buffer and launch the data conversion
     */
    private void handleFullPendingData(){
        Log.i(TAG, "handling Pending data buffer" + eegManager.getPendingRawData().length);
        final ArrayList<Float> toDecodeStatus = generateToDecodeStatus();
        final byte[] toDecodeBytes = eegManager.getPendingRawData().clone();
        bufPos = (eegManager.hasOverflow())? eegManager.handleOverflowDataBuffer() : 0; //handleOverflowBuffer return eegManager.getRawDataPacketSize() / 2
        launchRawDataConversion(toDecodeBytes, toDecodeStatus);
    }

    /**
     * Handle consecutive or non consecutive frame : store the eeg data in specific buffers according to their origins
     * @param data EEG raw data array received from Bluetooth Device
     * @param isConsecutive is true if received frames are consecutive, false otherwise
     */
    private void handleConsecutiveOrNonConsecutiveFrame(final byte[] data, boolean isConsecutive) {
        generateStatusData(isConsecutive, data);
        if (isConsecutive)
            storeEEGDataInBuffers(data);
        else
            storeEEGDataLostPacket();
    }

    /**
     * Generates the status data corresponding to the EEG pending data buffer
     * @return
     */
    private ArrayList<Float> generateToDecodeStatus(){
        ArrayList<Float> toDecodeStatus = statusData;
        if(eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE)){
            if(eegManager.getNbStatusBytes() > 0){
                statusData = new ArrayList<>(); //reinit raw container
                if(toDecodeStatus != null){
                    if(toDecodeStatus.size() > sampleRate){
                        int size = toDecodeStatus.size();
                        for(int it = sampleRate; it < size; it++){
                            statusData.add(toDecodeStatus.get(it)); //re add overflow status
                        }
                        for(int it = 0; it < size - sampleRate ; it++){
                            toDecodeStatus.remove(toDecodeStatus.size()-1); //remove overflow status
                        }
                    }
                }
            }
        }
        return toDecodeStatus;
    }

    /**
     * Convert the raw EEG data array to useable EEG data matrix (float values) and notify that EEG data is ready to the UI
     * @param toDecodeBytes the EEG raw data array to convert
     * @param toDecodeStatus the status data corresponding to the EEG data array
     */
    private void launchRawDataConversion(final byte[] toDecodeBytes, final ArrayList<Float> toDecodeStatus ){
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "computing and sending to application");
                eegManager.convertRawDataToEEG(toDecodeBytes); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

                ArrayList<Float> status = new ArrayList<>();
                switch(eegManager.getMbtManager().getBluetoothProtocol()){
                    case BLUETOOTH_LE:
                        status = toDecodeStatus;
                        break;
                    case BLUETOOTH_SPP:
                        status = eegManager.getEegResult().get(0);
                        eegManager.getEegResult().remove(0);
                        break;
                }
                eegManager.notifyEEGDataIsReady(status, sampleRate, nbChannels);//notify UI that eeg data are ready
            }
        });
    }

    /**
     * Reset the starting and previous indexes
     */
    private void resetIndex() {
        startingIndex = -1;
        previousIndex = -1;
    }

    public static int getStartingIndex() {
        return startingIndex;
    }

    public static int getPreviousIndex() {
        return previousIndex;
    }

    public static int getBufPos() {
        return bufPos;
    }

}