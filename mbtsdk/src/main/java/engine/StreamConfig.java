package engine;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import config.DeviceConfig;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegListener;
import features.MbtFeatures;
import features.ScannableDevices;

public final class StreamConfig {

    //private final long streamDuration; //For later use

    private final int notificationPeriod;

    //private final DeviceConfig deviceConfig; Will be used in future release

    private final EegListener eegListener;

    private final DeviceStatusListener deviceStatusListener;

    private StreamConfig(EegListener eegListener, int notificationPeriod, DeviceStatusListener deviceStatusListener){
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

    public static class Builder{
        //long streamDuration = -1L;
        int notificationPeriod = MbtFeatures.DEFAULT_CLIENT_PACKET_SIZE;
        DeviceStatusListener deviceStatusListener = null;
        final EegListener eegListener;



        public Builder(@NonNull EegListener eegListener){
            this.eegListener = eegListener;
        }

//        public Builder setStreamDuration(long durationInMillis){
//            this.streamDuration = streamDuration;
//            return this;
//        }

        public Builder setNotificationPeriod(int periodInMillis){
            this.notificationPeriod = periodInMillis;
            return this;
        }

        public Builder addSaturationAndOffsetListener(@Nullable DeviceStatusListener deviceStatusListener){
            this.deviceStatusListener = deviceStatusListener;
            return this;
        }

        public StreamConfig create(){
            return new StreamConfig(this.eegListener, this.notificationPeriod, this.deviceStatusListener);
        }




    }

}
