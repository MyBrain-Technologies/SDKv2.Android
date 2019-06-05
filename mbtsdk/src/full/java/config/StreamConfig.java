package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import core.bluetooth.lowenergy.DeviceCommand;
import core.bluetooth.lowenergy.DeviceCommands;
import core.bluetooth.lowenergy.DeviceStreamingCommands;
import core.eeg.storage.MbtEEGPacket;
import engine.clientevents.BaseError;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegListener;
import features.MbtFeatures;

/**
 * This class aims at configuring the stream process. It contains user configurable
 * parameters to specify how the streaming is going to be.
 *
 * <p>Use the {@link Builder} class to instanciate this.</p>
 */
@Keep
public final class StreamConfig {

    private int notificationPeriod;

    private final EegListener<BaseError> eegListener;

    private DeviceStatusListener<BaseError> deviceStatusListener;

    private boolean computeQualities;

    private ArrayList<DeviceCommand> deviceCommands;

    private StreamConfig(boolean computeQualities, EegListener<BaseError> eegListener, DeviceStatusListener<BaseError> deviceStatusListener, int notificationPeriod, DeviceStreamingCommands[] deviceCommands){
        this.computeQualities = computeQualities;
        this.eegListener = eegListener;
        this.deviceStatusListener = deviceStatusListener;
        this.notificationPeriod = notificationPeriod;
        this.deviceCommands = new ArrayList<>();
        for (DeviceStreamingCommands deviceCommand : deviceCommands){
            this.deviceCommands.add((DeviceCommand)deviceCommand);
        }
    }

    public EegListener getEegListener() {
        return eegListener;
    }

    public DeviceStatusListener<BaseError> getDeviceStatusListener() {
        return deviceStatusListener;
    }

    public int getNotificationPeriod() {
        return notificationPeriod;
    }

    public boolean shouldComputeQualities() {
        return computeQualities;
    }

    public ArrayList<DeviceCommand> getDeviceCommands() {
        return deviceCommands;
    }

    public void setNotificationPeriod(int notificationPeriod) {
        this.notificationPeriod = notificationPeriod;
    }

    public void setComputeQualities(boolean computeQualities) {
        this.computeQualities = computeQualities;
    }

    public void setDeviceCommand(ArrayList<DeviceCommand> deviceCommands) {
        this.deviceCommands = deviceCommands;
    }

    public void setDeviceStatusListener(DeviceStatusListener<BaseError> deviceStatusListener) {
        this.deviceStatusListener = deviceStatusListener;
    }

    /**
     * Builder class to ease construction of the {@link StreamConfig} instance.
     */
    @Keep
    public static class Builder{

        private int notificationPeriod = MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD;

        @NonNull
        private final EegListener<BaseError> eegListener;

        @Nullable
        private DeviceStatusListener<BaseError> deviceStatusListener;

        private boolean computeQualities = false;

        private DeviceStreamingCommands[] deviceCommands;

        /**
         * The eeg Listener is mandatory.
         */
        public Builder(@NonNull EegListener<BaseError> eegListener){
            this.eegListener = eegListener;
        }

        /**
         * Says whether or not the qualities are automatically computed while streaming EEG.
         * This requires some specific configuration: One complete period of EEG acquisition is mandatory, ie:
         * at least {@link MbtFeatures#DEFAULT_SAMPLE_RATE} values are mandatory to compute qualities. It can be simply seen as
         * one second of data.
         *
         * <p>The minimum notification period will be automatically set to 1000ms if qualities are enabled.</p>
         * <p>If the input {@link #notificationPeriod} is set by the user to less than 1000ms, the {@link engine.clientevents.ConfigError#ERROR_INVALID_PARAMS} error will be thrown</p>
         * @return the builder instance
         */
        public Builder useQualities(){
            this.computeQualities = true;
            return this;
        }

        /**
         * Configures optional parameters applied by the headset for the EEG signal acquisition
         * such as the amplifier gain, the notch filter, the MTU, etc.
         * Default values can be found in the user guide for the gain, filters, MTU
         * if you do not specify these parameters in the {@link StreamConfig] Builder.
         * @param deviceAcquisitionConfig bundles the configuration parameters
         */
        public Builder configureAcquisitionFromDeviceCommand(DeviceStreamingCommands... deviceCommands){
            this.deviceCommands = deviceCommands;
            return this;
        }

        /**
         * Use this method to specify how much eeg you want to receive in the {@link EegListener#onNewPackets(MbtEEGPacket)} method.
         *
         * <p>Warning, the duration is based on a quantity of eeg data. This quantity depends on the sampling frequency of the device.
         * It is by default {@link MbtFeatures#DEFAULT_SAMPLE_RATE} in Hertz.
         * The specified period is in any case a fixed duration based on a timer. This means that you may be notified with a certain latency.
         *</p>
         *
         * <p>For better performances, it is HIGHLY recommended that the period is a multiple of the sampling period.
         * For example, if the sampling frequency is 250Hz, then the sampling period is 4ms.
         * It means that the value must be a multiple of 4ms.</p>
         *
         * <p>It is by default set to {@link MbtFeatures#DEFAULT_CLIENT_NOTIFICATION_PERIOD}</p>
         * @param periodInMillis the period in milliseconds
         * @return the builder instance
         */
        @NonNull
        public Builder setNotificationPeriod(int periodInMillis){
            this.notificationPeriod = periodInMillis;
            return this;
        }

        @NonNull
        public Builder setDeviceStatusListener(DeviceStatusListener<BaseError> deviceStatusListener){
            this.deviceStatusListener = deviceStatusListener;
            return this;
        }

        @Nullable
        public StreamConfig create(){
            return new StreamConfig(this.computeQualities, this.eegListener, this.deviceStatusListener, this.notificationPeriod, this.deviceCommands);
        }
    }

    /**
     * Checks if the configuration parameters are correct
     * @return true is the configuration is correct, false otherwise
     */
    public boolean isConfigCorrect() {
        if(this.notificationPeriod <  (this.computeQualities ? MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS : MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS))
            return false;
        else if(notificationPeriod >  (this.computeQualities ? MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS : MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS))
            return false;

        return true;
    }

}
