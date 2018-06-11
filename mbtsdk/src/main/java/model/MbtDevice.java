package model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import features.MbtAcquisitionLocations;

/**
 * Created by manon on 10/10/16.
 // */
@Keep
public abstract class MbtDevice {
    String productName;
    String hardwareVersion;
    String firmwareVersion;
    String serialNumber;
    String deviceAddress;

    int sampRate;
    int nbChannels;
    ArrayList<MbtAcquisitionLocations> acquisitionLocations;
    ArrayList<MbtAcquisitionLocations> referencesLocations;
    ArrayList<MbtAcquisitionLocations> groundsLocation;
    int eegPacketLength;

    private InternalConfig internalConfig;

    private BluetoothDevice bluetoothDevice; //TODO à enlever si non pertinent

    MbtDevice(){

    }

    MbtDevice(@NonNull final String productName, @NonNull final String hardwareVersion,
              @NonNull final String firmwareVersion,
              @NonNull final String serialNumber,
              @NonNull int sampRate,
              @NonNull int nbChannels,
              @NonNull ArrayList<MbtAcquisitionLocations> acquisitionLocations,
              @NonNull ArrayList<MbtAcquisitionLocations> referencesLocations,
              @NonNull ArrayList<MbtAcquisitionLocations> groundsLocation,
              @NonNull int eegPacketLength) {

        this.firmwareVersion = firmwareVersion;
        this.productName = productName;
        this.serialNumber = serialNumber;
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
        this.serialNumber = null;
        this.hardwareVersion = null;
        this.bluetoothDevice = null;

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
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Gets the version of the hardware used
     * @return the hardware version
     */
    @NonNull
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

    @NonNull
    public int getSampRate() {return this.sampRate;}

    @NonNull
    public int getNbChannels() {return this.nbChannels;}

    @NonNull
    public ArrayList<MbtAcquisitionLocations> getAcquisitionLocations() {return this.acquisitionLocations;}

    @NonNull
    public ArrayList<MbtAcquisitionLocations> getReferencesLocations() {return this.referencesLocations;}

    @NonNull
    public ArrayList<MbtAcquisitionLocations> getGroundsLocation() {return this.groundsLocation;}

    @NonNull
    public int getEegPacketLength() {return this.eegPacketLength;}

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

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        this.bluetoothDevice = device;
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
