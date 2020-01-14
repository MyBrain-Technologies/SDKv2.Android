package command;

import androidx.annotation.Keep;

import java.nio.ByteBuffer;

import engine.clientevents.BaseError;
import utils.OADExtractionUtils;

import static utils.OADExtractionUtils.FILE_LENGTH_NB_BYTES;
import static utils.OADExtractionUtils.FIRMWARE_VERSION_NB_BYTES;

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
    class RequestFirmwareValidation extends DeviceCommand<byte[], BaseError> implements OADCommands {

        private static final int MSB_INDEX = 2;
        private static final int LSB_INDEX = 3;
        /**
         * The firmware version that will replace the current version installed on the headset device
         */
        private byte[] firmwareVersion;

        /**
         * The number of packets of the binary file that holds the firmware to upload & install on the headset device
         */
        private short nbPackets;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to request a validation of the current firmware before starting the transfer.
         * The current firmware needs to know the firmware version and the binary file length,
         * and can accept or reject the OAD update.
         * @param firmwareVersion is the firmware version that will replace the current version installed on the headset device
         * @param nbPackets is the number of packets of the binary file that holds the firmware to upload & install on the headset device
         * If you're interested in getting the returned response,
         * sent by the headset to the SDK once the command is received,
         * call the {@link RequestFirmwareValidation}(MbtVersion firmwareVersion, short nbPackets, {@linkCommandCallback<DeviceCommand, byte[]>)} constructor.
         */
        public RequestFirmwareValidation(byte[] firmwareVersion, short nbPackets) {
            super(DeviceCommandEvent.MBX_START_OTA_TXF);
                this.firmwareVersion = firmwareVersion;
                this.nbPackets = nbPackets;
                init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to request a validation of the current firmware before starting the transfer.
         * The current firmware needs to know the firmware version and the binary file length,
         * and can accept or reject the OAD update.
         * @param firmwareVersion is the firmware version that will replace the current version installed on the headset device
         * @param nbPackets is the number of packets of the binary file that holds the firmware to upload & install on the headset device
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link RequestFirmwareValidation}(String firmwareVersion, int nbPackets) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public RequestFirmwareValidation(byte[] firmwareVersion, short nbPackets, CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_SET_SERIAL_NUMBER);
            this.firmwareVersion = firmwareVersion;
            this.nbPackets = nbPackets;
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return firmwareVersion != null
                    && firmwareVersion.length == FIRMWARE_VERSION_NB_BYTES
                    && nbPackets == OADExtractionUtils.EXPECTED_NB_PACKETS;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null, or empty, or invalid formatted, firmware version in the "+this.getClass().getSimpleName()+ " contructor.";
        }

        @Override
        public void onResponseReceived(Object response) {
            this.setCommandEvent(DeviceCommandEvent.MBX_OTA_MODE_EVT);
            super.onResponseReceived(response);
        }

        @Override
        public byte[] getData() {
            if(!isValid())
                return null;

            ByteBuffer buffer = ByteBuffer.allocate(FIRMWARE_VERSION_NB_BYTES + FILE_LENGTH_NB_BYTES);
            buffer.put(firmwareVersion);
            buffer.putShort(nbPackets);
            byte[] data = buffer.array();
            //Reversing LSB and MSB
            byte temp = data[LSB_INDEX];
            data[LSB_INDEX] = data[MSB_INDEX];
            data[MSB_INDEX] = temp;
            return buffer.array();
        }
    }

    /**
     * Command sent from the SDK to the peripheral connected headset device
     * in order to send in Bluetooth an OAD packet to the current firmware.
     */
    @Keep
    class TransferPacket extends DeviceCommand<byte[], BaseError> implements OADCommands{

        /**
         * The packet to transfer to the connected peripheral headset device in Bluetooth
         */
        private byte[] packetToTransfer;

        /**
         * Command sent from the SDK to the peripheral connected headset device
         * in order to transfer in Bluetooth an OAD packet to the current firmware.
         * @param packetToTransfer The packet to transfer to the connected peripheral headset device in Bluetooth
         * If you're interested in getting the returned response,
         * sent by the headset to the SDK once the command is received,
         * call the {@link TransferPacket}(byte[] packetToTransfer, {@link  CommandInterface.SimpleCommandCallback<DeviceCommand, byte[]>)} constructor.
         */
        public TransferPacket(byte[] packetToTransfer) {
            super(DeviceCommandEvent.OTA_STATUS_TRANSFER);
                this.packetToTransfer = packetToTransfer;
                init(false);
        }

        /**
         * Command sent from the SDK to the peripheral connected headset device
         * in order to transfer in Bluetooth an OAD packet to the current firmware.
         * @param packetToTransfer The packet to transfer to the connected peripheral headset device in Bluetooth
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link TransferPacket}(String firmwareVersion, int nbPackets) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public TransferPacket(byte[] packetToTransfer, CommandInterface.SimpleCommandCallback commandCallback) {
            super(DeviceCommandEvent.OTA_STATUS_TRANSFER);
            this.packetToTransfer = packetToTransfer;
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return packetToTransfer != null
                    && packetToTransfer.length != 0;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null, or empty packet "+this.getClass().getSimpleName()+ " contructor.";
        }

        @Override
        public byte[] getData() {
            return packetToTransfer;
        }

        @Override
        public byte[] serialize() {
            return getData();
        }
    }

}
