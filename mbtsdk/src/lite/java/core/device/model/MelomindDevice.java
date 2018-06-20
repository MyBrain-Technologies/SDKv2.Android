package core.device.model;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import features.MbtAcquisitionLocations;

/**
 * Created by manon on 10/10/16.
 */
@Keep
public class MelomindDevice extends MbtDevice{

    MelomindDevice(){
        super();
    }

    MelomindDevice(@NonNull final String productName, @NonNull final String hardwareVersion,
                   @NonNull final String firmwareVersion,
                   @NonNull final String deviceId,
                   @NonNull final int sampRate,
                   @NonNull int nbChannels,
                   @NonNull ArrayList<MbtAcquisitionLocations> acquisitionLocations,
                   @NonNull ArrayList<MbtAcquisitionLocations> referencesLocations,
                   @NonNull ArrayList<MbtAcquisitionLocations> groundsLocation,
                   @NonNull int eegPacketLength) {
        super(productName, hardwareVersion, firmwareVersion, deviceId, sampRate, nbChannels, acquisitionLocations, referencesLocations, groundsLocation, eegPacketLength);

        this.firmwareVersion = firmwareVersion;
        this.productName = productName;
        this.serialNumber = deviceId;
        this.hardwareVersion = hardwareVersion;

        this.sampRate = sampRate;
        this.nbChannels = nbChannels;
        this.acquisitionLocations = acquisitionLocations;
        this.groundsLocation = groundsLocation;
        this.referencesLocations = referencesLocations;
        this.eegPacketLength = eegPacketLength;
    }


    public MelomindDevice(@NonNull String productName, @NonNull int sampRate, @NonNull int nbChannels, @NonNull ArrayList<MbtAcquisitionLocations> acquisitionLocations, @NonNull ArrayList<MbtAcquisitionLocations> referencesLocations, @NonNull ArrayList<MbtAcquisitionLocations> groundsLocation, @NonNull int eegPacketLength) {
        super(productName, sampRate, nbChannels, acquisitionLocations, referencesLocations, groundsLocation, eegPacketLength);
        this.firmwareVersion = null;
        this.productName = productName;
        this.serialNumber = null;
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
    public final String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Gets the commercial name of the device/headset - type of hardware
     * @return the name of the product
     */
    @NonNull
    public final String getProductName() {
        return this.productName;
    }


    /**
     * Gets the devide unique ID
     * @return the device unique ID
     */
    @Nullable
    public final String getDeviceId() {
        return this.serialNumber;
    }

    @NonNull
    @Override
    public String getDeviceAddress() {
        return super.getDeviceAddress();
    }

    @Override
    public void setProductName(@NonNull String productName) {
        super.setProductName(productName);
    }

    @Override
    public void setDeviceAddress(@NonNull String deviceAddress) {
        super.setDeviceAddress(deviceAddress);
    }


    @Override
    public void setInternalConfig(InternalConfig internalConfig) {
        super.setInternalConfig(internalConfig);
    }

    @Override
    public InternalConfig getInternalConfig() {
        return super.getInternalConfig();
    }

    /**
     * Gets the version of the hardware used
     * @return the heardware version
     */
    @NonNull
    public final String getHardwareVersion() {
        return this.hardwareVersion;
    }

    @NonNull
    public final int getSampRate() {return this.sampRate;}

    @NonNull
    public final int getNbChannels() {return this.nbChannels;}

    @NonNull
    public final ArrayList<MbtAcquisitionLocations> getAcquisitionLocations() {return this.acquisitionLocations;}

    @NonNull
    public final ArrayList<MbtAcquisitionLocations> getReferencesLocations() {return this.referencesLocations;}

    @NonNull
    public final ArrayList<MbtAcquisitionLocations> getGroundsLocation() {return this.groundsLocation;}

    @NonNull
    public final int getEegPacketLength() {return this.eegPacketLength;}

    public void setHardwareVersion(@NonNull final String hardwareVersion) {this.hardwareVersion = hardwareVersion;}

    public void setFirmwareVersion(@NonNull final String firmwareVersion) {this.firmwareVersion = firmwareVersion;}

    public void setDeviceId(@NonNull final String deviceId) {this.serialNumber = deviceId;}

    public void updateConfiguration(ArrayList<MbtAcquisitionLocations> acquisitionLocationsArrayList, ArrayList<MbtAcquisitionLocations> referenceArrayList, ArrayList<MbtAcquisitionLocations> grounLocationsArrayList){
        this.acquisitionLocations = acquisitionLocationsArrayList;
        this.referencesLocations = referenceArrayList;
        this.groundsLocation = grounLocationsArrayList;
    }

}
