package engine;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.eeg.storage.MbtEEGPacket;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EEGException;
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

    private final int notificationPeriod;

    private final EegListener<EEGException> eegListener;

    private final DeviceStatusListener deviceStatusListener;

    private final boolean computeQualities;

    private StreamConfig(boolean computeQualities, EegListener<EEGException> eegListener, int notificationPeriod, DeviceStatusListener deviceStatusListener){
        this.computeQualities = computeQualities;
        this.eegListener = eegListener;
        this.notificationPeriod = notificationPeriod;
        this.deviceStatusListener = deviceStatusListener;
    }

    public DeviceStatusListener getDeviceStatusListener() {
        return deviceStatusListener;
    }

    public EegListener getEegListener() {
        return eegListener;
    }

    public int getNotificationPeriod() {
        return notificationPeriod;
    }

    public boolean shouldComputeQualities() {
        return computeQualities;
    }


    /**
     * Builder class to ease construction of the {@link StreamConfig} instance.
     */
    @Keep
    public static class Builder{
        //long streamDuration = -1L;
        private int notificationPeriod = MbtFeatures.DEFAULT_CLIENT_NOTIFICATION_PERIOD;

        @Nullable
        private DeviceStatusListener deviceStatusListener = null;
        @NonNull
        private final EegListener<EEGException> eegListener;

        private boolean computeQualities = false;


        /**
         * The eeg Listener is mandatory.
         * @param eegListener
         */
        public Builder(@NonNull EegListener<EEGException> eegListener){
            this.eegListener = eegListener;
        }

//        public Builder setStreamDuration(long durationInMillis){
//            this.streamDuration = streamDuration;
//            return this;
//        }

        /**
         * Says whether or not the qualities are automatically computed while streaming EEG.
         * This requires some specific configuration: One complete period of EEG acquisition is mandatory, ie:
         * at least {@link MbtFeatures#DEFAULT_SAMPLE_RATE} values are mandatory to compute qualities. It can be simply seen as
         * one second of data.
         *
         * <p>The minimum notification period will be automatically set to 1000ms if qualities are enabled.</p>
         * <p>If the input {@link #notificationPeriod} is set by the user to less than 1000ms, the {@link EEGException#INVALID_PARAMETERS} error will be thrown</p>
         * @param useQualities a flag indicating whether or not the qualities shall be computed
         * @return the builder instance
         */
        public Builder useQualities(boolean useQualities){
            this.computeQualities = useQualities;
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
            return new StreamConfig(this.computeQualities, this.eegListener, this.notificationPeriod, this.deviceStatusListener);
        }
    }

    /**
     * Checks if the configuration parameters are correct
     * @return true is the configuration is correct, false otherwise
     */
    boolean isConfigCorrect() {
        if(this.notificationPeriod <  (this.computeQualities ? MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS : MbtFeatures.MIN_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS))
            return false;
        else if(notificationPeriod >  (this.computeQualities ? MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS : MbtFeatures.MAX_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS))
            return false;

        return true;
    }

}
