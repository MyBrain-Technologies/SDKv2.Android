package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.bluetooth.BtProtocol;
import utils.AsyncUtils;

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
    //SPP values are kept final for the moment, should probably be modifiable in the future
    private final int SPP_RAW_DATA_BUFFER_SIZE = 6750;
    private final int SPP_RAW_DATA_PACKET_SIZE = 4*9*3; //4 samples per packet 9 channels 3bytes data

    public static int BLE_RAW_DATA_INDEX_SIZE = 2;
    public static int BLE_RAW_DATA_SAMPLE_SIZE = 2;
    public static int BLE_RAW_DATA_NB_CHANNEL = 2;
    public static int BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = BLE_RAW_DATA_SAMPLE_SIZE * BLE_RAW_DATA_NB_CHANNEL;
    public static int BLE_NB_STATUS_BYTES = 0;
    private static int BLE_DEFAULT_SAMPLE_PER_PACKET = 4;
    public static int BLE_SAMPLE_PER_NOTIF = BLE_DEFAULT_SAMPLE_PER_PACKET;


    public static int BLE_RAW_DATA_PACKET_SIZE = BLE_DEFAULT_SAMPLE_PER_PACKET*BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES + BLE_NB_STATUS_BYTES; //4 samples per packet 2 channels 2bytes data
    public static int BLE_RAW_DATA_BUFFER_SIZE = BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES * 250;

    public final int sampleRate;
    public final int nbChannels;

    // EEG Data & Values
    private static int startingIndex = -1;
    private static byte[] lostPacketInterpolator; // Data size + compression byte + 2 packet length bytes

    private DataAcquisitionListener dataAcquisitionListener;

    private static int previousIndex = -1;
    private static byte[] pendingRawData;
    private static int bufPos = 0;
    private static byte[] oveflowBytes;
    private static boolean hasOverflow = false;
    private static ArrayList<Float> statusData;

    public MbtDataAcquisition(final int sampleRate, final int nbChannels, final int samplePerNotif, @NonNull BtProtocol btProtocol, final int statusByteNb) {
        BLE_SAMPLE_PER_NOTIF = samplePerNotif;
        this.nbChannels = nbChannels;
        this.sampleRate = sampleRate;

        if (btProtocol == BtProtocol.BLUETOOTH_SPP) {

            lostPacketInterpolator = new byte[138];
            Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
            pendingRawData = new byte[SPP_RAW_DATA_BUFFER_SIZE];
            oveflowBytes = new byte[SPP_RAW_DATA_PACKET_SIZE /2];

        }

        else {
            BLE_NB_STATUS_BYTES = statusByteNb;
            BLE_RAW_DATA_PACKET_SIZE = BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES * samplePerNotif;
            lostPacketInterpolator = new byte[2 + BLE_RAW_DATA_PACKET_SIZE];
            Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
            pendingRawData = new byte[BLE_RAW_DATA_BUFFER_SIZE];
            oveflowBytes = new byte[BLE_RAW_DATA_PACKET_SIZE];

            if(BLE_NB_STATUS_BYTES > 0)
                statusData = new ArrayList<>(sampleRate);
            else
                statusData = null;

        }
    }


    public synchronized void handleData(@NonNull final byte[] data, final BtProtocol protocol) {
        switch (protocol) {
            case BLUETOOTH_LE:
                handleBleData(data);
                break;
            case BLUETOOTH_SPP:
                handleBsppData(data);
                break;
            default:
                return;
        }
    }

    void resetIndex() {
        startingIndex = -1;
        previousIndex = -1;
    }

    private void handleBleData(@NonNull final byte[] data) {
        final int currentIndex = (data[0] & 0xff) << 8 | (data[1] & 0xff);
        //taking care of the first index
        if(previousIndex == -1){
            previousIndex = currentIndex -1;
        }

        final int indexDifference = currentIndex - previousIndex;

        if(indexDifference != 1)
            Log.e(TAG, "diff is " + indexDifference);

        for(int i = 0; i < indexDifference; i++) {

            //Checking whether received frames are consecutive or not
            if ((byte)(indexDifference - i - 1) == 0) {

                for(int j = 0; j < BLE_NB_STATUS_BYTES; j++){
                    byte tempStatus = data[BLE_RAW_DATA_INDEX_SIZE+j];
                    for(int k = 0; k < ((BLE_SAMPLE_PER_NOTIF - j*8) < 8 ? (BLE_SAMPLE_PER_NOTIF - j*8) : 8); k++){

                        statusData.add(isBitSet(tempStatus, k));
                    }
                }
                //In case packet size is too large for buffer, we only take first part in pending buffer. Second part is stored in overflow buffer for future use
                if (bufPos + BLE_RAW_DATA_PACKET_SIZE > BLE_RAW_DATA_BUFFER_SIZE) {
                    System.arraycopy(data, BLE_RAW_DATA_INDEX_SIZE+BLE_NB_STATUS_BYTES, pendingRawData, bufPos, BLE_RAW_DATA_BUFFER_SIZE - bufPos);
                    System.arraycopy(data, BLE_RAW_DATA_INDEX_SIZE+BLE_NB_STATUS_BYTES + BLE_RAW_DATA_BUFFER_SIZE - bufPos, oveflowBytes, 0, BLE_RAW_DATA_PACKET_SIZE - (BLE_RAW_DATA_BUFFER_SIZE - bufPos));
                    hasOverflow = true;
                    bufPos += BLE_RAW_DATA_PACKET_SIZE;
                } else {
                    System.arraycopy(data, BLE_RAW_DATA_INDEX_SIZE+BLE_NB_STATUS_BYTES, pendingRawData, bufPos, BLE_RAW_DATA_PACKET_SIZE);
                    bufPos += BLE_RAW_DATA_PACKET_SIZE;
                }
            } else {
                for(int j = 0; j < BLE_NB_STATUS_BYTES; j++){
                    for(int k = 0; k < (BLE_SAMPLE_PER_NOTIF - j*8 < 8 ? BLE_SAMPLE_PER_NOTIF - j*8 : 8); k++){
                        statusData.add(Float.NaN);
                    }
                }
                System.arraycopy(lostPacketInterpolator, 0, pendingRawData, bufPos, (bufPos + BLE_RAW_DATA_PACKET_SIZE) > BLE_RAW_DATA_BUFFER_SIZE ? BLE_RAW_DATA_BUFFER_SIZE - bufPos : BLE_RAW_DATA_PACKET_SIZE);
                bufPos += BLE_RAW_DATA_PACKET_SIZE;
                Log.e(TAG, "Added FF packet");
            }

            if (bufPos >= BLE_RAW_DATA_BUFFER_SIZE) {
                final ArrayList<Float> toDecodeStatus = statusData;
                if(BLE_NB_STATUS_BYTES > 0){
                    statusData = new ArrayList<>();
                    if(toDecodeStatus.size() > sampleRate){
                        int size = toDecodeStatus.size();
                        for(int it = sampleRate; it < size; it++){
                            statusData.add(toDecodeStatus.get(it));

                        }
                        for(int it = 0; it < size - sampleRate ; it++){
                            toDecodeStatus.remove(toDecodeStatus.size()-1);
                        }

                    }
                }

                final byte[] toDecodeBytes = pendingRawData.clone();
                pendingRawData = new byte[BLE_RAW_DATA_BUFFER_SIZE];
                if (hasOverflow) {
                    Log.i(TAG, "overflow detected");
                    System.arraycopy(oveflowBytes, 0, pendingRawData, 0, BLE_RAW_DATA_PACKET_SIZE / 2);
                    bufPos = BLE_RAW_DATA_PACKET_SIZE / 2;
                    hasOverflow = false;
                } else {
                    bufPos = 0;
                }

                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "computing and sending to application");
                        ArrayList<ArrayList<Float>> eegResult = MbtHandleData.handleBleData(toDecodeBytes);
                        notifyDataIsReady(eegResult, toDecodeStatus, sampleRate, nbChannels);
                    }
                });
            }
        }
        previousIndex = currentIndex;
    }

    private void handleBsppData(@NonNull final byte[] data) {

        final int currentIndex = (data[1] & 0xff) << 8 | (data[2] & 0xff);

        //taking care of the first index
        if(previousIndex == -1){
            previousIndex = currentIndex -1;
        }

        final int indexDifference = currentIndex - previousIndex;
        for(int i = 0; i < indexDifference; i++){

            //Checking whether received frames are consecutive or not
            if(indexDifference - i - 1 == 0){

                //In case packet size is too large for buffer, we only take first part in pending buffer. Second part is stored in overflow buffer for future use
                if(bufPos + SPP_RAW_DATA_PACKET_SIZE > SPP_RAW_DATA_BUFFER_SIZE){
                    System.arraycopy(data, 3, pendingRawData, bufPos, SPP_RAW_DATA_BUFFER_SIZE -bufPos);
                    System.arraycopy(data, 3+ SPP_RAW_DATA_BUFFER_SIZE -bufPos, oveflowBytes, 0, SPP_RAW_DATA_PACKET_SIZE -(SPP_RAW_DATA_BUFFER_SIZE -bufPos));
                    hasOverflow = true;
                    bufPos += SPP_RAW_DATA_PACKET_SIZE;
                }else{
                    System.arraycopy(data, 3, pendingRawData, bufPos, SPP_RAW_DATA_PACKET_SIZE);
                    bufPos += SPP_RAW_DATA_PACKET_SIZE;
                }

            }else{
                System.arraycopy(lostPacketInterpolator, 0, pendingRawData, bufPos, (bufPos + SPP_RAW_DATA_PACKET_SIZE) > SPP_RAW_DATA_BUFFER_SIZE ? SPP_RAW_DATA_BUFFER_SIZE -bufPos : SPP_RAW_DATA_PACKET_SIZE);
                bufPos += SPP_RAW_DATA_PACKET_SIZE;
                Log.e(TAG, "Added FF packet");
            }

            if (bufPos >= SPP_RAW_DATA_BUFFER_SIZE) {
                final List<byte[]> toDecode = new ArrayList<>();
                final byte[] toDecodeBytes = pendingRawData.clone();
                pendingRawData = new byte[SPP_RAW_DATA_BUFFER_SIZE];
                if(hasOverflow){
                    Log.i(TAG, "overflow detected");
                    System.arraycopy(oveflowBytes, 0, pendingRawData, 0, SPP_RAW_DATA_PACKET_SIZE /2);
                    bufPos = SPP_RAW_DATA_PACKET_SIZE /2;
                    hasOverflow = false;
                }else{
                    bufPos = 0;
                }

                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "computing and sending to application");
                        ArrayList<ArrayList<Float>> eegResult = MbtHandleData.handleBsppData(toDecodeBytes);
                        ArrayList<Float> status = eegResult.get(0);
                        eegResult.remove(0);
                        notifyDataIsReady(eegResult, status, sampleRate, nbChannels);
                    }
                });
            }
        }
        previousIndex = currentIndex;
    }

    public final void setDataAcquisitionListener(@Nullable final DataAcquisitionListener dataAcquisitionListener) {
        this.dataAcquisitionListener = dataAcquisitionListener;
    }

    static void reconfigureBuffers(final int sampleRate, int nbChannels, byte samplePerNotif, BtProtocol protocol, final int statusByteNb){
        if(protocol == BtProtocol.BLUETOOTH_LE){
            BLE_NB_STATUS_BYTES = statusByteNb;
            BLE_RAW_DATA_PACKET_SIZE = BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES * samplePerNotif;
            lostPacketInterpolator = new byte[2 + BLE_RAW_DATA_PACKET_SIZE];
            Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
            pendingRawData = new byte[BLE_RAW_DATA_BUFFER_SIZE];
            oveflowBytes = new byte[BLE_RAW_DATA_PACKET_SIZE];

            if(BLE_NB_STATUS_BYTES > 0)
                statusData = new ArrayList<>(sampleRate);
            else
                statusData = null;

        }else{
            //TODO
        }

        //Reinit indexes
        startingIndex = previousIndex = -1;
        bufPos = 0;

    }


    private void notifyDataIsReady(@NonNull final ArrayList<ArrayList<Float>> matrix, final ArrayList<Float> status, final int sampleRate, final int nbChannels) {
        if (this.dataAcquisitionListener != null)
            this.dataAcquisitionListener.onDataReady(matrix, status, sampleRate, nbChannels);
    }

    public interface DataAcquisitionListener {
        @WorkerThread
        void onDataReady(@NonNull final ArrayList<ArrayList<Float>> matrix, @Nullable final ArrayList<Float> status, final int sampleRate, final int nbChannels);
    }


    private static Float isBitSet(byte b, int bit)
    {
        if((b & (1 << bit)) != 0)
            return 1f;
        else
            return 0f;
    }

}