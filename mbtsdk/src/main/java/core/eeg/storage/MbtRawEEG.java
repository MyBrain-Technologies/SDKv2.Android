package core.eeg.storage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import features.MbtFeatures;

/**
 * Object that contains the EEG raw data for a single channel at a given moment and its status
 */
public class MbtRawEEG{

    @NonNull
    private byte[] bytesEEG; //array size must be 2 or 3 bytes

    @Nullable
    private ArrayList<Float> statusEEG; //list size must be 2 or 3

    /**
     * Initializes a new instance of the MbtRawEEG object.
     * This object stores a single EEG data and its associated status.
     * @param bytes The raw EEG data array corresponding to a single EEG data.
     * @param statusEEG The status associated to this EEG data.
     */
    public MbtRawEEG(@NonNull byte[] bytes, @Nullable ArrayList<Float> statusEEG) {
        this.bytesEEG = bytes;
        this.statusEEG = statusEEG;
    }
    /**
     * Initializes a new instance of the MbtRawEEG object.
     * This object stores a single EEG data and its associated status.
     * @param defaultValue The default value for initializing all the items of the raw EEG data array
     * @param statusEEG The status associated to this EEG data.
     */
    public MbtRawEEG(@NonNull byte defaultValue, @Nullable ArrayList<Float> statusEEG) {
        this.bytesEEG = new byte[MbtFeatures.getNbBytes()];
        for (int i = 0 ; i< MbtFeatures.getNbBytes(); i++){
            this.bytesEEG[i] = defaultValue;
        }
        this.statusEEG = statusEEG;
    }

    /**
     * Gets the status data
     * @return the status data
     *
     */
    @NonNull
    public byte[] getBytesEEG() {
        return bytesEEG;
    }

    /**
     * Sets a new value to the raw EEG data array
     * @param rawEEG the new raw EEG data array value
     */
    public void setBytesEEG(@NonNull byte[] rawEEG) {
        this.bytesEEG = rawEEG;
    }

    /**
     * Gets the status data
     * @return the status data
     */
    @Nullable
    public ArrayList<Float> getStatus() {
        return statusEEG;
    }

    /**
     * Sets a new value to the status data
     * @param status the new status data
     */
    public void setStatus(@Nullable ArrayList<Float> status) {
        this.statusEEG = status;
    }


    @Override
    public String toString() {
        /*
        return "MbtRawEEG{" +
                "bytesEEG=" + Arrays.toString(bytesEEG) +
                ", statusEEG=" + statusEEG +
                '}';*/
        return "" + Arrays.toString(bytesEEG);
    }
}
