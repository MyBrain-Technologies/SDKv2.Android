package core.eeg.storage;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import core.eeg.MbtEEGManager;


public class MbtBuffering  extends Observable {
    private static final String TAG = MbtBuffering.class.getName();

    private static byte[] oveflowBytes;
    private static byte[] pendingRawData;

    private static boolean hasOverflow = false;
    private static ArrayList<Float> statusData;
    private MbtEEGManager eegManager;

    private static boolean isFirstBufferFull;
    private static boolean isSecondBufferFull;

    // EEG Data & Values
    private static int startingIndex = -1;
    private static byte[] lostPacketInterpolator; // Data size + compression byte + 2 packet length bytes

    public MbtBuffering(MbtEEGManager eegManagerController) {
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
        isFirstBufferFull = false;
        isSecondBufferFull = false;
    }

    public void storePendingBuffer(final byte[] data, final int bufPos, final int src_pos, final int length){
        System.arraycopy(data, src_pos, pendingRawData, bufPos, length);
    }

    public void storeOverflowBuffer(final byte[] data, final int bufPos, final int src_pos){
        System.arraycopy(data, src_pos + eegManager.getRawDataBufferSize() - bufPos, oveflowBytes, 0, eegManager.getRawDataPacketSize() - (eegManager.getRawDataBufferSize() - bufPos));
        hasOverflow = true;
    }

    public void storeOverflowInPendingBuffer(final int length) {
        System.arraycopy(oveflowBytes, 0, pendingRawData, 0, length);
        hasOverflow = false;
    }

    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int statusByteNb){

        eegManager.setNbStatusBytes(statusByteNb);
        eegManager.setRawDataPacketSize(eegManager.getRawDataBytesPerWholeChannelsSamples() * samplePerNotif);
        lostPacketInterpolator = new byte[2 + eegManager.getRawDataPacketSize()];
        Arrays.fill(lostPacketInterpolator, (byte) 0xFF);
        pendingRawData = new byte[eegManager.getRawDataBufferSize()];
        oveflowBytes = new byte[eegManager.getRawDataPacketSize()];

        if(eegManager.getNbStatusBytes() > 0)
            statusData = new ArrayList<>(sampleRate);
        else
            statusData = null;
    }

    public void handleOverflowBuffer(){
        pendingRawData = new byte[eegManager.getRawDataBufferSize()];
        Log.i(TAG, "overflow detected");
        storeOverflowInPendingBuffer(eegManager.getRawDataPacketSize() /2);
        hasOverflow = false;

    }
    private void notifyBufferIsFull() {
        setChanged();
        notifyObservers(isSecondBufferFull);
    }
    
    public void setFirstBufferFull(boolean isFull) {
        isFirstBufferFull = isFull;
    }

    public void setSecondBufferFull(boolean bufferFull){
        if(bufferFull){
            notifyBufferIsFull();
        }
        isSecondBufferFull=bufferFull;
    }

    public byte[] getPendingRawData() {
        return pendingRawData;
    }

    public boolean hasOverflow() {
        return hasOverflow;
    }

}
