package config;

import android.support.annotation.IntRange;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import engine.clientevents.DeviceStatusListener;

@Keep
public class DeviceConfig {
    private FilterConfig notchFilter;
    private FilterConfig bandpassFilter;
    private AmpGainConfig gainValue;
    private int mtuValue;
    private boolean useP300;
    private boolean enableDcOffset;

    public final static String MTU_CONFIG = "MTU";
    public final static String AMP_GAIN_CONFIG = "GAIN";
    public final static String NOTCH_FILTER_CONFIG = "NOTCH";
    public final static String P300_CONFIG = "P300";
    public final static String OFFSET_CONFIG = "OFFSET";
    public final static String EEG_CONFIG = "EEG";

    private DeviceStatusListener deviceStatusListener;

    private DeviceConfig(FilterConfig notchFilter, FilterConfig bandpassFilter, AmpGainConfig gainValue, int mtuValue, boolean useP300, boolean enableDcOffset, DeviceStatusListener deviceStatusListener) {
        this.notchFilter = notchFilter;
        this.bandpassFilter = bandpassFilter;
        this.gainValue = gainValue;
        this.mtuValue = mtuValue;
        this.useP300 = useP300;
        this.enableDcOffset = enableDcOffset;
        this.deviceStatusListener = deviceStatusListener;
    }

    @Keep
    public FilterConfig getNotchFilter() {
        return notchFilter;
    }

    @Keep
    public FilterConfig getBandpassFilter() {
        return bandpassFilter;
    }

    @Keep
    public AmpGainConfig getGainValue() {
        return gainValue;
    }

    @Keep
    public int getMtuValue() {
        return mtuValue;
    }

    @Keep
    public boolean isUseP300() {
        return useP300;
    }

    @Keep
    public boolean isDcOffsetEnabled() {
        return enableDcOffset;
    }

    public DeviceStatusListener getDeviceStatusListener() {
        return deviceStatusListener;
    }

    @Keep
    public static class Builder{
        @Nullable
        FilterConfig notchFilter = null;
        @Nullable
        FilterConfig bandpassFilter = null;
        @Nullable
        AmpGainConfig gainValue = null;
        int mtuValue = -1;
        boolean useP300;
        boolean enableDcOffset;
        @Nullable
        DeviceStatusListener deviceStatusListener = null;

        public Builder(){}

        @NonNull
        public Builder notchFilter(FilterConfig value){
            this.notchFilter = value;
            return this;
        }

        @NonNull
        public Builder bandpassFilter(FilterConfig value){
            this.bandpassFilter = value;
            return this;
        }

        @NonNull
        public Builder gain(AmpGainConfig value){
            this.gainValue = value;
            return this;
        }

        @NonNull
        public Builder mtu(@IntRange(from=-1,to=121) int value){
            this.mtuValue = value;
            return this;
        }

        @NonNull
        public Builder useP300(boolean useP300){
            this.useP300 = useP300;
            return this;
        }

        @NonNull
        public Builder enableDcOffset(boolean enableDcOffset){
            this.enableDcOffset = enableDcOffset;
            return this;
        }

        /**
         * Use this method if you want to monitor headset's electrodes saturation and eeg offset.
         * Set it to null if unnecesary.
         *
         * <p>It is by default set to NULL</p>
         *
         * @param deviceStatusListener the device status listener
         * @return the device instance
         */
        @NonNull
        public Builder listenToDeviceStatus(DeviceStatusListener deviceStatusListener){
            this.deviceStatusListener = deviceStatusListener;
            return this;
        }


        @Nullable
        public DeviceConfig create() {
            return new DeviceConfig(notchFilter, bandpassFilter, gainValue, mtuValue, useP300, enableDcOffset, deviceStatusListener);
        }
    }

    @Override
    public String toString() {
        return "DeviceConfig{" +
                "notchFilter=" + notchFilter +
                ", bandpassFilter=" + bandpassFilter +
                ", gainValue=" + gainValue +
                ", mtuValue=" + mtuValue +
                ", useP300=" + useP300 +
                ", enableDcOffset=" + enableDcOffset +
                '}';
    }
}
