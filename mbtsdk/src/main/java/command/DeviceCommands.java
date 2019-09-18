package command;

import android.support.annotation.Keep;

import core.bluetooth.spp.MbtBluetoothSPP;
import engine.clientevents.BaseError;

/**
 * Mailbox commands sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 */
@Keep
public interface DeviceCommands {

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change its serial number.
     * The new serial number is stored and returned by the headset if the command succeeds
     */
    @Keep
    class UpdateSerialNumber extends DeviceCommand<byte[], BaseError> {

        /**
         * The new serial number value to set
         */
        private String serialNumber;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its serial number.
         * The new serial number is stored and returned by the headset if the command succeeds
         * @param serialNumber is the new serial number value to set
         * If you're interested in getting the returned response,
         * sent by the headset to the SDK once the update command is received,
         * call the {@link UpdateSerialNumber}(String serialNumber, {@linkCommandCallback<DeviceCommand, byte[]>)} constructor.
         */
        public UpdateSerialNumber(String serialNumber) {
            super(DeviceCommandEvent.MBX_SET_SERIAL_NUMBER);
            this.serialNumber = serialNumber;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its serial number.
         * The new serial number is stored by the headset if the command succeeds
         * @param serialNumber is the new serial number value to set
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the update command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link UpdateSerialNumber}(String serialNumber) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public UpdateSerialNumber(String serialNumber, CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_SET_SERIAL_NUMBER);
            this.serialNumber = serialNumber;
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return serialNumber != null && !serialNumber.isEmpty();
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null or empty serial number in the "+this.getClass().getSimpleName()+ " constructor.";
        }

        @Override
        public byte[] getData() {
            if(serialNumber == null)
                return null;

            return serialNumber.getBytes();
        }


    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change its external name (QR code number).
     * The new external name is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class UpdateExternalName extends DeviceCommand<byte[], BaseError> {

        /**
         * The new external name to set
         */
        private String externalName;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its external name.
         * The new external name is stored by the headset if the command succeeds.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the update command is received,
         * call the {@link UpdateExternalName}({@link CommandInterface.CommandCallback}<DeviceCommand, byte[]>) constructor.
         * @param externalName the new external name value to set
         */
        public UpdateExternalName(String externalName) {
            super(DeviceCommandEvent.MBX_SET_EXTERNAL_NAME);
            this.externalName = externalName;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its external name.
         * The new external name is stored by the headset if the command succeeds.
         * @param externalName the new external name value to set
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the update command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link UpdateExternalName}(String externalName) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public UpdateExternalName(String externalName, CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_SET_EXTERNAL_NAME);
            this.commandCallback = commandCallback;
            this.externalName = externalName;
            init();
        }

        @Override
        public boolean isValid() {
            return externalName != null && !externalName.isEmpty();
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null or empty external name in the "+this.getClass().getSimpleName()+ " constructor.";
        }

