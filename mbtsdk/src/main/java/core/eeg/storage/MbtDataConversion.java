package core.eeg.storage;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;

import core.bluetooth.BtProtocol;
import features.MbtFeatures;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static core.eeg.MbtEEGManager.getBleNbBytes;
import static core.eeg.MbtEEGManager.getSppNbBytes;
import static features.MbtFeatures.getSampleRate;

/**
 * MbtDataConversion is responsible for managing conversion from raw EEG data acquired by the Bluetooth headset into readable EEG values
 *
 * @author Manon LETERME on 25/10/16
 * @version Sophie ZECRI 25/05/2018
 */
public class MbtDataConversion {

    private static final String TAG = MbtDataConversion.class.getName();

    private static int EEG_AMP_GAIN = 12;
    private static final short SHIFT_BLE = 8 + 4; //mandatory 8 to switch from 24 bits to 32 bits + variable part which fits fw config
    private static final short SHIFT_SPP = 16;
    private static final int CHECK_SIGN_BLE = (int) (0x80 << SHIFT_BLE);
    private static final int CHECK_SIGN_SPP = (int) 0x00800000;
    private static final int NEGATIVE_MASK_BLE = (int) (0xFFFFFFFF << (32 - SHIFT_BLE));
    private static final int NEGATIVE_MASK_SPP = (int) 0xFF000000;
    private static final int POSITIVE_MASK_BLE = (int) (~NEGATIVE_MASK_BLE);
    private static final int POSITIVE_MASK_SPP = (int) (~NEGATIVE_MASK_SPP);
    private static final float VOLTAGE_BLE = (float) ((0.286d * Math.pow(10, -6)) / EEG_AMP_GAIN);
    private static final float VOLTAGE_SPP = (float) ((0.536d * Math.pow(10, -6)) / 24);
    private static final float INCORRECT_VALUE_BLE = 0x0000FFFF;
    private static final float INCORRECT_VALUE_SPP = 0x00FFFFFF;

    /**
     * Converts the EEG raw data array into a user-readable EEG matrix
     * @param rawData the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param protocol the bluetooth protocol used to acquire data from the headset : Low Energy or Serial Port Profile.
     * @return the eeg data as float lists, with NaN values for missing data
     * @throws IllegalArgumentException if the number of components of the raw EEG data array is not modulo 250
     */
    public static ArrayList<ArrayList<Float>> convertRawDataToEEG(@NonNull byte[] rawData, BtProtocol protocol) {
        Log.i(TAG, "converting EEG raw data to user-readable EEG values");

        int nbChannels = MbtFeatures.getNbChannels();
        int nbBytesData = (protocol.equals(BLUETOOTH_LE)) ? getBleNbBytes() : getSppNbBytes(); //equals 2 for BLE and equals 3 for SPP
        int totalNbBytesDataAllChannels = nbBytesData * nbChannels; //nbChannels equals 2 for Meloming Headset and 8 for VPRO headset

        ArrayList<ArrayList<Float>> eegData = new ArrayList<>(nbChannels);
        for(int i = 0; i < nbChannels ; i++){
            eegData.add(new ArrayList<Float>()); //init EEG data matrix with empty lists of Float
        }

        if (rawData.length % getSampleRate() != 0)
            throw new IllegalArgumentException("Data size is invalid");

        int rawDataIndex = 0;
        int max = (protocol.equals(BLUETOOTH_LE)) ? nbChannels : (rawData.length / totalNbBytesDataAllChannels) / totalNbBytesDataAllChannels;

        for (int i = 0; i < rawData.length / totalNbBytesDataAllChannels; i++) {
            for (int j = 0; j < max; j++) {
                if (protocol.equals(BLUETOOTH_SPP) && (j == 0)) { //Here is status parsing, which can be directly updated to matrix
                    eegData.get(j).add((float) (rawData[rawDataIndex + 2] & 1));
                } else { //Here are data from sensors, whom need to be transformed to float
                    int temp = (rawData[rawDataIndex] & 0xFF) << ((protocol.equals(BLUETOOTH_LE)) ? SHIFT_BLE : SHIFT_SPP) | (rawData[rawDataIndex + 1] & 0xFF) << ((protocol.equals(BLUETOOTH_LE)) ? (SHIFT_BLE - 8) : (8 | (rawData[rawDataIndex + 2] & 0xFF)));
                    if (temp == ((protocol.equals(BLUETOOTH_LE)) ? INCORRECT_VALUE_BLE : INCORRECT_VALUE_SPP)) {  //if value is incorrect ...
                        eegData.get(j).add(Float.NaN); //... fill the EEG data matrix with a NaN value for graphs
                    } else { //if value is correct ...
                        temp = ((temp & ((protocol.equals(BLUETOOTH_LE)) ? CHECK_SIGN_BLE : CHECK_SIGN_SPP)) > 0) ?  // checking the sign
                                (temp | ((protocol.equals(BLUETOOTH_LE)) ? NEGATIVE_MASK_BLE : NEGATIVE_MASK_SPP )) : // value is negative
                                (temp & ((protocol.equals(BLUETOOTH_LE)) ? POSITIVE_MASK_BLE : POSITIVE_MASK_SPP)); // value is positive
                        eegData.get(j).add(temp * ((protocol.equals(BLUETOOTH_LE)) ? VOLTAGE_BLE : VOLTAGE_SPP)); //fill the EEG data matrix with the converted EEG data
                    }                }
                rawDataIndex += nbBytesData;
            }
        }
        return eegData;
    }

}
