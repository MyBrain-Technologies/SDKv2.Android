package core.eeg.acquisition;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
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
    private static byte[] lostPacketInterpolator; // Data size + compression byte + 2 packet length bytes

    private static int previousIndex = -1;
    private static int SRC_POS = 3;

    private static int bufPos = 0;

    private final int sampleRate;
    private final int nbChannels;

    private MbtEEGManager eegManager;

    private DataAcquisitionListener dataAcquisitionListener;
    private static ArrayList<Float> statusData;

    public MbtDataAcquisition( MbtEEGManager eegManagerController) {
        this.eegManager = eegManagerController;
        eegManager.setSamplePerNotif(getSamplePerNotif());
        this.nbChannels = MbtFeatures.getNbChannels();
        this.sampleRate = MbtFeatures.getSampleRate();
        int lostPacketInterpolatorSize = 0;

        if (eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_SPP)) {
            lostPacketInterpolatorSize = 138;
        } else if (eegManager.getMbtManager().getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE)){
            eegManager.setNbStatusBytes(MbtFeatures.getStatusSize());
            eegManager.setRawDataPacketSize(eegManager.getRawDataBytesPerWholeChannelsSamples() * getSamplePerNotif());
            lostPacketInterpolatorSize = 2 + eegManager.getRawDataPacketSize();
            if(eegManager.getNbStatusBytes() > 0)
                statusData = new ArrayList<>(sampleRate);
            else
                statusData = null;
        }

        lostPacketInterpolator = new byte[lostPacketInterpolatorSize];
        Arrays.fill(lostPacketInterpolator, (byte) 0xFF);

    }


    public synchronized void handleData(@NonNull final byte[] data) {

        if( !eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE) && !eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_SPP))
            return;

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

                if(eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE)){
                    for (int j = 0; j < eegManager.getNbStatusBytes(); j++) {
                        byte tempStatus = data[eegManager.getRawDataIndexSize() + j];
                        for (int k = 0; k < ((getSamplePerNotif() - j * 8) < 8 ? (getSamplePerNotif() - j * 8) : 8); k++) {

                            statusData.add(isBitSet(tempStatus, k));
                        }
                    }
                    SRC_POS = eegManager.getRawDataIndexSize()+eegManager.getNbStatusBytes();
                }
                int pendingBufferLength;
                //In case packet size is too large for buffer, we only take first part in pending buffer. Second part is stored in overflow buffer for future use
                if (bufPos + eegManager.getRawDataPacketSize() > eegManager.getRawDataBufferSize()) { // check that the maximum size is not reached if we can add a packet to the buffer
                    eegManager.setFirstBufferFull(true); //notify the eeg manager that the buffer is full
                    pendingBufferLength = eegManager.getRawDataBufferSize() - bufPos; // the size of the pending buffer is the total buffer size - the overflow buffer size
                    eegManager.storeOverflowBuffer(data,bufPos,SRC_POS);//we store the overflowing part in the overflow buffer
                } else {
                    pendingBufferLength = eegManager.getRawDataPacketSize(); //if we have no overflow, the pending buffer has the same size as the buffer
                }
                eegManager.storePendingBuffer(data,bufPos,SRC_POS,pendingBufferLength); //we store the pending buffer in both case (overflow or no overflow)

            } else {
                if(eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE)){
                    for (int j = 0; j < eegManager.getNbStatusBytes(); j++) {
                        for (int k = 0; k < (getSamplePerNotif() - j * 8 < 8 ? getSamplePerNotif() - j * 8 : 8); k++) {
                            statusData.add(Float.NaN);
                        }
                    }
                }
                eegManager.storePendingBuffer(lostPacketInterpolator,bufPos,0,(bufPos + eegManager.getRawDataPacketSize()) > eegManager.getRawDataBufferSize() ? eegManager.getRawDataBufferSize() -bufPos : eegManager.getRawDataPacketSize());
                Log.e(TAG, "Added FF packet");
            }
            bufPos += eegManager.getRawDataPacketSize();

            if (bufPos >= eegManager.getRawDataBufferSize()) { //the input eeg buffer is full so it contains enough data to compute a new MBTEEGPacket
                eegManager.setSecondBufferFull(true); //notify buffer is full to eeg manager, so that the manager can change the value of isBufferFull in MbtBuffering (observable) and notify that the data is ready with notifyNewEeg

                final ArrayList<Float> toDecodeStatus = statusData;
                if(eegManager.getMbtManager().getBluetoothProtocol().equals(BLUETOOTH_LE)){
                    if(eegManager.getNbStatusBytes() > 0){
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
                }

                final byte[] toDecodeBytes = eegManager.getPendingRawData().clone();

                if (eegManager.hasOverflow()) {
                    eegManager.handleOverflowBuffer();
                    bufPos = eegManager.getRawDataPacketSize() / 2;
                } else {
                    bufPos = 0;
                }

                AsyncUtils.executeAsync(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "computing and sending to application");
                        eegManager.convertRawDataToEEG(toDecodeBytes); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute
                        switch(eegManager.getMbtManager().getBluetoothProtocol()){
                            case BLUETOOTH_LE:
                                notifyDataIsReady(eegManager.getEegResult(), toDecodeStatus, sampleRate, nbChannels);
                                break;
                            case BLUETOOTH_SPP:
                                ArrayList<Float> status = eegManager.getEegResult().get(0);
                                eegManager.getEegResult().remove(0);
                                notifyDataIsReady(eegManager.getEegResult(), status, sampleRate, nbChannels);
                                break;
                        }
                    }
                });
            }
        }
        previousIndex = currentIndex;
    }

    private void notifyDataIsReady(@NonNull final ArrayList<ArrayList<Float>> matrix, final ArrayList<Float> status, final int sampleRate, final int nbChannels) {
        if (this.dataAcquisitionListener != null)
            this.dataAcquisitionListener.onDataReady(matrix, status, sampleRate, nbChannels);
    }

    interface DataAcquisitionListener {
        @WorkerThread
        void onDataReady(@NonNull final ArrayList<ArrayList<Float>> matrix, @Nullable final ArrayList<Float> status, final int sampleRate, final int nbChannels);
    }

    public final void setDataAcquisitionListener(@Nullable final DataAcquisitionListener dataAcquisitionListener) {
        this.dataAcquisitionListener = dataAcquisitionListener;
    }

    private static Float isBitSet(byte b, int bit){
        if((b & (1 << bit)) != 0)
            return 1f;
        else
            return 0f;
    }

    void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int statusByteNb) {
        eegManager.reconfigureBuffers(sampleRate,samplePerNotif,statusByteNb);
        //Reinit indexes
        startingIndex = previousIndex = -1;
        bufPos = 0;
    }

}