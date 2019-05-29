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
    private Boolean useP300;
    private Boolean enableDcOffset;
    private String serialNumber;
    private String a2dpName;
    private String productName;
    private Boolean connectA2DP;
    private Boolean disconnectA2DP;
    private Boolean systemStatus;

    public final static String MTU_CONFIG = "MTU";
    public final static String AMP_GAIN_CONFIG = "GAIN";
    public final static String NOTCH_FILTER_CONFIG = "NOTCH";
    public final static String P300_CONFIG = "P300";
    public final static String OFFSET_CONFIG = "OFFSET";
    public final static String EEG_CONFIG = "EEG";
    public final static String SERIAL_NUMBER_CONFIG = "SERIAL_NUMBER";
    public final static String A2DP_NAME_CONFIG = "A2DP_NAME";
    public final static String PRODUCT_NAME_CONFIG = "PRODUCT_NAME";
    public final static String SYSTEM_STATUS_CONFIG = "SYSTEM_STATUS";
    public final static String CONNECT_A2DP_CONFIG = "CONNECT_A2DP";
    public final static String DISCONNECT_A2DP_CONFIG = "DISCONNECT_A2DP";

    private DeviceStatusListener deviceStatusListener;

    private DeviceConfig(FilterConfig notchFilter, FilterConfig bandpassFilter, AmpGainConfig gainValue, int mtuValue, Boolean useP300, Boolean enableDcOffset, DeviceStatusListener deviceStatusListener, String serialNumber, String productName, String a2dpName, Boolean connectA2D, Boolean disconnectA2DP, Boolean systemStatus) {
        this.notchFilter = notchFilter;
        this.bandpassFilter = bandpassFilter;
        this.gainValue = gainValue;
        this.mtuValue = mtuValue;
        this.useP300 = useP300;
        this.enableDcOffset = enableDcOffset;
        this.deviceStatusListener = deviceStatusListener;
        this.serialNumber = serialNumber;
        this.productName = productName;
        this.a2dpName = a2dpName;
        this.connectA2DP = connectA2D;
        this.disconnectA2DP = disconnectA2DP;
        this.systemStatus = systemStatus;
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
    public Boolean isUseP300() {
        return useP300;
    }

    @Keep
    public Boolean isDcOffsetEnabled() {
        return enableDcOffset;
    }

    public DeviceStatusListener getDeviceStatusListener() {
        return deviceStatusListener;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getProductName() {
        return productName;
    }

    public String getA2dpName() {
        return a2dpName;
    }

    public Boolean connectA2DP() {
        return connectA2DP;
    }

    public Boolean disconnectA2DP() {
        return disconnectA2DP;
    }

    public Boolean getSystemStatus() {
        return systemStatus;
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

        Boolean useP300 = null;
        Boolean enableDcOffset = null;

        @Nullable
        String serialNumber = null;
        @Nullable
        String productName = null;
        @Nullable
        String a2dpName = null;

        Boolean connectA2DP = null;
        Boolean disconnectA2DP = null;
        Boolean systemStatus = null;

        @Nullable
        DeviceStatusListener deviceStatusListener = null;

        public Builder(){}

        @NonNull
        public Builder notchFilter(FilterConfig value){
            this.notchFilter = value;
            return this;
        }

        @NonNull
        public Builder a2dpName(String a2dpName){
            this.a2dpName = a2dpName;
            return this;
        }

        @NonNull
        public Builder productName(String productName){
            this.productName = productName;
            return this;
        }

        @NonNull
        public Builder connectA2DP(boolean connectA2DP){
            this.connectA2DP = connectA2DP;
            return this;
        }

        @NonNull
        public Builder disconnectA2DP(boolean disconnectA2DP){
            this.disconnectA2DP = disconnectA2DP;
            return this;
        }
        @NonNull
        public Builder systemStatus(boolean systemStatus){
            this.systemStatus = systemStatus;
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
        public Builder useP300(Boolean useP300){
            this.useP300 = useP300;
            return this;
        }

        @NonNull
        public Builder enableDcOffset(Boolean enableDcOffset){
            this.enableDcOffset = enableDcOffset;
            return this;
        }

        @NonNull
        public Builder serialNumber(String serialNumber){
            this.serialNumber = serialNumber;
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
            return new DeviceConfig(notchFilter, bandpassFilter, gainValue, mtuValue, useP300, enableDcOffset, deviceStatusListener, serialNumber, productName, a2dpName, connectA2DP, disconnectA2DP, systemStatus);
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
