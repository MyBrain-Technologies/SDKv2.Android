package com.mybraintech.sdk.core.acquisition.eeg;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Keep
public final class MbtEEGPacket2 {

    /**
     * eg: 4x250
     */
    @NonNull
    private ArrayList<ArrayList<Float>> channelsData;

    @Nullable
    private ArrayList<Float> statusData;

    private final long timestamp;

    /**
     * eg: 4
     */
    private ArrayList<Float> qualities;

    private float[][] features = null;


    public MbtEEGPacket2(MbtEEGPacket2 packetToClone){
        this.timestamp = packetToClone.getTimeStamp();
        this.channelsData = packetToClone.getChannelsData();
        this.statusData = packetToClone.getStatusData();
        this.qualities = packetToClone.getQualities();
        this.features = packetToClone.getFeatures();
    }

    /**
     * Initializes a new instance of the MbtEEGPacket class.
     * @param channelsData The values from all channels
     * @param statusData The statuses associated
     */
    public MbtEEGPacket2(@NonNull final ArrayList<ArrayList<Float>> channelsData, @Nullable final ArrayList<Float> statusData) {
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
