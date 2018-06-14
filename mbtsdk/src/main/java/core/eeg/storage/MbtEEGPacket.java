package core.eeg.storage;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

@Keep
public final class MbtEEGPacket {
    @NonNull
    private ArrayList<ArrayList<Float>> channelsData;
    @Nullable
    private ArrayList<Float> qualities;
    @Nullable
    private ArrayList<Float> statusData;
    @NonNull
    private final long timestamp;

    /**
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     * @param qualities The qualities stored in a list. The
     *                  list size should be equal to the number of channels if there is
     *                  a status channel.
     * @param timestamp the timestamp in milliseconds when this packet is created
     */
    @Keep
    public MbtEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData,
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
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     * @param qualities The qualities stored in a list. The
     *                  list size should be equal to the number of channels if there is
     *                  a status channel.
     * @param timestamp the timestamp in milliseconds when this packet is created
     */
    @Keep
    public MbtEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData,
                        @Nullable final ArrayList<Float> qualities, @Nullable final ArrayList<Float> statusData, @NonNull final long timestamp) {
        if (timestamp < 0)
            throw new IllegalArgumentException("timestamp must NOT be NEGATIVE");
        if (timestamp > System.currentTimeMillis())
            throw new IllegalArgumentException("timestamp cannot be in the future");

        this.channelsData = channelsData;
        this.statusData = statusData;
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
    @Nullable
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

    /**
     * Returns true if the instance contains an empty EEG data matrix and an empty list of qualities and an empty list of status.
     * Returns false otherwise.
     * @return Returns true if the EEG data list contains no elements.
     */
    public boolean isEmpty(){
        return this.channelsData.isEmpty() && this.qualities.isEmpty() && this.statusData.isEmpty();
    }

    /**
     * Returns true if the instance EEG data matrix only contains 0.
     * Returns false otherwise.
     * @return Returns true if the EEG data list contains only 0.
     */
    public boolean containsZerosEegOnly(){
        int counterOfZeros = 0;
        int matrixSize = this.getChannelsData().size()*this.getChannelsData().get(0).size();
        for (int i = 0; i < this.getChannelsData().size() ; i++){
            for (int j = 0 ; j < this.getChannelsData().get(0).size() ; j++){
                if(this.getChannelsData().get(j).equals(0F))
                    counterOfZeros++;
            }
        }
        return (counterOfZeros == matrixSize); //return true if counter == matrixSize (if matrix contains only 0)
    }

}
