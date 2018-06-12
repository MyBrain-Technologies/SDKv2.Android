package core.eeg.storage;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

@Keep
public final class MbtEEGPacket {
    @NonNull
    private ArrayList<ArrayList<Float>> channelsData = new ArrayList<>(); //TODO check if we need to set this synchornized
    @Nullable
    private ArrayList<Float> qualities = new ArrayList<>();

    private ArrayList<Float> statusData = new ArrayList<>();
    @NonNull
    private final long timestamp;


    public MbtEEGPacket(){
        timestamp = System.currentTimeMillis();
    }

    /**
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     * @param qualities The qualities stored in a list. The
     *                  list size should be equal to the number of channels if there is
     *                  a status channel.
     */
    @Keep
    public MbtEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData,
                        @Nullable final ArrayList<Float> qualities) {

        this.channelsData = channelsData;
        this.qualities = qualities;
        if(this.qualities == null){
            this.qualities = new ArrayList<>();
        }
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     * @param qualities The qualities stored in a list. The
     *                  list size should be equal to the number of channels if there is
     *                  a status channel.
     */
    @Keep
    public MbtEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData,
                        @Nullable final ArrayList<Float> qualities, final ArrayList<Float> statusData) {

        this.channelsData = channelsData;
        this.statusData = statusData;
        this.qualities = qualities;
        if(this.qualities == null){
            this.qualities = new ArrayList<>();
        }

        this.timestamp = System.currentTimeMillis();

    }

    /**
     * Gets the TimeStamp
     * @return The timestamp of type <code>long</code> when this packet has been created
     */
    @Keep
    public final long getTimeStamp() {
        return this.timestamp;
    }

    /**
     * Gets all the data from all channels
     * @return the data from all channels
     */
    @NonNull
    @Keep
    public ArrayList<ArrayList<Float>> getChannelsData() {
        return channelsData;
    }

    /**
     * Sets value to all the data from all channels
     * @param channelsData the new value for the data from all channels
     */
    @Keep
    public void setChannelsData(@NonNull ArrayList<ArrayList<Float>> channelsData) {
        this.channelsData = channelsData;
    }

    /**
     * Gets the status data
     * @return the status data
     */
    public ArrayList<Float> getStatusData() {
        return statusData;
    }

    /**
     * Get the list of qualities
     * @return the qualities
     */
    @Nullable
    @Keep
    public ArrayList<Float> getQualities() {
        return qualities;
    }

    /**
     * Sets a new value to the qualities list
     * @return the qualities
     */
    @Keep
    public void setQualities(@NonNull ArrayList<Float> qualities) {
        this.qualities = qualities;
    }

    @Override
    public String toString() {
        return "MbtEEGPacket{" +
                "EEG Data=" + channelsData +
                ", qualities=" + qualities +
                ", statusData=" + statusData +
                ", timestamp=" + timestamp +
                '}';
    }
}
