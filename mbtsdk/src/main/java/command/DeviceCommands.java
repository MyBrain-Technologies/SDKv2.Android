package command;

import android.support.annotation.Keep;
import engine.SimpleRequestCallback;

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
    class UpdateSerialNumber extends DeviceCommand{

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
            this.serialNumber = serialNumber;
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
            this.serialNumber = serialNumber;
            this.responseCallback = responseCallback;
        }

        public String getSerialNumber() {
            return serialNumber;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change its external name (QR code number).
     * The new external name is stored and returned by the headset if the command succeeds.
     */
    class UpdateExternalName extends DeviceCommand{

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
            this.externalName = externalName;
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
            this.externalName = externalName;
            this.responseCallback = responseCallback;
        }

        public String getExternalName() {
            return externalName;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to change its product name.
     * The new product name is stored and returned by the headset if the command succeeds.
     */
    class UpdateProductName extends DeviceCommand{

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
            this.productName = productName;
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
            this.productName = productName;
            this.responseCallback = responseCallback;
        }

        public String getProductName() {
            return productName;
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
    class GetSystemStatus extends DeviceCommand{

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
            this.responseCallback = responseCallback;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to reboot the headset after the next disconnection.
     * No response is returned by the headset if the command succeeds.
     */
    class Reboot extends DeviceCommand{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to reboot the headset after the next disconnection.
         * No response is returned by the headset if the command succeeds.
         */
        public Reboot() {
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to establish a Bluetooth connection for audio streaming.
     * The connection status is returned by the headset if the command succeeds.
     */
    class ConnectAudio extends DeviceCommand{

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
            this.responseCallback = responseCallback;
        }
    }

    /**
     * Mailbox command sent from the SDK to the connected headset
     * in order to establish a Bluetooth disconnection for audio streaming.
     * The disconnection status is returned by the headset if the command succeeds.
     */
    class DisconnectAudio extends DeviceCommand{

        /**
         * Mailbox command sent from the SDK to the connected headset
         * in order to establish a Bluetooth disconnection for audio streaming.
         * The disconnection status is returned by the headset if the command succeeds.
         * If you're interested in getting the returned raw response
         * sent by the headset to the SDK once the connect command is received,
         * call the {@link DisconnectAudio}({@link SimpleRequestCallback}<byte[]> responseCallback) constructor
         */
        public DisconnectAudio() {
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
            this.responseCallback = responseCallback;
        }
    }

}
