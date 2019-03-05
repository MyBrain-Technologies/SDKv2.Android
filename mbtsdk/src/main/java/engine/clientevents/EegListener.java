package engine.clientevents;


import android.support.annotation.Keep;
import android.support.annotation.NonNull;


import core.eeg.storage.MbtEEGPacket;

/**
 * callback for eeg acquisition. This is often called "streaming"
 *
 * @param <U>
 */
@Keep
public interface EegListener<U extends BaseError> extends BaseErrorEvent<U>{

    /**
     * Callback triggered when the input eeg buffer is full, ie when raw buffer contains enough data to compute a new MBTEEGPacket.
     * This event is triggered from the EEG module when buffer is full.
     * @param eegPackets the eeg data as an MbtEEGPacket object. call {@link MbtEEGPacket#getChannelsData()} to get eeg as a matrix.
     * <p>Warning: the matrix is configured as follow : (EEG, channel)</p>
     *
     */
    void onNewPackets(@NonNull MbtEEGPacket eegPackets);
}