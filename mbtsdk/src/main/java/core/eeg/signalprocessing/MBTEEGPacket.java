package core.eeg.signalprocessing;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

@Keep
public final class MBTEEGPacket {
    @NonNull
    private ArrayList<ArrayList<Float>> channelsData;
    @Nullable
    private ArrayList<Float> qualities;
    @NonNull
    private final long timestamp;

    /**
     * Initializes a new instance of the MBTEEGPacket class.
     * @param channelsData The values from all channels
     * @param qualities The qualities stored in a list. The
     *                  list size should be equal to the number of channels if there is
     *                  a status channel.
     * @param timestamp the timestamp in milliseconds when this packet is created
     */
    @Keep
    public MBTEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData,
                        @Nullable final ArrayList<Float> qualities, final long timestamp) {
        if (timestamp < 0)
            throw new IllegalArgumentException("timestamp must NOT be NEGATIVE");
        if (timestamp > System.currentTimeMillis())
            throw new IllegalArgumentException("timestamp cannot be in the future");

        this.channelsData = channelsData;
        this.qualities = qualities;
        if(this.qualities == null){
            this.qualities = new ArrayList<>();
            this.qualities.add(0f);
            this.qualities.add(0f);
        }
        this.timestamp = timestamp;
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

    @Keep
    public void setChannelsData(@NonNull ArrayList<ArrayList<Float>> channelsData) {
        this.channelsData = channelsData;
    }

    /**
     * Get the qualities as an arrayList
     * @return the qualities as an array
     */
    @Nullable
    @Keep
    public ArrayList<Float> getQualities() {
        return qualities;
    }

    @Keep
    public void setQualities(@NonNull ArrayList<Float> qualities) {
        this.qualities = qualities;
    }
}
