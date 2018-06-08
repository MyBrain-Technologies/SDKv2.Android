package config;

import android.support.annotation.IntRange;
import android.support.annotation.Keep;

@Keep
public class DeviceConfig {
    private FilterConfig notchFilter;
    private FilterConfig bandpassFilter;
    private AmpGainConfig gainValue;
    private int mtuValue;
    private boolean useP300;


    DeviceConfig(FilterConfig notchFilter, FilterConfig bandpassFilter, AmpGainConfig gainValue, int mtuValue, boolean useP300) {
        this.notchFilter = notchFilter;
        this.bandpassFilter = bandpassFilter;
        this.gainValue = gainValue;
        this.mtuValue = mtuValue;
        this.useP300 = useP300;
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
    public static class Builder{
        FilterConfig notchFilter = null;
        FilterConfig bandpassFilter = null;
        AmpGainConfig gainValue = null;
        int mtuValue = -1;
        boolean useP300;

        public Builder(){}

        public Builder notchFilter(FilterConfig value){
            this.notchFilter = value;
            return this;
        }

        public Builder bandpassFilter(FilterConfig value){
            this.bandpassFilter = value;
            return this;
        }

        public Builder gain(AmpGainConfig value){
            this.gainValue = value;
            return this;
        }

        public Builder mtu(@IntRange(from=-1,to=121) int value){
            this.mtuValue = value;
            return this;
        }

        public Builder useP300(boolean useP300){
            this.useP300 = useP300;
            return this;
        }

        public DeviceConfig createStreamConfig() {
            return new DeviceConfig(notchFilter, bandpassFilter, gainValue, mtuValue, useP300);
        }
    }

}
