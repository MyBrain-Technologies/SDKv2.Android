package core.device;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.device.model.MbtDevice;
import features.MbtDeviceType;

/**
 * This interface contains all required object classes to communicate with the DEVICE module using
 * the {@link eventbus.EventBusManager} bus.
 */
public interface DeviceEvents {


    /**
     * Creating an instance of this class on the EventBus allows to get the {@link MbtDevice} instance
     * out of the Device module. Make sure to have a method in {@link MbtDeviceManager} that {@link org.greenrobot.eventbus.Subscribe}
     * to this event.
     */
    class GetDeviceEvent {
    }

    /**
     * This object is used to encapsulate the {@link MbtDevice} instance out of the module. The encapsulation
     * is mandatory because the {@link MbtDevice} instance be null at some point (for example, before a
     * bluetooth connection).
     */
    class PostDeviceEvent {
        @Nullable
        private final MbtDevice device;

        PostDeviceEvent(@Nullable MbtDevice device){
            this.device = device;
        }

        @Nullable
        public MbtDevice getDevice() {
            return device;
        }
    }


    class NewBluetoothDeviceEvent {
        @Nullable
        private final BluetoothDevice device;

        private final MbtDeviceType deviceType;

        public NewBluetoothDeviceEvent(@Nullable BluetoothDevice device, MbtDeviceType deviceType){
            this.device = device;
            this.deviceType = deviceType;
        }

        @Nullable
        public BluetoothDevice getDevice() {
            return device;
        }

        MbtDeviceType getDeviceType() {
            return deviceType;
        }
    }

    class AudioBluetoothDeviceEvent {
        @Nullable
        private final BluetoothDevice device;

        public AudioBluetoothDeviceEvent(@Nullable BluetoothDevice device){
            this.device = device;
        }

        @Nullable
        public BluetoothDevice getDevice() {
            return device;
        }

    }


    /**
     * Event that handle DC offset and Saturation raw measures
     */
    class RawDeviceMeasure {
        private byte[] rawMeasure;

        public RawDeviceMeasure(@NonNull byte[] rawMeasure){
            this.rawMeasure = rawMeasure;
        }

        byte[] getRawMeasure() {
            return rawMeasure;
        }
    }

    /**
     * Event that returns the headset's response after the SDK has sent a command to it using Mailbox or other characteristic writing methods
     */
    class RawDeviceResponseEvent {
        private byte[] rawResponse;

        public RawDeviceResponseEvent(@NonNull byte[] rawResponse){
            this.rawResponse = rawResponse;
        }

        public byte[] getRawResponse() {
            return rawResponse;
        }
    }

}
