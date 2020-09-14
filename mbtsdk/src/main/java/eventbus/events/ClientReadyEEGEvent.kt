package eventbus.events

import core.eeg.storage.MbtEEGPacket

/**
 * Event posted when a raw EEG data array has been converted to user-readable EEG matrix
 * Event data contains the converted EEG data matrix
 *
 * @author Sophie Zecri on 24/05/2018
 */
class ClientReadyEEGEvent(
    /**
     * Gets the user-readable list of accumulated MbtEEGPacket.
     * The size of this list can be determined by the client/ SDK user.
     * A MbtEEGPacket is an object that contains the EEG data matrix, their associated qualities and status.
     * @return the list of accumulated MbtEEGPacket
     */
    //Events are just POJO without any specific implementation
    val eegPackets: MbtEEGPacket) : IEvent