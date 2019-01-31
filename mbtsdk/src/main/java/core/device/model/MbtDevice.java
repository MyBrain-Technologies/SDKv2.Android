package core.device.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import features.MbtAcquisitionLocations;
import features.MbtFeatures;

/**
 * Created by manon on 10/10/16.
 // */
@Keep
public abstract class MbtDevice {

    public static final String DEFAULT_FW_VERSION = "0.0.0";

    String productName;
    @Nullable
    String hardwareVersion;
    @Nullable
    String firmwareVersion;
    @Nullable
    String serialNumber;
    @Nullable
    String externalName;

    String deviceAddress;

    int sampRate;

    int nbChannels;

    List<MbtAcquisitionLocations> acquisitionLocations;
    List<MbtAcquisitionLocations> referencesLocations;
    List<MbtAcquisitionLocations> groundsLocation;

    private InternalConfig internalConfig;

    @NonNull
    private final BluetoothDevice bluetoothDevice; //TODO à enlever si non pertinent

    MbtDevice(BluetoothDevice bluetoothDevice){
        this.bluetoothDevice = bluetoothDevice;
        this.deviceAddress = bluetoothDevice.getAddress();
        this.productName = bluetoothDevice.getName();
        this.internalConfig = null;
        this.externalName = MbtFeatures.MELOMIND_DEVICE_NAME;
    }

    /**
     * Gets the version of the firmware
     * @return the version of the firmware
     */
    @NonNull
    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Gets the commercial name of the device/headset - type of hardware
     * @return the name of the product
     */
    @NonNull
    public String getProductName() {
        return this.productName;
    }


    /**
     * Gets the devide unique ID
     * @return the device unique ID
     */
    @Nullable
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Gets the version of the hardware used
     * @return the hardware version
     */
    @Nullable
    public String getHardwareVersion() {
        return this.hardwareVersion;
    }

    /**
     * Gets the device MAC address
     * @return the device MAC address
     */
    @NonNull
    public String getDeviceAddress(){
        return deviceAddress;
    }

    public int getSampRate() {return this.sampRate;}

    public int getNbChannels() {return this.nbChannels;}

    @NonNull
    public List<MbtAcquisitionLocations> getAcquisitionLocations() {return this.acquisitionLocations;}

    @NonNull
    public List<MbtAcquisitionLocations> getReferencesLocations() {return this.referencesLocations;}

    @NonNull
    public List<MbtAcquisitionLocations> getGroundsLocation() {return this.groundsLocation;}

    public void setHardwareVersion(@NonNull final String hardwareVersion) {this.hardwareVersion = hardwareVersion;}

    public void setFirmwareVersion(@NonNull final String firmwareVersion) {this.firmwareVersion = firmwareVersion;}

    public void setSerialNumber(@NonNull final String serialNumber) {this.serialNumber = serialNumber;}

    public void setProductName(@NonNull final String productName){
        this.productName = productName;
    }

    public void setDeviceAddress(@NonNull final String deviceAddress){
        this.deviceAddress = deviceAddress;
    }

    public InternalConfig getInternalConfig() {
        return internalConfig;
    }

    public void setInternalConfig(InternalConfig internalConfig) {
        this.internalConfig = internalConfig;
    }

    public void setExternalName(@Nullable String externalName){
        this.externalName = externalName;
    }

    @Nullable
    public String getExternalName(){
        return externalName;
    }

    @NonNull
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public final static class InternalConfig{
        private byte notchFilterConfig;
        private byte bandPassFilterConfig;
        private byte gainValue;
        private byte statusBytes;
        private byte nbPackets;

        public InternalConfig(Byte[] configFromHeadset){
            if (configFromHeadset.length >= 5){
                notchFilterConfig = configFromHeadset[0];
                bandPassFilterConfig = configFromHeadset[1];
                gainValue = configFromHeadset[2];
                statusBytes = configFromHeadset[4];
                nbPackets = configFromHeadset[5];
            }
        }

        public byte getNotchFilterConfig() {
            return notchFilterConfig;
        }

        public byte getBandPassFilterConfig() {
            return bandPassFilterConfig;
        }

        public byte getGainValue() {
            return gainValue;
        }

        public byte getStatusBytes() {
            return statusBytes;
        }

        public byte getNbPackets() {
            return nbPackets;
        }


        @Override
        public String toString() {
            return "InternalConfig{" +
                    "notchFilterConfig=" + notchFilterConfig +
                    ", bandPassFilterConfig=" + bandPassFilterConfig +
                    ", gainValue=" + gainValue +
                    ", statusBytes=" + statusBytes +
                    ", nbPackets=" + nbPackets +
                    '}';
        }
    }
}
