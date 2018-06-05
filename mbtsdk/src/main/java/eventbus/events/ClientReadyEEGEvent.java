package eventbus.events;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import core.eeg.storage.MBTEEGPacket;

/**
 * Event posted when a raw EEG data array has been converted to user-readable EEG matrix
 * Event data contains the converted EEG data matrix
 *
 * @author Sophie Zecri on 24/05/2018
 */
public class ClientReadyEEGEvent { //Events are just POJO without any specific implementation

    private ArrayList<MBTEEGPacket> eegPackets;
    private ArrayList<Float> status;

    public ClientReadyEEGEvent(@NonNull ArrayList<MBTEEGPacket> eegPackets, ArrayList<Float> status) {
        this.eegPackets = eegPackets;
        this.status = status;
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the user-readable EEG data matrix
     */
    public ArrayList<MBTEEGPacket> getEegPackets() {
        return eegPackets;
    }

    /**
     * Gets the status corresponding to the EEG data
     * @return the status corresponding to the EEG data
     */
    public ArrayList<Float> getStatus() {
        return status;
    }

}
