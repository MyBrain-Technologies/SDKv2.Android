package command;

import android.support.annotation.Keep;

import java.nio.ByteBuffer;

import core.device.model.FirmwareVersion;
import core.device.oad.OADManager;
import engine.clientevents.BaseError;

/**
 * OAD Mailbox commands sent from the SDK to the headset
 * in order to perform the firmware update
 */
@Keep
public interface OADCommands {

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to request a validation of the current firmware before starting the transfer.
     * The current firmware needs to know the firmware version and the binary file length,
     * and can accept or reject the OAD update.
     */
    @Keep
    class RequestFirmwareValidation extends DeviceCommand<byte[], BaseError> {

        private final int FIRMWARE_VERSION_NB_BYTES = 2;
        private final int BINARY_FILE_LENGTH_NB_BYTES = 2;

        /**
         * The firmware version that will replace the current version installed on the headset device
         */
        private FirmwareVersion firmwareVersion;

        /**
         * The number of packets of the binary file that holds the firmware to upload & install on the headset device
         */
        private short binaryFileNbPackets;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to request a validation of the current firmware before starting the transfer.
         * The current firmware needs to know the firmware version and the binary file length,
         * and can accept or reject the OAD update.
         * @param firmwareVersion is the firmware version that will replace the current version installed on the headset device
         * @param binaryFileNbPackets is the number of packets of the binary file that holds the firmware to upload & install on the headset device
         * If you're interested in getting the returned response,
         * sent by the headset to the SDK once the command is received,
         * call the {@link RequestFirmwareValidation}(FirmwareVersion firmwareVersion, short binaryFileNbPackets, {@linkCommandCallback<DeviceCommand, byte[]>)} constructor.
         */
        public RequestFirmwareValidation(FirmwareVersion firmwareVersion, short binaryFileNbPackets) {
            super(DeviceCommandEvent.MBX_START_OTA_TXF);
                this.firmwareVersion = firmwareVersion;
                this.binaryFileNbPackets = binaryFileNbPackets;
                init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to request a validation of the current firmware before starting the transfer.
         * The current firmware needs to know the firmware version and the binary file length,
         * and can accept or reject the OAD update.
         * @param firmwareVersion is the firmware version that will replace the current version installed on the headset device
         * @param binaryFileNbPackets is the number of packets of the binary file that holds the firmware to upload & install on the headset device
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link RequestFirmwareValidation}(String firmwareVersion, int binaryFileNbPackets) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public RequestFirmwareValidation(FirmwareVersion firmwareVersion, short binaryFileNbPackets, CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_SET_SERIAL_NUMBER);
            this.firmwareVersion = firmwareVersion;
            this.binaryFileNbPackets = binaryFileNbPackets;
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return firmwareVersion != null
                    && !firmwareVersion.getFirmwareVersionAsString().isEmpty()
                    && binaryFileNbPackets == OADManager.EXPECTED_NB_PACKETS_BINARY_FILE;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null, or empty, or invalid formatted, firmware version in the "+this.getClass().getSimpleName()+ " contructor.";
        }

        @Override
        public byte[] getData() {
            if(firmwareVersion == null)
                return null;

            ByteBuffer buffer = ByteBuffer.allocate(BINARY_FILE_LENGTH_NB_BYTES + FIRMWARE_VERSION_NB_BYTES);
            buffer.put(firmwareVersion.getFirmwareVersionAsString().getBytes());
            buffer.putShort(binaryFileNbPackets);
            return buffer.array();
        }
    }

    /**
     * Command sent from the SDK to the peripheral connected headset device
     * in order to send in Bluetooth an OAD packet to the current firmware.
     */
    @Keep
    class SendPacket extends DeviceCommand<byte[], BaseError> {

        /**
         * The packet to send to the connected peripheral headset device in Bluetooth
         */
        private byte[] packetToSend;

        /**
         * Command sent from the SDK to the peripheral connected headset device
         * in order to send in Bluetooth an OAD packet to the current firmware.
         * @param packetToSend The packet to send to the connected peripheral headset device in Bluetooth
         * If you're interested in getting the returned response,
         * sent by the headset to the SDK once the command is received,
         * call the {@link SendPacket}(byte[] packetToSend, {@linkCommandCallback<DeviceCommand, byte[]>)} constructor.
         */
        public SendPacket(byte[] packetToSend) {
            super(DeviceCommandEvent.GATT_OTA_STATUS_TRANSFER);
                this.packetToSend = packetToSend;
                init();
        }

        /**
         * Command sent from the SDK to the peripheral connected headset device
         * in order to send in Bluetooth an OAD packet to the current firmware.
         * @param packetToSend The packet to send to the connected peripheral headset device in Bluetooth
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link RequestFirmwareValidation}(String firmwareVersion, int binaryFileNbPackets) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public SendPacket(byte[] packetToSend, CommandInterface.SimpleCommandCallback commandCallback) {
            super(null);
            this.packetToSend = packetToSend;
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return packetToSend != null
                    && packetToSend.length != 0;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null, or empty packet "+this.getClass().getSimpleName()+ " contructor.";
        }

        @Override
        public byte[] getData() {
            return packetToSend;
        }

        @Override
        public byte[] serialize() {
            return getData();
        }
    }

}
