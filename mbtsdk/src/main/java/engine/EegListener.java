package engine;


import java.util.ArrayList;
import core.eeg.storage.MbtEEGPacket;

public interface EegListener extends MbtClientEvents{
    /**
     * Callback triggered when the input eeg buffer is full, ie when raw buffer contains enough data to compute a new MBTEEGPacket.
     * This event is triggered from the MbtDataAcquisition class when (bufPos >= BLE_RAW_DATA_BUFFER_SIZE)
     * Warning, this callb is in worker thread. you need to call runOnUiThread to change views if necessary
     * @param eegPackets the eeg data (Channels, EEG values)
     */
    void onNewPackets(MbtEEGPacket eegPackets);
}