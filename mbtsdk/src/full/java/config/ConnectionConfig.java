package config;

import android.support.annotation.IntRange;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import command.BluetoothCommands;
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

    private String deviceName;

    private String deviceQrCode;

    private int maxScanDuration;

    private boolean connectAudio;

    private MbtDeviceType deviceType;

    /**
     *  Maximum Transmission Unit
     *  is the maximum size of the data packets
     *  sent by the headset to the SDK.
     */
    @IntRange(from = BluetoothCommands.Mtu.MINIMUM,to = BluetoothCommands.Mtu.MAXIMUM)
    private int mtu;

    private final ConnectionStateListener<BaseError> connectionStateListener;

    private ConnectionConfig(String deviceName, String deviceQrCode, int maxScanDuration, boolean connectAudio, MbtDeviceType deviceType, int mtu, ConnectionStateListener<BaseError> connectionStateListener){
        this.deviceName = deviceName;
        this.deviceQrCode = deviceQrCode;
        this.maxScanDuration = maxScanDuration;
        this.deviceType = deviceType;
        this.connectAudio = (deviceType == MbtDeviceType.MELOMIND && connectAudio);
        this.connectionStateListener = connectionStateListener;
        this.mtu = mtu;
    }

    /**
     * @return the user entered device name. Might be null
     */
    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceQrCode() {
        return deviceQrCode;
    }

    public int getMaxScanDuration() {
        return maxScanDuration;
    }

    public int getMtu(){
        return mtu;
    }


    /**
     * Check the validity of the configured MTU
     * @return true if the MTU is included between
     * {@link BluetoothCommands.Mtu#MINIMUM} and {@link BluetoothCommands.Mtu#MAXIMUM}
     * Return false otherwise.
     */
    public boolean isMtuValid(){
        return new BluetoothCommands.Mtu(mtu).isValid();
    }

    /**
     * Check the validity of the configured Bluetooth scanning duration
     * @return true if the duration is higher than
     * {@link MbtFeatures#MIN_SCAN_DURATION}
     * Return false otherwise.
     */
    public boolean isScanDurationValid(){
        return maxScanDuration >= MbtFeatures.MIN_SCAN_DURATION;
    }

    /**
     * Check the validity of the configured QR code
     * @return true if the QR code is :
     * - null (looking for any device, no matter its name)
     * - or not null and its length matchs the expected length
     */
    public boolean isDeviceQrCodeValid(){
        return deviceQrCode == null ||  (deviceQrCode.length() == MbtFeatures.DEVICE_QR_CODE_LENGTH || deviceQrCode.length() == MbtFeatures.DEVICE_QR_CODE_LENGTH-1);
    }

    /**
     * Check the validity of the configured name
     * @return true if the QR code is
     * - null (looking for any device, no matter its QR code)
     * - or not null and its length matchs the expected length
     */
    public boolean isDeviceNameValid(MbtDeviceType type){
        if(type == MbtDeviceType.VPRO)
            return true; //TODO

        return deviceName == null || deviceName.length() == MbtFeatures.DEVICE_NAME_LENGTH;
    }

    /**
     * By default, Bluetooth connection is only initiated for Data streaming but not for the Audio streaming
     */
    public boolean connectAudio() {
        return connectAudio;
    }

    public MbtDeviceType getDeviceType() {
        return deviceType;
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }

    public void setDeviceName(String deviceName){
        this.deviceName = deviceName;
    }

    public void setDeviceQrCode(String deviceQrCode) {
        this.deviceQrCode = deviceQrCode;
    }

    public void setMaxScanDuration(int maxScanDuration) {
        this.maxScanDuration = maxScanDuration;
    }

    public void setConnectAudio(boolean connectAudio) {
        this.connectAudio = connectAudio;
    }

    public void setDeviceType(MbtDeviceType deviceType) {
        this.deviceType = deviceType;
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

        @IntRange(from = BluetoothCommands.Mtu.MINIMUM, to = BluetoothCommands.Mtu.MAXIMUM)
        private int mtu = BluetoothCommands.Mtu.DEFAULT;

        private boolean connectAudio = false;
        private MbtDeviceType deviceType = MbtDeviceType.MELOMIND;
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
         * Use this method to specify the QR Code number of the device you are trying to connect to.
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
         *  Maximum Transmission Unit
         *  is the maximum size of the data packets
         *  sent by the headset to the SDK.
         *  Enter a value included between {@link BluetoothCommands.Mtu#MINIMUM} and {@link BluetoothCommands.Mtu#MAXIMUM}
         *  Enter a value included between {@link BluetoothCommands.Mtu#MINIMUM} and {@link BluetoothCommands.Mtu#MAXIMUM}
         */
        @NonNull
        public Builder mtu(@IntRange(from = BluetoothCommands.Mtu.MINIMUM, to = BluetoothCommands.Mtu.MAXIMUM) int mtu){
            this.mtu = mtu;
            //BluetoothCommand command = new BluetoothCommands.Mtu(mtu);
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
         * Use this method to force audio connection to a device. This is automatically set to Boolean#FALSE
         * if the device is not a melomind.
         *
         * If the device is a melomind, then the flag will tell either or not the SDK has to connect the audio bluetooth.
         * <p>Caution, the audio is handled by the Android system itself and is not meant to be connect via a third party application.
         * If set to Boolean#TRUE, the connection attempt may fail. It is still possible to connect to audio through the system settings of your android device.</p>
         *
         * @return the builder instance
         */
        @NonNull
        public Builder connectAudioIfDeviceCompatible(){
            this.connectAudio = true;
            return this;
        }

        /**
         * Use this method to define which kind of device you want to connect to.
         * @see MbtDeviceType
         * @param deviceType
         * @return the builder instance
         */
        @NonNull
        public Builder scanDeviceType(MbtDeviceType deviceType){
            this.deviceType = deviceType;
            return this;
        }

        @NonNull
        public ConnectionConfig create(){
            return new ConnectionConfig(this.deviceName, this.deviceQrCode, this.maxScanDuration, this.connectAudio, this.deviceType, this.mtu, this.connectionStateListener);
        }


    }

}
