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

        public PostDeviceEvent(@Nullable MbtDevice device){
            this.device = device;
        }

        @Nullable
        public MbtDevice getDevice() {
            return device;
        }
    }


    class FoundDeviceEvent {
        @Nullable
        private final BluetoothDevice device;

        private final MbtDeviceType deviceType;

        public FoundDeviceEvent(@Nullable BluetoothDevice device, MbtDeviceType deviceType){
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

    class DisconnectedDeviceEvent {    }

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

    /**
     * Event that returns the headset's response after the SDK has sent a command to it using Mailbox or other characteristic writing methods
     */
    class RawDeviceResponseEvent {
        private byte[] rawResponse;

        public RawDeviceResponseEvent(byte[] rawResponse){
            this.rawResponse = rawResponse;
        }

        public byte[] getRawResponse() {
            return rawResponse;
        }
    }

    /**
     * Event triggered to notify the SDK Bluetooth unit
     * that the SDK Device unit needs to check the OAD update request validity, in order
     * to know if the current firmware validate or reject the new firmware to upload
     */
    class OADValidationRequestEvent{

        /**
         * New firmware version
         */
        private String firmwareVersion;

        /**
         * Number of OAD packets to send to the firmware (chunk of the binary file that holds the new firmware)
         */
        private int binaryFileLength;

        public OADValidationRequestEvent(String firmwareVersion, int binaryFileLength) {
            this.firmwareVersion = firmwareVersion;
            this.binaryFileLength = binaryFileLength;
        }

        /**
         * Returns the new firmware version
         * @return the new firmware version
         */
        public String getFirmwareVersion() {
            return firmwareVersion;
        }

        /**
         * Number of OAD packets to send to the firmware (chunk of the binary file that holds the new firmware)
         */
        public int getBinaryFileLength() {
            return binaryFileLength;
        }
    }

    /**
     * Event triggered to notify the SDK Device unit
     * when a response is received by the SDK Bluetooth unit
     * to know if the current firmware validate or reject the new firmware to upload
     */
    class OADValidationResponseEvent{

        /**
         * Validation flag used to transmit the firmware response related to a validity request
         * True if the headset device validated the OAD update request
         * False if the headset device rejected the OAD update request
         */
        private boolean isValidated;

        public OADValidationResponseEvent(boolean isValidated) {
            this.isValidated = isValidated;
        }

        /**
         * Validation flag used to transmit the firmware response related to a validity request
         * True if the headset device validated the OAD update request
         * False if the headset device rejected the OAD update request
         */
        public boolean isValidated() {
            return isValidated;
        }
    }

    /**
     * Event triggered when a packet has to be sent to the Bluetooth manager
     * so that it can send it to the headset
     */
    class OADTransferEvent{

        /**
         * OAD packet is a chunk of the binary file that holds the new firmware
         */
        private byte[] OADPacket;

        public OADTransferEvent(byte[] OADPacket) {
            this.OADPacket = OADPacket;
        }

        /**
         * OAD packet is a chunk of the binary file that holds the new firmware
         */
        public byte[] getOADPacket() {
            return OADPacket;
        }
    }

    /**
     * Event triggered to notify the SDK Device unit
     * when a response is caught by the SDK Bluetooth unit
     * to know if the new firmware has been fully received by the headset device to update
     */
    class OADReadbackEvent {

        /**
         * Readback flag used to transmit the firmware response related to a transfer request
         * True if the headset device received all the OAD packets sent by the SDK
         * False if the headset device encountered a problem during the transfer
         */
        private boolean CRCreadback;

        public OADReadbackEvent(boolean readback) {
            this.CRCreadback = readback;
        }

        /**
         * Readback flag used to transmit the firmware response related to a transfer request
         * True if the headset device received all the OAD packets sent by the SDK
         * False if the headset device encountered a problem during the transfer
         */
        public boolean isCRCreadback() {
            return CRCreadback;
        }
    }
}
