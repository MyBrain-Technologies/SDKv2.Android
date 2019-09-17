package core.device.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import config.MbtConfig;
import core.bluetooth.spp.MbtBluetoothSPP;
import features.MbtAcquisitionLocations;
import features.MbtDeviceType;
import features.MbtFeatures;

/**
 * Created by manon on 10/10/16.
 */
@Keep
public class VProDevice extends MbtDevice{

    public VProDevice(@NonNull final BluetoothDevice device){
        super(device, MbtDeviceType.VPRO, MbtFeatures.VPRO_NB_CHANNELS);
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

    //Returned : [0x00, 0x00, 0x00, num_eeg_channels, amp_gain, ads_freq_sampling];
    public static InternalConfig convertRawInternalConfig(Byte[] rawConfig) {
        int sampRateIndex = MbtBluetoothSPP.COMPRESS_NB_BYTES + MbtBluetoothSPP.PACKET_ID_NB_BYTES+2;
        byte[] sampRate = new byte[4];
        Arrays.fill(sampRate, (byte)0);

        byte[] tempSampRate = ArrayUtils.subarray(ArrayUtils.toPrimitive(rawConfig), sampRateIndex, rawConfig.length);
        int lengthDiff = sampRate.length - tempSampRate.length;

        if(lengthDiff > 0){
            for(int i=0; i<lengthDiff; i++){
                sampRate[lengthDiff+i] = tempSampRate[i];
            }
        }
        return new InternalConfig(
                rawConfig[MbtBluetoothSPP.COMPRESS_NB_BYTES + MbtBluetoothSPP.PACKET_ID_NB_BYTES],
                rawConfig[MbtBluetoothSPP.COMPRESS_NB_BYTES + MbtBluetoothSPP.PACKET_ID_NB_BYTES+1],
                sampRate);
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
