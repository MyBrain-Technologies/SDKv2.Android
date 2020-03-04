package core.device.model;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import features.MbtAcquisitionLocations;
import features.MbtDeviceType;
import features.MbtFeatures;

/**
 * Created by manon on 10/10/16.
 // */
@Keep
public abstract class MbtDevice implements Serializable {

    public static final String DEFAULT_FW_VERSION = "0.0.0";

    public MbtDeviceType deviceType;

    /**
     * Bluetooth Low Energy name
     */
    String productName;
    @Nullable
    /**
     * Hardware version number
     */
    MbtVersion hardwareVersion;

    /**
     * Firmware version number
     */
    @Nullable
    MbtVersion firmwareVersion;

    /**
     * Serial number
     */
    @Nullable
    String serialNumber;

    /**
     * Bluetooth A2DP /external name (QR code)
     */
    @Nullable
    String externalName;

    /**
     * Bluetooth Low energy device address
     */
    String deviceAddress;

    /**
     * Bluetooth A2DP device address
     */
    @Nullable
    private String audioDeviceAddress;

    private int eegPacketLength;

    List<MbtAcquisitionLocations> acquisitionLocations;
    List<MbtAcquisitionLocations> referencesLocations;
    List<MbtAcquisitionLocations> groundsLocation;

    private InternalConfig internalConfig;

    MbtDevice(String address, String name, @NonNull MbtDeviceType deviceType, int nbChannels){
        this.internalConfig = new InternalConfig(nbChannels);
        this.deviceType = deviceType;
        this.deviceAddress = address;
        this.productName = name;
        this.eegPacketLength = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH;
        this.firmwareVersion = new MbtVersion("0.0.0");
        this.hardwareVersion = new MbtVersion("0.0.0");
    }

    MbtDevice(BluetoothDevice bluetoothDevice, @NonNull MbtDeviceType deviceType, int nbChannels){
        this.internalConfig = new InternalConfig(nbChannels);
        this.deviceType = deviceType;
        this.deviceAddress = bluetoothDevice.getAddress();
        this.productName = bluetoothDevice.getName();
        this.eegPacketLength = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH;
        this.firmwareVersion = new MbtVersion("0.0.0");
        this.hardwareVersion = new MbtVersion("0.0.0");
    }

    /**
     * Gets the version of the firmware
     * @return the version of the firmware
     */
    @NonNull
    public MbtVersion getFirmwareVersion() {
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
    public MbtVersion getHardwareVersion() {
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

    public int getSampRate() {return this.internalConfig.sampRate;}

    public int getNbChannels() {return this.internalConfig.nbChannels;}

    public int getEegPacketLength() {
        return eegPacketLength;
    }

    @NonNull
    public List<MbtAcquisitionLocations> getAcquisitionLocations() {return this.acquisitionLocations;}

    @NonNull
    public List<MbtAcquisitionLocations> getReferencesLocations() {return this.referencesLocations;}

    @NonNull
    public List<MbtAcquisitionLocations> getGroundsLocation() {return this.groundsLocation;}

    public void setHardwareVersion(@NonNull final String hardwareVersion) {this.hardwareVersion = new MbtVersion(hardwareVersion);}
    public void setHardwareVersion(@NonNull final MbtVersion hardwareVersion) {this.hardwareVersion = hardwareVersion;}

    public void setFirmwareVersion(@NonNull final MbtVersion firmwareVersion) {this.firmwareVersion = firmwareVersion;}

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

    public void setInternalConfig(InternalConfig internalConfig){
        this.internalConfig = internalConfig;
        if(eegPacketLength != getSampRate());
            eegPacketLength = getSampRate();
    }

    public void setNbChannels(int nbChannels) {
        this.internalConfig.nbChannels = nbChannels;
    }

    public void setAcquisitionLocations(List<MbtAcquisitionLocations> acquisitionLocations) {
        this.acquisitionLocations = acquisitionLocations;
    }

    public void setReferencesLocations(List<MbtAcquisitionLocations> referencesLocations) {
        this.referencesLocations = referencesLocations;
    }

    public void setGroundsLocation(List<MbtAcquisitionLocations> groundsLocation) {
        this.groundsLocation = groundsLocation;
    }

    public void setExternalName(@Nullable String externalName){
        this.externalName = externalName;
    }

    @Nullable
    public String getExternalName(){
        return externalName;
    }

    public String getAudioDeviceAddress() {
        return audioDeviceAddress;
    }

    public void setAudioDeviceAddress(String audioDeviceAddress) {
        this.audioDeviceAddress = audioDeviceAddress;
    }

    public MbtDeviceType getDeviceType() {
        return deviceType;
    }

    public static class InternalConfig implements Serializable{
        public final byte DEFAULT = -1;

        byte notchFilterConfig;
        byte bandPassFilterConfig;
        byte gainValue;
        byte statusBytes;
        byte nbPackets;
        int sampRate;
        int nbChannels;

        public InternalConfig(int nbChannels) {
            this.nbChannels = nbChannels;
            this.sampRate = MbtFeatures.DEFAULT_SAMPLE_RATE;
        }

        public InternalConfig(int nbChannels, byte notchFilterConfig, byte bandPassFilterConfig, byte gainValue, byte statusBytes, byte nbPackets, int sampRate) {
            this.nbChannels = nbChannels;
            this.notchFilterConfig = notchFilterConfig;
            this.bandPassFilterConfig = bandPassFilterConfig;
            this.gainValue = gainValue;
            this.statusBytes = statusBytes;
            this.nbPackets = nbPackets;
            this.sampRate = sampRate;
        }

        public InternalConfig(int nbChannels, byte gainValue, byte[] sampRate) {
            this.nbChannels = nbChannels;
            this.notchFilterConfig = DEFAULT;
            this.bandPassFilterConfig = DEFAULT;
            this.gainValue = gainValue;
            this.statusBytes = DEFAULT;
            this.nbPackets = DEFAULT;
            this.sampRate = ByteBuffer.wrap(sampRate).order(ByteOrder.BIG_ENDIAN).getInt();
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

        public int getSampRate() {
            return sampRate;
        }

        public int getNbChannels() {
            return nbChannels;
        }



        @Override
        public String toString() {
            return "InternalConfig{" +
                    "notchFilterConfig=" + notchFilterConfig +
                    ", bandPassFilterConfig=" + bandPassFilterConfig +
                    ", gainValue=" + gainValue +
                    ", statusBytes=" + statusBytes +
                    ", nbPackets=" + nbPackets +
                    ", sampRate=" + sampRate +
                    ", nbChannels=" + nbChannels +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MbtDevice{" +
                "productName='" + productName + '\'' +
                ", hardwareVersion='" + hardwareVersion + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", externalName='" + externalName + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                ", acquisitionLocations=" + acquisitionLocations +
                ", referencesLocations=" + referencesLocations +
                ", groundsLocation=" + groundsLocation +
                ", internalConfig=" + internalConfig +
                ", audioBluetoothAddress=" + audioDeviceAddress +
                '}';
    }
}
