package core.eeg.storage;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

@Keep
public final class MbtEEGPacket {
    @NonNull
    private ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();

    @Nullable
    private ArrayList<Float> statusData = new ArrayList<>();

    @NonNull
    private final long timestamp;

    private ArrayList<Float> qualities;

    private float[][] features = null;


    public MbtEEGPacket(MbtEEGPacket packetToClone){
        this.timestamp = packetToClone.getTimeStamp();
        this.channelsData = packetToClone.getChannelsData();
        this.statusData = packetToClone.getStatusData();
        this.qualities = packetToClone.getQualities();
        this.features = packetToClone.getFeatures();
    }

    public MbtEEGPacket(){
        timestamp = System.currentTimeMillis();
    }

    /**
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     */
    public MbtEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData) {

        this.channelsData = channelsData;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     * @param statusData The statuses associated
     */
    public MbtEEGPacket(@NonNull final ArrayList<ArrayList<Float>> channelsData, final ArrayList<Float> statusData) {

        this.channelsData = channelsData;
        this.statusData = statusData;
        this.timestamp = System.currentTimeMillis();

    }

    /**
     * Gets the TimeStamp
     * @return The timestamp of type <code>long</code> when this packet has been created
     */
    public final long getTimeStamp() {
        return this.timestamp;
    }

    /**
     * Gets all the EEG data acquired from all channels in a matrix format during a specific duration.
     * <p> Each column of the matrix contains the number of EEG data acquired by one channel.</p>
     * <p> Call {@link #getChannelsData()}.size() to get the number of EEG data acquired by one channel.
     * <p> Each line of the matrix contains the acquired EEG data by all the channels at a specifc moment:
     * <p> 2 EEG data acquired for a Melomind headset and 9 EEG data for a Vpro headset (each channels acquire the same number of EEG data).
     * <p> Call {@link #getChannelsData}.get(0).size() to get the number of channels/electrodes.
     *
     * @return the EEG data from all channels
     */
    @NonNull
    public ArrayList<ArrayList<Float>> getChannelsData() {
        return channelsData;
    }

    /**
     * Sets value to all the data from all channels
     * @param channelsData the new value for the data from all channels
     */
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

    @Override
    public String toString() {
        return "MbtEEGPacket{" +
                "EEG=" + (channelsData != null && !channelsData.isEmpty() ? (channelsData.size()+"x"+channelsData.get(0).size()) : channelsData) +
                ",\n quality= [" + (qualities != null ? (qualities.get(0) + "," +qualities.get(1)) : null+"]") +
                "\\n, statusData=" + (statusData != null ? "size: " +statusData.size() : null) +
                ",\n timestamp=" + timestamp +
                '}';
    }

    /**
     * Returns true if the instance contains an empty EEG data matrix and an empty list of qualities and an empty list of status.
     * Returns false otherwise.
     * @return Returns true if the EEG data list contains no elements.
     */
    public boolean isEmpty(){
        return this.channelsData.isEmpty() && this.statusData.isEmpty();
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

    public ArrayList<Float> getQualities() {
        return qualities;
    }

    public void setQualities(ArrayList<Float> qualities) {
        this.qualities = qualities;
    }

    public float[][] getFeatures() {
        return features;
    }

    public ArrayList<Float> getFeature(int frequency) {
        ArrayList<Float> feature = new ArrayList<>();
        for (int channelIndex = 0; channelIndex < features.length ; channelIndex++){
            feature.add(features[channelIndex][frequency]);
        }
        return feature;
    }

    public void setFeatures(float[][] features) {
        this.features = features;
    }

    public void setStatusData(@Nullable ArrayList<Float> statusData) {
        this.statusData = statusData;
    }
}
