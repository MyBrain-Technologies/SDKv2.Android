package engine;


import core.eeg.storage.MBTEEGPacket;

public interface EegListener extends MbtClientEvents{
    /**
     * Callback triggered when the input eeg buffer is full, ie when raw buffer contains enough data to compute a new MBTEEGPacket.
     * This event is triggered from the MbtDataAcquisition class when (bufPos >= BLE_RAW_DATA_BUFFER_SIZE)
     * Warning, this callb is in worker thread. you need to call runOnUiThread to change views if necessary
     * @param mbteegPackets the eeg data (Channels, EEG values)
     * @param nbChannels the number of EEG acquisition channels
     * @param nbSamples //TODO remove this input
     * @param sampleRate //TODO might be unnecessary
     */
    void onNewPackets(final MBTEEGPacket mbteegPackets, final int nbChannels, final int nbSamples, final int sampleRate);
}