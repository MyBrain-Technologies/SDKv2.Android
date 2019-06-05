package core.bluetooth.lowenergy;

import android.support.annotation.Keep;
import engine.SimpleRequestCallback;

/**
 * Mailbox commands and other headset commands related to EEG streaming are hold in this object
 */
@Keep
public interface DeviceCommands {

    class UpdateSerialNumber extends DeviceCommand{
        private String serialNumber;

        public UpdateSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public UpdateSerialNumber(String serialNumber, SimpleRequestCallback<byte[]> responseCallback) {
            this.serialNumber = serialNumber;
            this.responseCallback = responseCallback;
        }

        public String getSerialNumber() {
            return serialNumber;
        }
    }

    class UpdateExternalName extends DeviceCommand{
        private String externalName;

        public UpdateExternalName(String externalName) {
            this.externalName = externalName;
        }

        public UpdateExternalName(String externalName, SimpleRequestCallback<byte[]> responseCallback) {
            this.externalName = externalName;
            this.responseCallback = responseCallback;
        }

        public String getExternalName() {
            return externalName;
        }
    }

    class UpdateProductName extends DeviceCommand{
        private String productName;

        public UpdateProductName(String productName) {
            this.productName = productName;
        }

        public UpdateProductName(String productName, SimpleRequestCallback<byte[]> responseCallback) {
            this.productName = productName;
            this.responseCallback = responseCallback;
        }

        public String getProductName() {
            return productName;
        }
    }


    class GetSystemStatus extends DeviceCommand{

        public GetSystemStatus(SimpleRequestCallback<byte[]> responseCallback) {
            this.responseCallback = responseCallback;
        }
    }

    class Reboot extends DeviceCommand{

        public Reboot() {
        }
        public Reboot(SimpleRequestCallback<byte[]> responseCallback) {
            this.responseCallback = responseCallback;
        }
    }

    class ConnectAudio extends DeviceCommand{

        public ConnectAudio() {
        }
        public ConnectAudio(SimpleRequestCallback<byte[]> responseCallback) {
            this.responseCallback = responseCallback;
        }
    }

    class DisconnectAudio extends DeviceCommand{

        public DisconnectAudio() {
        }

        public DisconnectAudio(SimpleRequestCallback<byte[]> responseCallback) {
            this.responseCallback = responseCallback;
        }
    }

}
