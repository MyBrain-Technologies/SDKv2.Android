package command;

import android.support.annotation.Keep;

import config.AmpGainConfig;
import config.FilterConfig;
import engine.SimpleRequestCallback;

/**
 * Mailbox commands and other headset commands related to EEG streaming are hold in this object
 */
@Keep
public interface DeviceStreamingCommands {


    class Mtu extends DeviceCommand implements DeviceStreamingCommands{
        private int mtu;

        public Mtu(int mtu) {
            this.mtu = mtu;
        }

        public Mtu(int mtu, SimpleRequestCallback<byte[]> responseCallback) {
            this.mtu = mtu;
            this.responseCallback = responseCallback;
        }

        public int getMtu() {
            return mtu;
        }

    }

    class NotchFilter extends DeviceCommand implements DeviceStreamingCommands {
        private FilterConfig notchFilter;

        public NotchFilter(FilterConfig notchFilter) {
            this.notchFilter = notchFilter;
        }

        public NotchFilter(FilterConfig notchFilter, SimpleRequestCallback<byte[]> responseCallback) {
            this.notchFilter = notchFilter;
            this.responseCallback = responseCallback;
        }

        public FilterConfig getNotchFilter() {
            return notchFilter;
        }
    }

    class BandpassFilter extends DeviceCommand implements DeviceStreamingCommands{
        private FilterConfig bandpassFilter;

        public BandpassFilter(FilterConfig bandpassFilter) {
            this.bandpassFilter = bandpassFilter;
        }

        public BandpassFilter(FilterConfig bandpassFilter, SimpleRequestCallback<byte[]> responseCallback) {
            this.bandpassFilter = bandpassFilter;
            this.responseCallback = responseCallback;
        }

        public FilterConfig getBandpassFilter() {
            return bandpassFilter;
        }
    }

    class AmplifierGain extends DeviceCommand implements DeviceStreamingCommands{
        private AmpGainConfig ampGainConfig;

        public AmplifierGain(AmpGainConfig ampGainConfig) {
            this.ampGainConfig = ampGainConfig;
        }

        public AmplifierGain(AmpGainConfig ampGainConfig, SimpleRequestCallback<byte[]> responseCallback) {
            this.ampGainConfig = ampGainConfig;
            this.responseCallback = responseCallback;
        }

        public AmpGainConfig getAmpGainConfig() {
            return ampGainConfig;
        }
    }

    class Triggers extends DeviceCommand implements DeviceStreamingCommands{
        private boolean enableTriggers;

        public Triggers(boolean enableTriggers) {
            this.enableTriggers = enableTriggers;
        }

        public Triggers(boolean enableTriggers, SimpleRequestCallback<byte[]> responseCallback) {
            this.enableTriggers = enableTriggers;
            this.responseCallback = responseCallback;
        }

        public boolean areTriggersEnabled() {
            return enableTriggers;
        }
    }

    class DcOffset extends DeviceCommand implements DeviceStreamingCommands {
        private boolean enableDcOffset;

        public DcOffset(boolean enableDcOffset) {
            this.enableDcOffset = enableDcOffset;
        }

        public DcOffset(boolean enableDcOffset, SimpleRequestCallback<byte[]> responseCallback) {
            this.enableDcOffset = enableDcOffset;
            this.responseCallback = responseCallback;
        }

        public boolean isEnableDcOffset() {
            return enableDcOffset;
        }
    }

    class EegConfig extends DeviceCommand implements DeviceStreamingCommands{

        public EegConfig(SimpleRequestCallback<byte[]> responseCallback) {
            this.responseCallback = responseCallback;
        }

    }

}
