package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedList;

import command.DeviceCommand;
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

    //private final long streamDuration; //For later use

    private final int notificationPeriod;

    private final EegListener<BaseError> eegListener;

    private final boolean computeQualities;

    private LinkedList<DeviceCommand> deviceCommands;

    private StreamConfig(EegListener<BaseError> eegListener, DeviceStatusListener<BaseError> deviceStatusListener,int notificationPeriod){
        this.computeQualities = false;
        this.eegListener = eegListener;
        this.notificationPeriod = notificationPeriod;
        this.deviceStatusListener = deviceStatusListener;
        this.deviceCommands = null;
    }
    private DeviceStatusListener<BaseError> deviceStatusListener;

    public EegListener getEegListener() {
        return eegListener;
    }

    public int getNotificationPeriod() {
        return notificationPeriod;
    }

    public boolean shouldComputeQualities() {
        return computeQualities;
    }

    public DeviceStatusListener<BaseError> getDeviceStatusListener() {
        return deviceStatusListener;
    }

    public LinkedList<DeviceCommand> getDeviceCommands() {
        return deviceCommands;
    }

    /**
     * Builder class to ease construction of the {@link StreamConfig} instance.
     */
    @Keep
    public static class Builder{

        private int notificationPeriod = MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD;

        @Nullable
        private DeviceStatusListener<BaseError> deviceStatusListener;

        @NonNull
        private final EegListener<BaseError> eegListener;

        /**
         * The eeg Listener is mandatory.
         */
        public Builder(@NonNull EegListener<BaseError> eegListener){
            this.eegListener = eegListener;
        }


        @NonNull
        public Builder setDeviceStatusListener(DeviceStatusListener<BaseError> deviceStatusListener){
            this.deviceStatusListener = deviceStatusListener;
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

        @Nullable
        public StreamConfig create(){
            return new StreamConfig(this.eegListener, this.deviceStatusListener, this.notificationPeriod);
        }
    }

    /**
     * Checks if the configuration parameters are correct
     * @return true is the configuration is correct, false otherwise
     */
    public boolean isConfigCorrect() {
        if(this.notificationPeriod <  MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS)
            return false;
        else if(notificationPeriod >  MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS)
            return false;

        return true;
    }

}