        @Override
        public byte[] getData() {
            if(externalName == null)
                return null;

            return externalName.getBytes();
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change its product name.
     * The new product name is stored and returned by the headset if the command succeeds.
     */
    @Keep
    class UpdateProductName extends DeviceCommand<byte[], BaseError>{

        /**
         * The new product name to set
         */
        private String productName;

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its product name.
         * The new product name is stored by the headset if the command succeeds.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the update command is received,
         * call the {@link UpdateProductName}({@link CommandInterface.CommandCallback}<DeviceCommand, byte[]>) constructor.
         * @param productName is the new product name value to set
         */
        public UpdateProductName(String productName) {
            super(DeviceCommandEvent.MBX_SET_PRODUCT_NAME);
            this.productName = productName;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its product name.
         * The new product name is stored by the headset if the command succeeds.
         * @param productName the new product name value to set
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the update command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * call the {@link UpdateProductName}(String productName) constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public UpdateProductName(String productName, CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_SET_PRODUCT_NAME);
            this.commandCallback = commandCallback;
            this.productName = productName;
            init();
        }

        @Override
        public boolean isValid() {
            return productName != null && !productName.isEmpty();
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null or empty product name in the "+this.getClass().getSimpleName()+ " constructor.";
        }

        @Override
        public byte[] getData() {
            if(productName == null)
                return null;

            return productName.getBytes();
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to get the device system status such as :
     * the processor status,
     * the external memory status,
     * the audio status,
     * and the ADS status.
     * The device system status is returned by the headset if the command succeeds.
     */
    @Keep
    class GetSystemStatus extends DeviceCommand<byte[], BaseError>{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to get the device system status such as :
         * the processor status,
         * the external memory status,
         * the audio status,
         * and the ADS status.
         * The device system status is returned by the headset if the command succeeds.
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the get command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * Each status is returned in one byte of the raw response array.
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public GetSystemStatus(CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_SYS_GET_STATUS);
            this.commandCallback = commandCallback;
            init(); //must be called after the commandCallback initialisation : isValid will return false otherwise
        }

        @Override
        public boolean isValid() {
            return commandCallback != null;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to provide a null CommandCallback instance in the "+this.getClass().getSimpleName()+ " constructor.";
        }

        @Override
        public byte[] getData() {
            return null;
        }


    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to reboot the headset after the next disconnection.
     * No response is returned by the headset if the command succeeds.
     */
    @Keep
    class Reboot extends DeviceCommand<byte[], BaseError>{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to reboot the headset after the next disconnection.
         * No response is returned by the headset if the command succeeds.
         */
        public Reboot() {
            super(DeviceCommandEvent.MBX_SYS_REBOOT_EVT);

            init(false);
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to reboot the headset after the next disconnection.
         * No response is returned by the headset if the command succeeds.
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public Reboot(CommandInterface.ICommandCallback commandCallback) {
            super(DeviceCommandEvent.MBX_SYS_REBOOT_EVT);
            this.commandCallback = commandCallback;
            init(false);
        }

        @Override
        public boolean isValid() {
            return !(commandCallback instanceof CommandInterface.MbtResponse);
        }

        @Override
        public byte[] getData() {
            return null;
        }

        @Override
        public String getInvalidityError() {
            return "You are not allowed to use a CommandCallback or other Object that extends the MbtResponse interface for the commandCallback input.\n"
            +"As no response is expected, you have to call the ICommandCallback constructor to createForDevice a instance of ICommandCallback object.\n"
            +"If you still use a CommandCallback or other Object that extends the MbtResponse interface, the command won't be sent and the onError callback will be triggered.";
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to establish a Bluetooth connection for audio streaming.
     * The connection status is returned by the headset if the command succeeds.
     */
    @Keep
    class ConnectAudio extends DeviceCommand<byte[], BaseError>{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth connection for audio streaming.
         * The connection status is returned by the headset if the command succeeds.
         * sent by the headset to the SDK once the connect command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link ConnectAudio}({@link CommandInterface.CommandCallback}<DeviceCommand, byte[]> commandCallback) constructor
         */
        public ConnectAudio() {
            super(DeviceCommandEvent.MBX_CONNECT_IN_A2DP);
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth connection for audio streaming.
         * The connection status is returned by the headset if the command succeeds.
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the connect command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link ConnectAudio}() constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public ConnectAudio(CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_CONNECT_IN_A2DP);
            this.commandCallback = commandCallback;
            init();
        }



        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public byte[] getData() {
            return null;
        }

        @Override
        public String getInvalidityError() {
            return null;
        }

    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to establish a Bluetooth disconnection for audio streaming.
     * The disconnection status is returned by the headset if the command succeeds.
     */
    @Keep
    class DisconnectAudio extends DeviceCommand<byte[], BaseError>{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth disconnection for audio streaming.
         * The disconnection status is returned by the headset if the command succeeds.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link DisconnectAudio}({@link CommandInterface.CommandCallback}<DeviceCommand, byte[]> commandCallback) constructor
         */
        public DisconnectAudio() {
            super(DeviceCommandEvent.MBX_DISCONNECT_IN_A2DP);
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth disconnection for audio streaming.
         * The disconnection status is returned by the headset if the command succeeds.
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the disconnect command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link DisconnectAudio}() constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public DisconnectAudio(CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.MBX_DISCONNECT_IN_A2DP);
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public byte[] getData() {
            return null;
        }

        @Override
        public String getInvalidityError() {
            return null;
        }
    }


    /**
     * SPP command sent from the SDK to the connected headset
     * in order to read the battery charge level.
     * The battery level is returned by the headset if the command succeeds.
     */
    @Keep
    class GetBattery extends DeviceCommand<byte[], BaseError>{

        /**
         * SPP command sent from the SDK to the connected headset
         * in order to read the battery charge level.
         * The battery level is returned by the headset if the command succeeds.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the command is received,
         * call the {@link DeviceStreamingCommands.StopEEGAcquisition}({@link CommandInterface.CommandCallback}<DeviceCommand, byte[]> commandCallback) constructor
         */
        public GetBattery() {
            super(DeviceCommandEvent.CMD_GET_BATTERY_VALUE);
            init();
        }

        /**
         * SPP command sent from the SDK to the connected headset
         * in order to read the battery charge level.
         * The battery level is returned by the headset if the command succeeds.
         * @param commandCallback is a {@link CommandInterface.CommandCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link GetBattery}() constructor
         * The onRequestSent callback is triggered if the command has successfully been sent.
         */
        public GetBattery(CommandInterface.CommandCallback<byte[]> commandCallback) {
            super(DeviceCommandEvent.CMD_GET_BATTERY_VALUE);
            this.commandCallback = commandCallback;
            init();
        }

        @Override
        public byte[] serialize() {
            return MbtBluetoothSPP.assembleCodes(this.getIdentifier());
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public byte[] getData() {
            return null;
        }

        @Override
        public String getInvalidityError() {
            return null;
        }
    }
}
