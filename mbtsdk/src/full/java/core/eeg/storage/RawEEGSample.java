package core.eeg.storage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Object that contains the EEG raw data for all channels at a given moment and its associated status, if any
 */
public class RawEEGSample {

    @Nullable
    private final ArrayList<byte[]> bytesEEG; //array size must be 2 or 3 bytes

    @Nullable
    private Float statusEEG = null; //list size must be 2 or 3

    @NonNull
    public static RawEEGSample LOST_PACKET_INTERPOLATOR = new RawEEGSample(null, Float.NaN);

    /**
     * Initializes a new instance of the RawEEGSample object.
     * This object stores a single EEG data and its associated status.
     * @param bytes The raw EEG data array corresponding to a single EEG data.
     * @param statusEEG The status associated to this EEG data.
     */
    public RawEEGSample(@Nullable final ArrayList<byte[]> bytes, @Nullable Float statusEEG) {
        this.bytesEEG = bytes;
        this.statusEEG = statusEEG;
    }


    /**
     * Initializes a new instance of the RawEEGSample object.
     * This object stores a single EEG data and its associated status.
     * @param bytes The raw EEG data array corresponding to a single EEG data.
     */
    public RawEEGSample(ArrayList<byte[]> bytes) {
        this.bytesEEG = bytes;
    }

    /**
     * Gets the status data
     * @return the status data
     *
     */
    @Nullable
    public ArrayList<byte[]> getBytesEEG() {
        return bytesEEG;
    }

    /**
     * Gets the status data
     * @return the status data
     */
    @Nullable
    public Float getStatus() {
        return statusEEG;
    }

    /**
     * Sets a new value to the status data
     * @param status the new status data
     */
    public void setStatus(@Nullable Float status) {
        this.statusEEG = status;
    }


    @NonNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (byte[] bytes : bytesEEG) {
            s.append(Arrays.toString(bytes));
        }
        s.append(" and status " + statusEEG);
        return s.toString();
    }
}
