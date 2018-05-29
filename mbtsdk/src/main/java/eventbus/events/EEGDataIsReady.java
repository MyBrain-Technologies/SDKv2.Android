package eventbus.events;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Event posted when a raw EEG data array has been converted to user-readable EEG matrix
 * Event data contains the converted EEG data matrix
 *
 * @author Sophie Zecri on 24/05/2018
 */
public class EEGDataIsReady { //Events are just POJO without any specific implementation

    private ArrayList<ArrayList<Float>> matrix;
    private ArrayList<Float> status;
    private int sampleRate;
    private int nbChannels;

    public EEGDataIsReady(@NonNull ArrayList<ArrayList<Float>> matrix, ArrayList<Float> status, int sampleRate, int nbChannels) {
        this.matrix = matrix;
        this.status = status;
        this.sampleRate = sampleRate;
        this.nbChannels = nbChannels;
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the user-readable EEG data matrix
     */
    public ArrayList<ArrayList<Float>> getMatrix() {
        return matrix;
    }

    /**
     * Gets the status corresponding to the EEG data
     * @return the status corresponding to the EEG data
     */
    public ArrayList<Float> getStatus() {
        return status;
    }

    /**
     * Gets the sample rate
     * @return the sample rate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Gets the number of channels used for EEG acquisition
     * @return the number of channels used for EEG acquisition
     */
    public int getNbChannels() {
        return nbChannels;
    }
}
