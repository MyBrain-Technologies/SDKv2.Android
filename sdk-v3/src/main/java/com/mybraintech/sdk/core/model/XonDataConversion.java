package com.mybraintech.sdk.core.model;

import androidx.annotation.NonNull;

import com.mybraintech.sdk.core.acquisition.EnumBluetoothProtocol;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


/**
 * copy and optimize from old sdk
 * <p>
 * MbtDataConversion is responsible for managing conversion from raw EEG data acquired by the Bluetooth headset into readable EEG values
 * <p>
 * optimize: remove static methods and use builder to create new instance
 */
public class XonDataConversion {
    private EnumBluetoothProtocol protocol;
    private int nbChannels;
    private float xonVoltage = (float) ((Math.pow(10, -6)));

    public XonDataConversion(byte gain) {

        this.protocol = EnumBluetoothProtocol.BLE;
        Timber.d("Dev_debug XonDataConversion constructor protocol:%s nbChannels:%s gain:%s", protocol, nbChannels, gain);
        this.nbChannels = 8;

    }

    /**
     * Converts the EEG raw data array into a user-readable EEG matrix
     *
     * @param rawEEGdataList the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @return the eeg data as float lists, with NaN values for missing data
     */
    @NonNull
    public ArrayList<ArrayList<Float>> convertRawDataToEEG(@NonNull List<RawEEGSample2> rawEEGdataList) {
        ArrayList<ArrayList<Float>> eegData = new ArrayList<>();

        Timber.d("Dev_debug XonDataConversion convertRawDataToEEG rawEEGdataList :%s", rawEEGdataList);
        ArrayList<Float> consolidatedEEGSample;

        for (RawEEGSample2 singleRawEEGdata : rawEEGdataList) { // for each acquisition of the headset
            consolidatedEEGSample = new ArrayList<>();
            Timber.d("Dev_debug XonDataConversion convertRawDataToEEG singleRawEEGdata :%s", singleRawEEGdata);
            if (singleRawEEGdata.getEegData() == null) {

                for (int nbChannel = 0; nbChannel < nbChannels; nbChannel++) {
                    consolidatedEEGSample.add(Float.NaN); //... fill the EEG data matrix with a NaN value for
                }
            } else {
                for (byte[] bytes : singleRawEEGdata.getEegData()) {
                    int temp = convert3BytesToSignedInt(bytes);

                    float dataTobeAdded = temp * xonVoltage;
                    consolidatedEEGSample.add(dataTobeAdded); //fill the EEG data matrix with the converted EEG data
                    //Here are data from sensors, whom need to be transformed to float
                }
            }
            eegData.add(consolidatedEEGSample);
        }


        return eegData;
    }

    public static int convert3BytesToSignedInt(byte[] byteArray) {
        if (byteArray.length != 3) {
            throw new IllegalArgumentException("Byte array must contain exactly 3 bytes");
        }

        // Extract the bytes
        int byte0 = byteArray[0] & 0xFF; // Ensure unsigned interpretation
        int byte1 = byteArray[1] & 0xFF; // Ensure unsigned interpretation
        int byte2 = byteArray[2] & 0xFF; // Ensure unsigned interpretation

        // Combine the bytes into a 24-bit value
        int combinedValue = (byte2 << 16) | (byte1 << 8) | byte0;

        // Perform sign extension if the most significant bit (bit 23) is set
        if ((combinedValue & 0x00800000) != 0) { // Check if bit 23 is set
            combinedValue |= 0xFF000000; // Extend the sign to 32 bits
        }

        // Divide by 256 (equivalent to shifting right by 8 bits)
        return combinedValue / 256;
    }

    private boolean isBle() {
        return protocol.equals(EnumBluetoothProtocol.BLE);
    }

}
