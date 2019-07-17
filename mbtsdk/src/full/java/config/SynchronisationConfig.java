package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

import core.eeg.storage.MbtEEGFeatures;

@Keep
public final class SynchronisationConfig {

    private String ipAddress;

    private int port;

    private boolean streamRawEEG;
    private boolean streamQualities;

    private ArrayList<MbtEEGFeatures.Feature> featuresToStream;

    private SynchronisationConfig(@NonNull String ipAddress, int port, boolean streamRawEEG, boolean streamQualities, ArrayList<MbtEEGFeatures.Feature> featuresToStream){

        if(ipAddress == null || ipAddress.isEmpty() || port < 0)
            throw new IllegalArgumentException("Impossible to stream data to a null or empty IP address");

        this.ipAddress = ipAddress;
        this.port = port;
        this.streamRawEEG = streamRawEEG;
        this.streamQualities= streamQualities;
        this.featuresToStream = featuresToStream;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public boolean streamRawEEG() {
        return streamRawEEG;
    }

    public boolean streamQualities() {
        return streamQualities;
    }

    public ArrayList<MbtEEGFeatures.Feature> getFeaturesToStream() {
        return featuresToStream;
    }

    /**
     * Specify the IP address, port and element(feature or EEG) to stream
     */
    @Keep
    public static class Builder{

        private String ipAddress = "";

        private int port = 8000;

        private boolean streamRawEEG = false;
        private boolean streamQualities = false;


        private ArrayList<MbtEEGFeatures.Feature> featuresToStream = new ArrayList<>();

        public Builder() {
        }

        public Builder streamRawEEG(){
            this.streamRawEEG = true;
            return this;
        }

        public Builder streamQualities(){
            this.streamQualities = true;
            return this;
        }

        /**
         * Stream all the available features that are related to the input frequencies
         * @param frequenciesToStream frequencies to stream
         * @return
         */
        public Builder streamFrequencyFeatures(MbtEEGFeatures.Frequency... frequenciesToStream){
            final int nbFeaturePerFrequency = 4;
            for (int feature = 0; feature < frequenciesToStream.length*nbFeaturePerFrequency ; feature++) {
                this.featuresToStream.add(frequenciesToStream[feature].getRatio());
                this.featuresToStream.add(frequenciesToStream[feature].getPower());
                this.featuresToStream.add(frequenciesToStream[feature].getLogPower());
                this.featuresToStream.add(frequenciesToStream[feature].getNormalizedPower());
            }
            return this;
        }

        /**
         * Stream specific features
         * @param featuresToStream
         * @return
         */
        public Builder streamFeatures(MbtEEGFeatures.Feature... featuresToStream){
            this.featuresToStream.addAll(Arrays.asList(featuresToStream));
            return this;
        }
        /**
         * Stream specific feature
         * @param featureToStream
         * @return
         */
        public Builder streamFeature(MbtEEGFeatures.Feature featureToStream){
            this.featuresToStream.add(featureToStream);
            return this;
        }

        public Builder ipAddress(String ipAddress){
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder port(int port){
            this.port = port;
            return this;
        }

        @Nullable
        public SynchronisationConfig create(){
            if(featuresToStream.isEmpty())
                featuresToStream = null;
            return new SynchronisationConfig(ipAddress, port, streamRawEEG, streamQualities, featuresToStream);
        }
    }
}
