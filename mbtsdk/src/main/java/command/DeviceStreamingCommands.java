package command;

import android.support.annotation.Keep;

import config.AmpGainConfig;
import config.FilterConfig;
import engine.SimpleRequestCallback;

/**
 * Device commands sent from the SDK to the headset
 * and related to EEG streaming
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 */
@Keep
public interface DeviceStreamingCommands {

    /**
     * Command sent from the SDK to the connected headset
     * in order to change its Maximum Transmission Unit
     * (maximum size of the data sent by the headset to the SDK).
     * The new serial number is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class Mtu extends DeviceCommand implements DeviceStreamingCommands{

        /**
         * The new Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK)
         * to set
         */
        private int mtu;

        /**
         * Command sent from the SDK to the connected headset
         * in order to change its Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK).
         * The new serial number is stored and returned by the headset if the command succeeds.
         * @param mtu is the new Maximum Transmission Unit
         */
        public Mtu(int mtu) {
            this.mtu = mtu;
        }

        /**
         * Command sent from the SDK to the connected headset
         * in order to change its Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK).
         * The new serial number is stored and returned by the headset if the command succeeds.
         * @param mtu is the new Maximum Transmission Unit
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link Mtu}(int mtu) constructor
         */
        public Mtu(int mtu, SimpleRequestCallback<byte[]> responseCallback) {
            this.mtu = mtu;
            this.responseCallback = responseCallback;
        }

