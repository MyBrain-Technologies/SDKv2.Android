package command;

import android.support.annotation.IntRange;
import android.support.annotation.Keep;

import engine.clientevents.BaseError;

/**
 * Device commands sent from the SDK to the headset
 * and related to EEG streaming
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 */
@Keep
public interface BluetoothCommands {

    /**
     * Command sent from the SDK to the connected headset
     * in order to change its Maximum Transmission Unit
     * (maximum size of the data sent by the headset to the SDK).
     */
    @Keep
    class Mtu extends BluetoothCommand<Integer, BaseError>{

        /**
         * The new Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK)
         * to set
         */
        private int mtu;

        private final int MINIMUM = 23;
        private final int MAXIMUM = 121;

        /**
         * Command sent from the SDK to the connected headset
         * in order to change its Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK).
         * The new serial number is stored and returned by the headset if the command succeeds.
         * @param mtu is the new Maximum Transmission Unit
         */
        public Mtu(@IntRange(from = MINIMUM, to = MAXIMUM) int mtu) {
            this.mtu = mtu;
            this.init();
        }

        /**
         * Command sent from the SDK to the connected headset
         * in order to change its Maximum Transmission Unit
         * (maximum size of the data sent by the headset to the SDK).
         * The new serial number is stored and returned by the headset if the command succeeds.
         * @param mtu is the new Maximum Transmission Unit
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the configuration command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link Mtu}(int mtu) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public Mtu(@IntRange(from = MINIMUM, to = MAXIMUM) int mtu, CommandInterface.CommandCallback<Integer> commandCallback) {
            this.mtu = mtu;
            this.commandCallback = commandCallback;
            this.init();
        }

        @Override
        public Integer serialize() {
            return getData();
        }

        @Override
        public boolean isValid() {
            return mtu >= MINIMUM && mtu <= MAXIMUM ;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a MTU lower than "+ MINIMUM + " and higher than "+ MAXIMUM +" in the "+this.getClass().getSimpleName()+ " contructor.";
        }

        @Override
        public Integer getData() {
            return mtu;
        }

    }
}
