package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import core.eeg.storage.Feature;
import core.eeg.storage.FrequencyBand;

@Keep
public interface SynchronisationConfig {

    @Keep
    abstract class AbstractConfig {

    private String ipAddress;

    private boolean streamRawEEG;
    private boolean streamQualities;

    private HashSet<Feature> featuresToStream;

    AbstractConfig(@NonNull String ipAddress, boolean streamRawEEG, boolean streamQualities, HashSet<Feature> featuresToStream) {

        if (ipAddress == null || ipAddress.isEmpty())
            throw new IllegalArgumentException("Impossible to stream data to a null or empty IP address");

        this.ipAddress = ipAddress;
        this.streamRawEEG = streamRawEEG;
        this.streamQualities = streamQualities;
        this.featuresToStream = featuresToStream;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public boolean streamRawEEG() {
        return streamRawEEG;
    }

    public boolean streamQualities() {
        return streamQualities;
    }

    public HashSet<Feature> getFeaturesToStream() {
        return featuresToStream;
    }

        @Override
        public String toString() {
            return "SynchronisationConfig{" +
                    "ipAddress='" + ipAddress + '\'' +
                    ", streamRawEEG=" + streamRawEEG +
                    ", streamQualities=" + streamQualities +
                    ", featuresToStream=" + featuresToStream +
                    '}';
        }

        /**
     * Specify the IP address, port and element(feature or EEG) to stream
     */
    @Keep
    public static abstract class Builder<B extends Builder<B>> {

        abstract B self();

        String ipAddress = "";

        boolean streamRawEEG = false;
        boolean streamQualities = false;

        HashSet<Feature> featuresToStream = new HashSet<>();

        public B streamRawEEG() {
            this.streamRawEEG = true;
            return self();
        }

        public B streamQualities() {
            this.streamQualities = true;
            return self();
        }

        /**
         * Stream all the available features that are related to the input frequencies
         * @param frequenciesToStream frequencies to stream
         * @return
         */
        public B streamFrequencyBandFeatures(FrequencyBand... frequenciesToStream) {
            final int nbFeaturePerFrequency = 4;
            for (int feature = 0; feature < frequenciesToStream.length * nbFeaturePerFrequency; feature++) {
                this.featuresToStream.add(frequenciesToStream[feature].getRatio());
                this.featuresToStream.add(frequenciesToStream[feature].getPower());
                this.featuresToStream.add(frequenciesToStream[feature].getLogPower());
                this.featuresToStream.add(frequenciesToStream[feature].getNormalizedPower());
                this.featuresToStream.add(frequenciesToStream[feature].getMaximum());
                this.featuresToStream.add(frequenciesToStream[feature].getKurtosis());
                this.featuresToStream.add(frequenciesToStream[feature].getStandardDeviation());
                this.featuresToStream.add(frequenciesToStream[feature].getSkewness());
            }
            return self();
        }

        /**
         * Stream specific features
         *
         * @param featuresToStream
         * @return
         */
        public B streamFeatures(Feature... featuresToStream) {
            this.featuresToStream.addAll(Arrays.asList(featuresToStream));
            return self();
        }

        /**
         * Stream specific feature
         * @param featureToStream
         * @return
         */
        public B streamFeature(Feature featureToStream) {
            this.featuresToStream.add(featureToStream);
            return self();
        }

        public B ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return self();
        }

        public abstract AbstractConfig create();
    }
}

    @Keep
    final class OSC extends AbstractConfig {

        private int port;

        OSC(@NonNull String ipAddress, int port, boolean streamRawEEG, boolean streamQualities, HashSet<Feature> featuresToStream) {
            super(ipAddress, streamRawEEG, streamQualities, featuresToStream);
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        @Keep
        public static class Builder extends AbstractConfig.Builder<Builder>{

            int port = 8000;

            public Builder port(int port){
                this.port = port;
                return self();
            }

            @Override
            Builder self() {
                return this;
            }

            @Override
            public OSC create() {
                if(featuresToStream.isEmpty())
                    featuresToStream = null;

                return new OSC(super.ipAddress, port, streamRawEEG, streamQualities, featuresToStream);
            }
        }
    }

    @Keep
    final class LSL extends AbstractConfig {

        LSL(@NonNull String ipAddress, boolean streamRawEEG, boolean streamQualities, HashSet<Feature> featuresToStream) {
            super(ipAddress, streamRawEEG, streamQualities, featuresToStream);
        }

        @Keep
        public static class Builder extends AbstractConfig.Builder<Builder>{

            @Override
            Builder self() {
                return this;
            }

            @Override
            public LSL create() {
                if(featuresToStream.isEmpty())
                    featuresToStream = null;

                return new LSL(super.ipAddress, streamRawEEG, streamQualities, featuresToStream);
            }
        }
    }

}