        public int getMtu() {
            return mtu;
        }

    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change the applied Notch filter.
     * The new notch filter is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class NotchFilter extends DeviceCommand implements DeviceStreamingCommands {
        /**
         * The new notch filter to apply
         */
        private FilterConfig notchFilter;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied Notch filter.
         * The new notch filter is stored and returned by the headset if the command succeeds.
         * @param notchFilter is the new Notch filter to apply
         */
        public NotchFilter(FilterConfig notchFilter) {
            this.notchFilter = notchFilter;
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied Notch filter.
         * The new notch filter is stored and returned by the headset if the command succeeds.
         * @param notchFilter is the new Notch filter to apply
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link NotchFilter}(FilterConfig notchFilter) constructor.
         */
        public NotchFilter(FilterConfig notchFilter, SimpleRequestCallback<byte[]> responseCallback) {
            this.notchFilter = notchFilter;
            this.responseCallback = responseCallback;
        }

        public FilterConfig getNotchFilter() {
            return notchFilter;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change the applied Bandpass filter.
     * The new bandpass filter is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class BandpassFilter extends DeviceCommand implements DeviceStreamingCommands{
        /**
         * The new bandpass filter to apply
         */
        private FilterConfig bandpassFilter;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied Bandpass filter.
         * The new bandpass filter is stored and returned by the headset if the command succeeds.
         * @param bandpassFilter is the new Bandpass filter to apply
         */
        public BandpassFilter(FilterConfig bandpassFilter) {
            this.bandpassFilter = bandpassFilter;
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied Bandpass filter.
         * The new bandpass filter is stored and returned by the headset if the command succeeds
         * @param bandpassFilter is the new Bandpass filter to apply
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link BandpassFilter}(FilterConfig bandpassFilter) constructor
         */
        public BandpassFilter(FilterConfig bandpassFilter, SimpleRequestCallback<byte[]> responseCallback) {
            this.bandpassFilter = bandpassFilter;
            this.responseCallback = responseCallback;
        }

        public FilterConfig getBandpassFilter() {
            return bandpassFilter;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change the applied amplifier gain.
     * The new bandpass filter is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class AmplifierGain extends DeviceCommand implements DeviceStreamingCommands{
        /**
         * The new amplifier gain to apply
         */
        private AmpGainConfig ampGainConfig;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied amplifier gain.
         * The new amplifier gain is stored and returned by the headset if the command succeeds
         * @param ampGainConfig is the new Amplifier gain to apply
         */
        public AmplifierGain(AmpGainConfig ampGainConfig) {
            this.ampGainConfig = ampGainConfig;
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied amplifier gain.
         * The new amplifier gain is stored and returned by the headset if the command succeeds
         * @param ampGainConfig is the new Amplifier gain to apply
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link AmplifierGain}(AmpGainConfig ampGainConfig) constructor.
         */
        public AmplifierGain(AmpGainConfig ampGainConfig, SimpleRequestCallback<byte[]> responseCallback) {
            this.ampGainConfig = ampGainConfig;
            this.responseCallback = responseCallback;
        }

        public AmpGainConfig getAmpGainConfig() {
            return ampGainConfig;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to enable or disable triggers receiving and synchronize external acquisitions.
     * The triggers receiving status is returned by the headset if the command succeeds.
     */
    @Keep
    class Triggers extends DeviceCommand implements DeviceStreamingCommands{
        /**
         * The new boolean status of triggers receiving
         * Triggers are sent to the SDK if enableTriggers is set to true.
         * Triggers are not sent to the SDK if enableTriggers is set to false.
         */
        private boolean enableTriggers;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to enable or disable triggers receiving and synchronize external acquisitions.
         * The triggers receiving status is returned by the headset if the command succeeds.
         * @param enableTriggers is the new boolean status of triggers receiving
         * Triggers are sent to the SDK if enableTriggers is set to true.
         * Triggers are not sent to the SDK if enableTriggers is set to false.
         */
        public Triggers(boolean enableTriggers) {
            this.enableTriggers = enableTriggers;
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to enable or disable triggers receiving and synchronize external acquisitions.
         * The triggers receiving status is returned by the headset if the command succeeds.
         * @param enableTriggers is the new boolean status of triggers receiving
         * Triggers are sent to the SDK if enableTriggers is set to true.
         * Triggers are not sent to the SDK if enableTriggers is set to false.
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link Triggers}(boolean enableTriggers) constructor.
         */
        public Triggers(boolean enableTriggers, SimpleRequestCallback<byte[]> responseCallback) {
            this.enableTriggers = enableTriggers;
            this.responseCallback = responseCallback;
        }

        public boolean areTriggersEnabled() {
            return enableTriggers;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to enable or disable EEG signal DC offset receiving.
     * The DC offset receiving status is returned by the headset if the command succeeds.
     */
    @Keep
    class DcOffset extends DeviceCommand implements DeviceStreamingCommands {
        /**
         * The new boolean status of EEG signal DC offset receiving
         * DC offsets are sent to the SDK if enableDcOffset is set to true.
         * DC offsets are not sent to the SDK if enableDcOffset is set to false.
         */
        private boolean enableDcOffset;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to enable or disable EEG signal DC offset receiving.
         * The DC offset receiving status is returned by the headset if the command succeeds.
         * @param enableDcOffset is the new boolean status of EEG signal DC offset receiving
         * DC offsets are sent to the SDK if enableDcOffset is set to true.
         * DC offsets are not sent to the SDK if enableDcOffset is set to false.
         */
        public DcOffset(boolean enableDcOffset) {
            this.enableDcOffset = enableDcOffset;
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to enable or disable EEG signal DC offset receiving.
         * The DC offset receiving status is returned by the headset if the command succeeds.
         * @param enableDcOffset is the new boolean status of EEG signal DC offset receiving
         * DC offsets are sent to the SDK if enableDcOffset is set to true.
         * DC offsets are not sent to the SDK if enableDcOffset is set to false.
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link DcOffset}(boolean enableDcOffset) constructor.
         */
        public DcOffset(boolean enableDcOffset, SimpleRequestCallback<byte[]> responseCallback) {
            this.enableDcOffset = enableDcOffset;
            this.responseCallback = responseCallback;
        }

        public boolean isEnableDcOffset() {
            return enableDcOffset;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to get the device streaming configuration such as :
     * the notch filter,
     * the bandpass filter,
     * the triggers receiving status,
     * the signal processing buffer size (number of EEG sample times),
     * and real frequency sample measured by the firmware.
     * The DC offset receiving status is returned by the headset if the command succeeds.
     */
    @Keep
    class EegConfig extends DeviceCommand implements DeviceStreamingCommands{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to get the device streaming configuration such as :
         * the notch filter,
         * the bandpass filter,
         * the triggers receiving status,
         * the signal processing buffer size (number of EEG sample times),
         * and real frequency sample measured by the firmware.
         * The DC offset receiving status is returned by the headset if the command succeeds.
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the get command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * Each status is returned in one byte of the raw response array.
         */
        public EegConfig(SimpleRequestCallback<byte[]> responseCallback) {
            this.responseCallback = responseCallback;
        }

    }

}
