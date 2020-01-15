package eventbus.events;

import androidx.annotation.NonNull;

import core.eeg.storage.MbtEEGPacket;

/**
 * Event posted when a raw EEG data array has been converted to user-readable EEG matrix
 * Event data contains the converted EEG data matrix
 *
 * @author Sophie Zecri on 24/05/2018
 */
public class ClientReadyEEGEvent { //Events are just POJO without any specific implementation

    private MbtEEGPacket eegPackets;

    public ClientReadyEEGEvent(@NonNull MbtEEGPacket eegPackets) {
        this.eegPackets = eegPackets;
    }

    /**
     * Gets the user-readable list of accumulated MbtEEGPacket.
     * The size of this list can be determined by the client/ SDK user.
     * A MbtEEGPacket is an object that contains the EEG data matrix, their associated qualities and status.
     * @return the list of accumulated MbtEEGPacket
     */
    public MbtEEGPacket getEegPackets() {
        return eegPackets;
    }

}
