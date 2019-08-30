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
        this.eegPacketLength = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH;

    }

    public static short getBatteryPercentageFromByteValue(byte value){
        final short level;
        switch (value){
            case (byte) 0:
                level = 0;
                break;
            case (byte) 1:
                level = 15;
                break;
            case (byte) 2:
                level = 30;
                break;
            case (byte) 3:
                level = 50;
                break;
            case (byte) 4:
                level = 65;
                break;
            case (byte) 5:
                level = 85;
                break;
            case (byte) 6:
                level = 100;
                break;
            case (byte) 0xFF:
            default:
                level = -1;
                break;
        }
        return level;
    }

    /**
     * Gets the version of the firmware
     * @return the version of the firmware
     */
    @Nullable
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
    @Nullable
    public final String getHardwareVersion() {
        return this.hardwareVersion;
    }

    public final int getSampRate() {return this.sampRate;}

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

    public static boolean hasMelomindName(BluetoothDevice device) {
        return (device != null
                && device.getName() != null
                && isDeviceNameValidForMelomind(device.getName()));
    }

    public static boolean isDeviceNameValidForMelomind(String deviceName){
        return (deviceName.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX_LEGACY) || deviceName.startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX));
    }

    @Override
    public void setExternalName(String externalName) {
        super.setExternalName(externalName);
    }

    @Override
    public String getExternalName() {
        return super.getExternalName();
    }
}
