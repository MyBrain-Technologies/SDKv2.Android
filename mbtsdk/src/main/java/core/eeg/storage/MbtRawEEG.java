package core.eeg.storage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

public class MbtRawEEG{

    @NonNull
    private byte[] rawEEG;

    @Nullable
    private ArrayList<Float> status;

    /**
     * Initializes a new instance of the MbtRawEEG class.
     * @param rawEEG The raw EEG data array
     * @param status The status
     */
    public MbtRawEEG(@NonNull byte[] rawEEG, @Nullable ArrayList<Float> status) {
        this.rawEEG = rawEEG;
        this.status = status;
    }

    /**
     * Gets the status data
     * @return the status data
     *
     */
    @NonNull
    public byte[] getRawEEG() {
        return rawEEG;
    }

    /**
     * Sets a new value to the raw EEG data array
     * @param rawEEG the new raw EEG data array value
     */
    public void setRawEEG(@NonNull byte[] rawEEG) {
        this.rawEEG = rawEEG;
    }

    /**
     * Gets the status data
     * @return the status data
     */
    @Nullable
    public ArrayList<Float> getStatus() {
        return status;
    }

    /**
     * Sets a new value to the status data
     * @param status the new status data
     */
    public void setStatus(@Nullable ArrayList<Float> status) {
        this.status = status;
    }
}
