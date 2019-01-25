package core.device.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import features.MbtAcquisitionLocations;
import features.MbtFeatures;

/**
 * Created by manon on 10/10/16.
 */
@Keep
public class MelomindDevice extends MbtDevice{

    public MelomindDevice(@NonNull final BluetoothDevice device){
        super(device);
        this.acquisitionLocations = Arrays.asList(MbtAcquisitionLocations.P3, MbtAcquisitionLocations.P4);
        this.groundsLocation = Arrays.asList(MbtAcquisitionLocations.M2);
        this.referencesLocations = Arrays.asList(MbtAcquisitionLocations.M1);
        this.nbChannels = MbtFeatures.MELOMIND_NB_CHANNELS;
        this.sampRate = MbtFeatures.DEFAULT_SAMPLE_RATE;
        this.firmwareVersion = "0.0.0";
        this.hardwareVersion = "0.0.0";
        this.serialNumber = "0000000000";
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
    public final String getSerialNumber() {
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
    public final List<MbtAcquisitionLocations> getAcquisitionLocations() {return this.acquisitionLocations;}

    @NonNull
    public final List<MbtAcquisitionLocations> getReferencesLocations() {return this.referencesLocations;}

    @NonNull
    public final List<MbtAcquisitionLocations> getGroundsLocation() {return this.groundsLocation;}

    public void setHardwareVersion(@NonNull final String hardwareVersion) {this.hardwareVersion = hardwareVersion;}

    public void setFirmwareVersion(@NonNull final String firmwareVersion) {this.firmwareVersion = firmwareVersion;}

    public void setSerialNumber(@NonNull final String deviceId) {this.serialNumber = deviceId;}

    public void updateConfiguration(ArrayList<MbtAcquisitionLocations> acquisitionLocationsArrayList, ArrayList<MbtAcquisitionLocations> referenceArrayList, ArrayList<MbtAcquisitionLocations> grounLocationsArrayList){
        this.acquisitionLocations = acquisitionLocationsArrayList;
        this.referencesLocations = referenceArrayList;
        this.groundsLocation = grounLocationsArrayList;
    }

    public static boolean isMelomind(BluetoothDevice device) {
        return (device != null && device.getName() != null && device.getName().contains(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX));
    }
}
