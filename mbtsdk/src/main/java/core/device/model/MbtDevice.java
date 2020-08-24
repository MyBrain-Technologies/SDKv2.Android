package core.device.model;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import features.MbtAcquisitionLocations;
import features.MbtDeviceType;
import features.MbtFeatures;

/**
 * Created by manon on 10/10/16.
 // */
@Keep
public abstract class MbtDevice implements Serializable {

    public static final String DEFAULT_PRODUCT_NAME = "melomind";
    public static final String DEFAULT_HW_VERSION = "0.0.0";
    public static final String DEFAULT_FW_VERSION = "0.0.0";
    public static final String DEFAULT_DEVICEID = "1010100000";

    private static final int DEFAULT_NB_CHANNEL = 2;

    private final static ArrayList<MbtAcquisitionLocations> DEFAULT_LOCATIONS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.P3, MbtAcquisitionLocations.P4));
    private final static ArrayList<MbtAcquisitionLocations> DEFAULT_REFERENCES = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M1));
    private final static ArrayList<MbtAcquisitionLocations> DEFAULT_GROUNDS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M2));

    protected MbtDeviceType deviceType;

    /**
     * Bluetooth Low Energy name
     */
    protected String productName = DEFAULT_PRODUCT_NAME;
    @Nullable
    /**
     * Hardware version number
     */
    protected MbtVersion hardwareVersion = new MbtVersion(DEFAULT_HW_VERSION);

    /**
     * Firmware version number
     */
    @Nullable
    protected MbtVersion firmwareVersion = new MbtVersion(DEFAULT_FW_VERSION);

    /**
     * Serial number
     */
    @Nullable
    protected String serialNumber;

    /**
     * Bluetooth A2DP /external name (QR code)
     */
    @Nullable
    protected String externalName = DEFAULT_PRODUCT_NAME;

    /**
     * Bluetooth Low energy device address
     */
    protected String deviceAddress;
    protected String deviceId = DEFAULT_DEVICEID;

    /**
     * Bluetooth A2DP device address
     */
    @Nullable
    protected String audioDeviceAddress;

    protected int eegPacketLength = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH;

    protected List<MbtAcquisitionLocations> acquisitionLocations = DEFAULT_LOCATIONS;
    protected List<MbtAcquisitionLocations> referencesLocations = DEFAULT_REFERENCES;
    protected List<MbtAcquisitionLocations> groundsLocation = DEFAULT_GROUNDS;

    protected InternalConfig internalConfig;

    MbtDevice(){
        this.internalConfig = new InternalConfig(DEFAULT_NB_CHANNEL);
        this.internalConfig.sampRate = MbtFeatures.DEFAULT_SAMPLE_RATE;
    }

    public MbtDevice(String address, String name, @NonNull MbtDeviceType deviceType, int nbChannels){
        this.internalConfig = new InternalConfig(nbChannels);
        this.deviceType = deviceType;
        this.deviceAddress = address;
        this.productName = name;;
    }

    public MbtDevice(BluetoothDevice bluetoothDevice, @NonNull MbtDeviceType deviceType, int nbChannels){
        this.internalConfig = new InternalConfig(nbChannels);
        this.deviceType = deviceType;
        this.deviceAddress = bluetoothDevice.getAddress();
        this.productName = bluetoothDevice.getName();;
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

    public String getDeviceId() {
        return deviceId;
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
        protected int sampRate;
        protected int nbChannels;

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
