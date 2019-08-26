package core.device;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.device.model.FirmwareVersion;
import core.device.model.MbtDevice;
import eventbus.MbtEventBus;
import features.MbtDeviceType;

/**
 * This interface contains all required object classes to communicate with the DEVICE module using
 * the {@link MbtEventBus} bus.
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

        public PostDeviceEvent(@Nullable MbtDevice device){
            this.device = device;
        }

        @Nullable
        public MbtDevice getDevice() {
            return device;
        }
    }

    class AudioDisconnectedDeviceEvent {    }

    class AudioConnectedDeviceEvent {
        @Nullable
        private final BluetoothDevice device;

        public AudioConnectedDeviceEvent(@Nullable BluetoothDevice device){
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

    class StartOADUpdate {
        private FirmwareVersion firmwareVersion;

        public StartOADUpdate(FirmwareVersion firmwareVersion) {
            this.firmwareVersion = firmwareVersion;
        }

        public FirmwareVersion getFirmwareVersion() {
            return firmwareVersion;
        }
    }
}
