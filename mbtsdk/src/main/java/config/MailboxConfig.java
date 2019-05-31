package config;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Mailbox commands NOT related to EEG streaming are hold in this object
 */
@Keep
public class MailboxConfig extends DeviceCommandConfig {

    public final static String SERIAL_NUMBER_CONFIG = "SERIAL_NUMBER_CONFIG";
    public final static String EXTERNAL_NAME_CONFIG = "EXTERNAL_NAME_CONFIG";
    public final static String PRODUCT_NAME_CONFIG = "PRODUCT_NAME_CONFIG";
    public final static String SYSTEM_STATUS_CONFIG = "SYSTEM_STATUS_CONFIG";
    public final static String CONNECT_AUDIO_CONFIG = "CONNECT_AUDIO_CONFIG";
    public final static String DISCONNECT_AUDIO_CONFIG = "DISCONNECT_AUDIO_CONFIG";

    private String serialNumber;
    private String externalName;
    private String productName;
    private boolean connectAudio;
    private boolean disconnectAudio;
    private boolean systemStatus;

    private MailboxConfig(String serialNumber, String productName, String externalName, boolean connectA2D, boolean disconnectA2DP, boolean systemStatus) {

        this.serialNumber = serialNumber;
        this.productName = productName;
        this.externalName = externalName;
        this.connectAudio = connectA2D;
        this.disconnectAudio = disconnectA2DP;
        this.systemStatus = systemStatus;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getProductName() {
        return productName;
    }

    public String getExternalName() {
        return externalName;
    }

    public boolean connectAudio() {
        return connectAudio;
    }

    public boolean disconnectAudio() {
        return disconnectAudio;
    }

    public boolean getSystemStatus() {
        return systemStatus;
    }

    @Keep
    public static class Builder{

        @Nullable
        String serialNumber = null;
        @Nullable
        String productName = null;
        @Nullable
        String externalName = null;

        boolean connectAudio = false;
        boolean disconnectAudio = false;
        boolean systemStatus = false;

        public Builder(){}

        /**
         * Set a new value to the serial number of the headset currently connected in Bluetooth Low Energy
         */
        @NonNull
        public Builder setSerialNumber(String serialNumber){
            this.serialNumber = serialNumber;
            return this;
        }

        /**
         * Set a new value to the Bluetooth A2DP name of the headset currently connected in Bluetooth Low Energy
         */
        @NonNull
        public Builder setExternalName(String externalName){
            this.externalName = externalName;
            return this;
        }

        /**
         * Set a new value to the product name of the headset currently connected in Bluetooth Low Energy
         */
        @NonNull
        public Builder setProductName(String productName){
            this.productName = productName;
            return this;
        }

        /**
         * Establish an audio Bluetooth A2DP connection to the headset currently connected in Bluetooth Low Energy
         */
        @NonNull
        public Builder connectAudio(){
            this.connectAudio = true;
            return this;
        }

        /**
         * Establish an audio Bluetooth A2DP disconnection to the headset current connected in Bluetooth Low Energy
         */
        @NonNull
        public Builder disconnectAudio(){
            this.disconnectAudio = true;
            return this;
        }

        /**
         * Get the system status of the headset currently connected in Bluetooth Low Energy
         */
        @NonNull
        public Builder getSystemStatus(){
            this.systemStatus = true;
            return this;
        }

        @Nullable
        public MailboxConfig create() {
            return new MailboxConfig(serialNumber, productName, externalName, connectAudio, disconnectAudio, systemStatus);
        }
    }

    @Override
    public String toString() {
        return "MailboxConfig{" +
                "setSerialNumber=" + serialNumber +
                ", setProductName=" + productName +
                ", setExternalName=" + externalName +
                ", connectAudio=" + connectAudio +
                ", disconnectAudio=" + disconnectAudio +
                ", systemStatus=" + systemStatus +
                '}';
    }
}
