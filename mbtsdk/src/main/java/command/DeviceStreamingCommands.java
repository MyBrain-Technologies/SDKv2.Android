package command;

import android.support.annotation.IntRange;
import android.support.annotation.Keep;

import config.AmpGainConfig;
import config.FilterConfig;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;

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
        private final int UNDEFINED = 0;
        private int mtu = UNDEFINED;

        private final int MINIMUM = 23;
        private final int MAXIMUM = 121;

        /**
         * Command sent from the SDK to the connected headset
         * in order to change its Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK).
         * The new serial number is stored and returned by the headset if the command succeeds.
         * @param mtu is the new Maximum Transmission Unit
         */
        public Mtu(@IntRange(from = 23, to = 121) int mtu) {
            super(DeviceCommandEvents.MTU);
            this.mtu = mtu;
            init();
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
        public Mtu(@IntRange(from = MINIMUM, to = MAXIMUM) int mtu, SimpleRequestCallback<byte[]> responseCallback) {
             super(DeviceCommandEvents.MTU);
             new Mtu(mtu);
             this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return mtu >= MINIMUM && mtu <= MAXIMUM ;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) {
            mtu = UNDEFINED;
        }

        @Override
        public byte[] getData() {
            if(mtu == UNDEFINED)
                return null;

            return new byte[]{(byte)mtu};
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
            super(DeviceCommandEvents.MBX_SET_NOTCH_FILT);
            this.notchFilter = notchFilter;
            init();
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
            super(DeviceCommandEvents.MBX_SET_NOTCH_FILT);
            new NotchFilter(notchFilter);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return notchFilter != null;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) {
            notchFilter = null;
        }

        @Override
        public byte[] getData() {
            if(notchFilter == null)
                return null;

            return new byte[]{(byte)notchFilter.getNumVal()};
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
            super(DeviceCommandEvents.MBX_SET_BANDPASS_FILT);
            this.bandpassFilter = bandpassFilter;
            init();
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
            super(DeviceCommandEvents.MBX_SET_BANDPASS_FILT);
            new BandpassFilter(bandpassFilter);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return bandpassFilter != null;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) {
            bandpassFilter = null;
        }

        @Override
        public byte[] getData() {
            if(bandpassFilter == null)
                return null;

            return new byte[]{(byte)bandpassFilter.getNumVal()};
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change the applied amplifier gain.
     * The new bandpass filter is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class AmplifierGain extends DeviceCommand implements DeviceStreamingCommands {
        /**
         * The new amplifier gain to apply
         */
        private AmpGainConfig ampGainConfig;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied amplifier gain.
         * The new amplifier gain is stored and returned by the headset if the command succeeds
         *
         * @param ampGainConfig is the new Amplifier gain to apply
         */
        public AmplifierGain(AmpGainConfig ampGainConfig) {
            super(DeviceCommandEvents.MBX_SET_AMP_GAIN);
            this.ampGainConfig = ampGainConfig;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change the applied amplifier gain.
         * The new amplifier gain is stored and returned by the headset if the command succeeds
         *
         * @param ampGainConfig    is the new Amplifier gain to apply
         * @param responseCallback is a {@link SimpleRequestCallback} object
         *                         that provides a callback for the returned raw response
         *                         sent by the headset to the SDK once the configuration command is received.
         *                         This raw response is a byte array that has be to converted to be readable.
         *                         If you're not interested in getting the returned response,
         *                         call the {@link AmplifierGain}(AmpGainConfig ampGainConfig) constructor.
         */
        public AmplifierGain(AmpGainConfig ampGainConfig, SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_SET_AMP_GAIN);
            new AmplifierGain(ampGainConfig);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return ampGainConfig != null;
        }


        @Override
        public void onError(BaseError error, String additionnalInfo) {
            ampGainConfig = null;
        }

        @Override
        public byte[] getData() {
            if (ampGainConfig == null)
                return null;

            return new byte[]{(byte) ampGainConfig.getNumVal()};
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
            super(DeviceCommandEvents.MBX_P300_ENABLE);
            this.enableTriggers = enableTriggers;
            init();
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
            super(DeviceCommandEvents.MBX_P300_ENABLE);
            new Triggers(enableTriggers);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) { }

        @Override
        public byte[] getData() {

            return new byte[]{enableTriggers ? ENABLE : DISABLE};
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
            super(DeviceCommandEvents.MBX_DC_OFFSET_ENABLE);
            this.enableDcOffset = enableDcOffset;
            init();
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
            super(DeviceCommandEvents.MBX_DC_OFFSET_ENABLE);
            new DcOffset(enableDcOffset);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) { }

        @Override
        public byte[] getData() {
            return new byte[]{enableDcOffset ? ENABLE : DISABLE};
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
            super(DeviceCommandEvents.MBX_GET_EEG_CONFIG);
            this.responseCallback = responseCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return responseCallback != null;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) { }

        @Override
        public byte[] getData() {
            return null;
        }
    }

}
