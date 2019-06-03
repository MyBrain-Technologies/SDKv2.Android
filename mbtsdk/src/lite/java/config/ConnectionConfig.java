package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import engine.clientevents.BaseError;
import engine.clientevents.ConnectionStateListener;
import features.MbtDeviceType;
import features.MbtFeatures;

/**
 * This class aims at configuring the bluetooth connection to a myBrain device. It contains user configurable
 * parameters to specify how the connection step is supposed to happen.
 *
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
@Keep
public final class ConnectionConfig {

    private final String deviceName;

    private final String deviceQrCode;

    private final int maxScanDuration;

    private final boolean connectAudio;

    private final MbtDeviceType deviceType = MbtDeviceType.MELOMIND;

    private final ConnectionStateListener<BaseError> connectionStateListener;

    private ConnectionConfig(String deviceName,  String deviceQrCode, int maxScanDuration, boolean connectAudio, ConnectionStateListener<BaseError> connectionStateListener){
        this.deviceName = deviceName;
        this.deviceQrCode = deviceQrCode;
        this.maxScanDuration = maxScanDuration;
        this.connectAudio = (deviceType == MbtDeviceType.MELOMIND && connectAudio);
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

    public String getDeviceQrCode() {
        return deviceQrCode;
    }

    /**
     * By default, Bluetooth connection is only initiated for Data streaming but not for the Audio streaming
     */
    public boolean useAudio() {
        return connectAudio;
    }

    public MbtDeviceType getDeviceType() {
        return deviceType;
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }

    /**
     * Builder class to ease construction of the {@link ConnectionConfig} instance.
     */
    @Keep
    public static class Builder{
        @Nullable
        private String deviceName = null;
        private String deviceQrCode = null;
        private int maxScanDuration = MbtFeatures.DEFAULT_MAX_SCAN_DURATION_IN_MILLIS;
        private boolean connectAudio = false;
        @NonNull
        private final ConnectionStateListener<BaseError> connectionStateListener;


        public Builder(@NonNull ConnectionStateListener<BaseError> connectionStateListener){
            this.connectionStateListener = connectionStateListener;
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
         * Use this method to specify the QR code number of the device you are trying to connect to.
         * The QR code must contain 9 or 10 digits and start with the {@link core.device.model.MelomindsQRDataBase#QR_PREFIX} prefix
         * Pass NULL if unknown or if you don't want to specify any.
         * @param deviceQrCode the device QR code number. Can be NULL
         * @return the builder instance
         */
        @NonNull
        public Builder deviceQrCode(@Nullable String deviceQrCode){
            this.deviceQrCode = deviceQrCode;
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
        public Builder connectAudio(){
            this.connectAudio = true;
            return this;
        }

        @NonNull
        public ConnectionConfig create(){
            return new ConnectionConfig(this.deviceName, this.deviceQrCode, this.maxScanDuration, this.connectAudio, this.connectionStateListener);
        }


    }

}
