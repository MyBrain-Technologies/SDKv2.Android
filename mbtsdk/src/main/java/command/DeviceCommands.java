package command;

import android.support.annotation.Keep;
import engine.SimpleRequestCallback;
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
    class UpdateSerialNumber extends DeviceCommand {

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
         * call the {@link UpdateSerialNumber}(String serialNumber, {@linkSimpleRequestCallback<byte[]>)} constructor.
         */
        public UpdateSerialNumber(String serialNumber) {
            super(DeviceCommandEvents.MBX_SET_SERIAL_NUMBER,
                    DeviceCommandEvents.MBX_SET_SERIAL_NUMBER_ADDITIONAL);
            this.serialNumber = serialNumber;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its serial number.
         * The new serial number is stored by the headset if the command succeeds
         * @param serialNumber is the new serial number value to set
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the update command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link UpdateSerialNumber}(String serialNumber) constructor
         */
        public UpdateSerialNumber(String serialNumber, SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_SET_SERIAL_NUMBER,
                    DeviceCommandEvents.MBX_SET_SERIAL_NUMBER_ADDITIONAL);
            new UpdateSerialNumber(serialNumber);
            this.responseCallback = responseCallback;
        }

        @Override
        public SimpleRequestCallback<byte[]> getResponseCallback() {
            return super.getResponseCallback();
        }

        @Override
        public boolean isValid() {
            return serialNumber != null && !serialNumber.isEmpty();
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) {
            serialNumber = null;
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
    class UpdateExternalName extends DeviceCommand {

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
         * call the {@link UpdateExternalName}({@link SimpleRequestCallback}<byte[]>) constructor.
         * @param externalName the new external name value to set
         */
        public UpdateExternalName(String externalName) {
            super(DeviceCommandEvents.MBX_SET_SERIAL_NUMBER,
                    DeviceCommandEvents.MBX_SET_EXTERNAL_NAME_ADDITIONAL);
            this.externalName = externalName;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its external name.
         * The new external name is stored by the headset if the command succeeds.
         * @param externalName the new external name value to set
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the update command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned response,
         * call the {@link UpdateExternalName}(String externalName) constructor
         */
        public UpdateExternalName(String externalName, SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_SET_SERIAL_NUMBER,
                    DeviceCommandEvents.MBX_SET_EXTERNAL_NAME_ADDITIONAL);
            new UpdateExternalName(externalName);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return externalName != null && !externalName.isEmpty();
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) {
            externalName = null;
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
    class UpdateProductName extends DeviceCommand {

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
         * call the {@link UpdateProductName}({@link SimpleRequestCallback}<byte[]>) constructor.
         * @param productName is the new product name value to set
         */
        public UpdateProductName(String productName) {
            super(DeviceCommandEvents.MBX_SET_PRODUCT_NAME);
            this.productName = productName;
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to change its product name.
         * The new product name is stored by the headset if the command succeeds.
         * @param productName the new product name value to set
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the update command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * call the {@link UpdateProductName}(String productName) constructor
         */
        public UpdateProductName(String productName, SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_SET_PRODUCT_NAME);
            new UpdateProductName(productName);
            this.responseCallback = responseCallback;
        }

        @Override
        public boolean isValid() {
            return productName != null && !productName.isEmpty();
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) {
            productName = null;
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
    class GetSystemStatus extends DeviceCommand {

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to get the device system status such as :
         * the processor status,
         * the external memory status,
         * the audio status,
         * and the ADS status.
         * The device system status is returned by the headset if the command succeeds.
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the get command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * Each status is returned in one byte of the raw response array.
         */
        public GetSystemStatus(SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_SYS_GET_STATUS);
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

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to reboot the headset after the next disconnection.
     * No response is returned by the headset if the command succeeds.
     */
    @Keep
    class Reboot extends DeviceCommand {

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to reboot the headset after the next disconnection.
         * No response is returned by the headset if the command succeeds.
         */
        public Reboot() {
            super(DeviceCommandEvents.MBX_SYS_REBOOT_EVT,
                    DeviceCommandEvents.MBX_SYS_REBOOT_EVT_ADDITIONAL);
            init();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void onError(BaseError error, String additionnalInfo) { }

        @Override
        public byte[] getData() {
            return null;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to establish a Bluetooth connection for audio streaming.
     * The connection status is returned by the headset if the command succeeds.
     */
    @Keep
    class ConnectAudio extends DeviceCommand {

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth connection for audio streaming.
         * The connection status is returned by the headset if the command succeeds.
         * sent by the headset to the SDK once the connect command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link ConnectAudio}({@link SimpleRequestCallback}<byte[]> responseCallback) constructor
         */
        public ConnectAudio() {
            super(DeviceCommandEvents.MBX_CONNECT_IN_A2DP,
                    DeviceCommandEvents.MBX_CONNECT_IN_A2DP_ADDITIONAL);
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth connection for audio streaming.
         * The connection status is returned by the headset if the command succeeds.
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the connect command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link ConnectAudio}() constructor
         */
        public ConnectAudio(SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_CONNECT_IN_A2DP,
                    DeviceCommandEvents.MBX_CONNECT_IN_A2DP_ADDITIONAL);
            new ConnectAudio();
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
            return null;
        }

    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to establish a Bluetooth disconnection for audio streaming.
     * The disconnection status is returned by the headset if the command succeeds.
     */
    @Keep
    class DisconnectAudio extends DeviceCommand {

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth disconnection for audio streaming.
         * The disconnection status is returned by the headset if the command succeeds.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link DisconnectAudio}({@link SimpleRequestCallback}<byte[]> responseCallback) constructor
         */
        public DisconnectAudio() {
            super(DeviceCommandEvents.MBX_DISCONNECT_IN_A2DP,
                    DeviceCommandEvents.MBX_DISCONNECT_IN_A2DP_ADDITIONAL);
            init();
        }

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth disconnection for audio streaming.
         * The disconnection status is returned by the headset if the command succeeds.
         * @param responseCallback is a {@link SimpleRequestCallback} object
         * that provides a callback for the returned raw response
         * sent by the headset to the SDK once the disconnect command is received.
         * This raw response is a byte array that has be to converted to be readable.
         * If you're not interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link DisconnectAudio}() constructor
         */
        public DisconnectAudio(SimpleRequestCallback<byte[]> responseCallback) {
            super(DeviceCommandEvents.MBX_DISCONNECT_IN_A2DP,
                    DeviceCommandEvents.MBX_DISCONNECT_IN_A2DP_ADDITIONAL);
            new DisconnectAudio();
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
            return null;
        }

    }

}
