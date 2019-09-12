package core.device.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import features.MbtAcquisitionLocations;
import features.MbtDeviceType;
import features.MbtFeatures;

/**
 * Created by manon on 10/10/16.
 */
@Keep
public class VProDevice extends MbtDevice{

    public VProDevice(@NonNull final BluetoothDevice device){
        super(device, MbtDeviceType.VPRO);
        this.externalName = MbtFeatures.VPRO_DEVICE_NAME;
        this.productName = device.getName();
        this.deviceAddress = device.getAddress();

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

    /**
     * Gets the version of the hardware used
     * @return the heardware version
     */
    @NonNull
    public final String getHardwareVersion() {
        return this.hardwareVersion;
    }

    @NonNull
    public final int getSampRate() {return this.getInternalConfig().getSampRate();}

    @NonNull
    public final int getNbChannels() {return this.nbChannels;}

    @Override
    public void setInternalConfig(Byte[] rawConfig) {
        //Returned : [payload_length(2B), 0x0E, 0x00, 0x00, 0x00, num_eeg_channels, amp_gain, ads_freq_sampling];
        nbChannels = rawConfig[6];
        super.setInternalConfig(convertRawInternalConfig(rawConfig));
    }

    public static InternalConfig convertRawInternalConfig(Byte[] rawConfig) {
        return new InternalConfig(
                rawConfig[7],
                rawConfig[8]);
    }

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

}
