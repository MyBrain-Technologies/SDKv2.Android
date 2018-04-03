package model;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by manon on 10/10/16.
 // */
@Keep
public abstract class MbtDevice {
    String productName;
    String hardwareVersion;
    String firmwareVersion;
    String deviceId;
    String deviceAddress;

    int sampRate;
    int nbChannels;
    ArrayList<MbtAcquisitionLocations> acquisitionLocations;
    ArrayList<MbtAcquisitionLocations> referencesLocations;
    ArrayList<MbtAcquisitionLocations> groundsLocation;
    int eegPacketLength;

    private InternalConfig internalConfig;

    MbtDevice(){

    }

    MbtDevice(@NonNull final String productName, @NonNull final String hardwareVersion,
              @NonNull final String firmwareVersion,
              @NonNull final String deviceId,
              @NonNull int sampRate,
              @NonNull int nbChannels,
              @NonNull ArrayList<MbtAcquisitionLocations> acquisitionLocations,
              @NonNull ArrayList<MbtAcquisitionLocations> referencesLocations,
              @NonNull ArrayList<MbtAcquisitionLocations> groundsLocation,
              @NonNull int eegPacketLength) {

        this.firmwareVersion = firmwareVersion;
        this.productName = productName;
        this.deviceId = deviceId;
        this.hardwareVersion = hardwareVersion;

        this.sampRate = sampRate;
        this.nbChannels = nbChannels;
        this.acquisitionLocations = acquisitionLocations;
        this.groundsLocation = groundsLocation;
        this.referencesLocations = referencesLocations;
        this.eegPacketLength = eegPacketLength;
    }

    MbtDevice(@NonNull final String productName,
              @NonNull final int sampRate,
              @NonNull int nbChannels,
              @NonNull ArrayList<MbtAcquisitionLocations> acquisitionLocations,
              @NonNull ArrayList<MbtAcquisitionLocations> referencesLocations,
              @NonNull ArrayList<MbtAcquisitionLocations> groundsLocation,
              @NonNull int eegPacketLength) {

        this.firmwareVersion = null;
        this.productName = productName;
        this.deviceId = null;
        this.hardwareVersion = null;

        this.sampRate = sampRate;
        this.nbChannels = nbChannels;
        this.groundsLocation = groundsLocation;
        this.referencesLocations = referencesLocations;
        this.acquisitionLocations = acquisitionLocations;
        this.eegPacketLength = eegPacketLength;
    }


    /**
     * Gets the version of the firmware
     * @return the version of the firmware
     */
    @NonNull
    protected String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Gets the commercial name of the device/headset - type of hardware
     * @return the name of the product
     */
    @NonNull
    protected String getProductName() {
        return this.productName;
    }


    /**
     * Gets the devide unique ID
     * @return the device unique ID
     */
    protected String getDeviceId() {
        return this.deviceId;
    }

    /**
     * Gets the version of the hardware used
     * @return the hardware version
     */
    @NonNull
    protected String getHardwareVersion() {
        return this.hardwareVersion;
    }

    /**
     * Gets the device MAC address
     * @return the device MAC address
     */
    @NonNull
    protected String getDeviceAddress(){
        return deviceAddress;
    }

    @NonNull
    protected int getSampRate() {return this.sampRate;}

    @NonNull
    protected int getNbChannels() {return this.nbChannels;}

    @NonNull
    protected ArrayList<MbtAcquisitionLocations> getAcquisitionLocations() {return this.acquisitionLocations;}

    @NonNull
    protected ArrayList<MbtAcquisitionLocations> getReferencesLocations() {return this.referencesLocations;}

    @NonNull
    protected ArrayList<MbtAcquisitionLocations> getGroundsLocation() {return this.groundsLocation;}

    @NonNull
    protected int getEegPacketLength() {return this.eegPacketLength;}

    protected void setHardwareVersion(@NonNull final String hardwareVersion) {this.hardwareVersion = hardwareVersion;}

    protected void setFirmwareVersion(@NonNull final String firmwareVersion) {this.firmwareVersion = firmwareVersion;}

    protected void setDeviceId(@NonNull final String deviceId) {this.deviceId = deviceId;}

    protected void setProductName(@NonNull final String productName){
        this.productName = productName;
    }

    protected void setDeviceAddress(@NonNull final String deviceAddress){
        this.deviceAddress = deviceAddress;
    }

    protected InternalConfig getInternalConfig() {
        return internalConfig;
    }

    protected void setInternalConfig(InternalConfig internalConfig) {
        this.internalConfig = internalConfig;
    }

    @Keep
    public enum MbtAcquisitionLocations {
        Fpz,
        Fp1,
        Fp2,

        AF7,
        AF3,
        AFz,
        AF4,
        AF8,

        A2,

        F9,
        F7,
        F5,
        F3,
        F1,
        Fz,
        F2,
        F4,
        F6,
        F8,
        F10,

        FT9,
        FT7,
        FC5,
        FC3,
        FC1,
        FCz,
        FC2,
        FC4,
        FC6,
        FT8,
        FT10,

        T7,
        C5,
        C3,
        C1,
        Cz,
        C2,
        C4,
        C6,
        T8,

        TP9,
        TP7,
        CP5,
        CP3,
        CP1,
        CPz,
        CP2,
        CP4,
        CP6,
        TP8,
        TP10,

        P9,
        P7,
        P5,
        P3,
        P1,
        Pz,
        P2,
        P4,
        P6,
        P8,
        P10,

        PO3,
        POz,
        PO4,

        PO7,
        O1,
        Oz,
        O2,
        PO8,

        PO9,
        O9,
        Iz,
        O10,
        PO10,

        M1, // Mastoid 1
        M2,  // Mastoid 2

        ACC,

        EXT1,
        EXT2,
        EXT3,

        NULL1,
        NULL2,
        NULL3,


    }

    final static class InternalConfig{
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
    }

}
