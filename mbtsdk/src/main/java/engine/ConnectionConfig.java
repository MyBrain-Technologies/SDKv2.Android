package engine;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import engine.clientevents.ConnectionStateListener;
import features.MbtFeatures;
import features.ScannableDevices;

/**
 * This class aims at configuring the bluetooth connection to a myBrain device. It contains user configurable
 * parameters to specify how the connection step is supposed to happen.
 *
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
public final class ConnectionConfig {

    private final String deviceName;

    private final int maxScanDuration;

    private final int connectionTimeout;

    private final boolean connectAudio;

    private final ScannableDevices deviceType;

    private final ConnectionStateListener connectionStateListener;

    private ConnectionConfig(String deviceName, int maxScanDuration, int connectionTimeout, boolean connectAudio, ScannableDevices deviceType, ConnectionStateListener connectionStateListener){
        this.deviceName = deviceName;
        this.maxScanDuration = maxScanDuration;
        this.connectionTimeout = connectionTimeout;
        this.deviceType = deviceType;
        this.connectAudio = (deviceType == ScannableDevices.MELOMIND && connectAudio);
        this.connectionStateListener = connectionStateListener;
    }

    /**
     * @return the user entered device name. Might be null
     */
    public String getDeviceName() {
        return deviceName;
    }


    public int getMaxScanDuration() {
        return maxScanDuration;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public boolean isConnectAudio() {
        return connectAudio;
    }

    public ScannableDevices getDeviceType() {
        return deviceType;
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }

    /**
     * Builder class to ease construction of the {@link ConnectionConfig} instance.
     */
    public static class Builder{
        @Nullable
        private String deviceName = null;
        private int maxScanDuration = MbtFeatures.DEFAULT_MAX_SCAN_DURATION_IN_MILLIS;
        private int connectionTimeout = MbtFeatures.DEFAULT_MAX_CONNECTION_DURATION_IN_MILLIS;
        private boolean connectAudio = false;
        private ScannableDevices deviceType = ScannableDevices.ALL;
        @NonNull
        private final ConnectionStateListener connectionStateListener;


        public Builder(@NonNull ConnectionStateListener stateListener){
            this.connectionStateListener = stateListener;
        }

        /**
         * Use this method to specify the name of the device you are trying to connect to.
         * Pass NULL if unknown or if you don't want to specify any. This tells the SDK to connect to the first found device matching other criteria.
         * @param deviceName the device name. Can be NULL
         * @return the builder instance
         */
        @NonNull
        public Builder deviceName(@Nullable String deviceName){
            this.deviceName = deviceName;
            return this;
        }

        /**
         * Use this method to specify a custom scan duration in <b>milliseconds</b>. By default, it is set to {{@link MbtFeatures#DEFAULT_MAX_SCAN_DURATION_IN_MILLIS}}.
         * When a scan is starting, the user is notified with the state {@link core.bluetooth.BtState#SCAN_STARTED}
         * If the maximum duration is reached without being able to find any device, the user is notified with the state
         * {@link core.bluetooth.BtState#SCAN_TIMEOUT}
         *
         * @param maxScanDurationInMillis the new maximum duration in milliseconds
         * @return the builder instance
         */
        @NonNull
        public Builder maxScanDuration(int maxScanDurationInMillis){
            this.maxScanDuration = maxScanDurationInMillis;
            return this;
        }

        /**
         * Unused at the moment
         * */
//        public Builder connectionTimeout(int connectionTimeoutInMillis){
//            this.connectionTimeout = connectionTimeoutInMillis;
//            return this;
//        }

        /**
         * Use this method to force audio connection to a device. This is automatically set to {@link Boolean#FALSE}
         * if the device is not a melomind.
         *
         * If the device is a melomind, then the flag will tell either or not the SDK has to connect the audio bluetooth.
         * <p>Caution, the audio is handled by the Android system itself and is not meant to be connect via a third party application.
         * If set to {@link Boolean#TRUE}, the connection attempt may fail. It is still possible to connect to audio through the system settings of your android device.</p>
         *
         * @param shouldConnectAudio true to connect automatically, false otherwise. If the device is not audio compatible, the flag is forced to false.
         * @return the builder instance
         */
        @NonNull
        public Builder connectAudioIfDeviceCompatible(boolean shouldConnectAudio){
            this.connectAudio = shouldConnectAudio;
            return this;
        }

        /**
         * Use this method to define which king of device you want to connect to.
         * @see ScannableDevices
         * @param deviceType
         * @return the builder instance
         */
        @NonNull
        public Builder scanDeviceType(ScannableDevices deviceType){
            this.deviceType = deviceType;
            return this;
        }

        @Nullable
        public ConnectionConfig create(){
            return new ConnectionConfig(this.deviceName, this.maxScanDuration, this.connectionTimeout, this.connectAudio, this.deviceType, this.connectionStateListener);
        }


    }

}
