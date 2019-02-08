package engine;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import engine.clientevents.ConnectionStateReceiver;
import features.MbtFeatures;
import features.ScannableDevices;

/**
 * This class aims at configuring the bluetooth connection to a myBrain device. It contains user configurable
 * parameters to specify how the connection step is supposed to happen.
 *
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
@Keep
public final class ConnectionConfig {

    private final String deviceName;

    private final int maxScanDuration;

    private final int maxConnectionDuration;

    private final boolean connectAudio;

    private final ScannableDevices deviceType;

    private final ConnectionStateReceiver connectionStateReceiver;

    private ConnectionConfig(String deviceName, int maxScanDuration, int connectionTimeout, boolean connectAudio, ScannableDevices deviceType, ConnectionStateReceiver connectionStateReceiver){
        this.deviceName = deviceName;
        this.maxScanDuration = maxScanDuration;
        this.maxConnectionDuration = connectionTimeout;
        this.deviceType = deviceType;
        this.connectAudio = (deviceType == ScannableDevices.MELOMIND && connectAudio);
        this.connectionStateReceiver = connectionStateReceiver;
    }

    /**
     * @return the user entered device name. Might be null
     */
    public String getDeviceName() {
        return deviceName;
    }


    int getMaxScanDuration() {
        return maxScanDuration;
    }

    public int getMaxConnectionDuration() {
        return maxConnectionDuration;
    }

    /**
     * By default, Bluetooth connection is only initiated for Data streaming but not for the Audio streaming
     */
    boolean useAudio() {
        return connectAudio;
    }

    ScannableDevices getDeviceType() {
        return deviceType;
    }

    ConnectionStateReceiver getConnectionStateReceiver() {
        return connectionStateReceiver;
    }

    /**
     * Builder class to ease construction of the {@link ConnectionConfig} instance.
     */
    @Keep
    public static class Builder{
        @Nullable
        private String deviceName = null;
        private int maxScanDuration = MbtFeatures.DEFAULT_MAX_SCAN_DURATION_IN_MILLIS;
        private int maxConnectionDuration = MbtFeatures.DEFAULT_MAX_CONNECTION_DURATION_IN_MILLIS;
        private boolean connectAudio = false;
        private ScannableDevices deviceType = ScannableDevices.ALL;
        @NonNull
        private final ConnectionStateReceiver connectionStateReceiver;


        public Builder(@NonNull ConnectionStateReceiver connectionStateReceiver){
            this.connectionStateReceiver = connectionStateReceiver;
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
         * <p>Warning, minimum input must be equal to 10000ms</p>
         *
         * @param maxScanDurationInMillis the new maximum duration in milliseconds
         * @return the builder instance
         */
        @NonNull
        public Builder maxScanDuration(int maxScanDurationInMillis){
            this.maxScanDuration = maxScanDurationInMillis;
            return this;
        }

        @NonNull
        public Builder maxConnectionDuration(int maxDurationInMillis){
            this.maxConnectionDuration = maxDurationInMillis;
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
         * @param useAudio true to connect automatically, false otherwise. If the device is not audio compatible, the flag is forced to false.
         * @return the builder instance
         */
        @NonNull
        public Builder connectAudioIfDeviceCompatible(boolean useAudio){
            this.connectAudio = useAudio;
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

        @NonNull
        public ConnectionConfig create(){
            return new ConnectionConfig(this.deviceName, this.maxScanDuration, this.maxConnectionDuration, this.connectAudio, this.deviceType, this.connectionStateReceiver);
        }


    }

}
