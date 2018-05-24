package core.eeg.storage;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import core.bluetooth.BtProtocol;


/**
 * Created by manon on 25/10/16.
 */
public class MbtDataConversion {

    private static final String TAG = MbtDataConversion.class.getName();

    private static final short SHIFT_MELOMIND = 8+4; //mandatory 8 to switch from 24 bits to 32 bits + variable part which fits fw config
    private static final int CHECK_SIGN_MELOMIND = (int) (0x80 << SHIFT_MELOMIND);
    private static final int NEGATIVE_MASK_MELOMIND = (int) (0xFFFFFFFF << (32-SHIFT_MELOMIND));
    private static final int POSITIVE_MASK_MELOMIND = (int) (~NEGATIVE_MASK_MELOMIND);

    private static int EEG_AMP_GAIN = 12;

    /**
     * Creates the eeg data output from a simple raw data array and call convertDataSPP or convertDataBLE to convert raw EEG
     * @param rawData the raw data coming from BLE or SPP
     * @return the eeg data as float lists, with NaN values for missing data
     */
    public static ArrayList<ArrayList<Float>> convertRawDataToEEG(@NonNull byte[] rawData, BtProtocol protocol, int nbChannels) {
        Log.e(TAG, "convertRawDatatoEEG data:" + Arrays.toString(rawData)); //todo remove

        int nbBytesData;
        int max = 0;
        nbBytesData = (protocol.equals(BtProtocol.BLUETOOTH_LE))? 2 : 3; //equals 2 for BLE and equals 3 for SPP

        int DIVIDER = nbBytesData*nbChannels;
        switch(protocol){
            case BLUETOOTH_LE:
                max = nbChannels;
                break;
            case BLUETOOTH_SPP:
                try{
                    max = (rawData.length/DIVIDER)/DIVIDER;
                } catch (ArithmeticException ae) {
                    Log.e(TAG,"ArithmeticException occured!");
                }
                break;
        }

        ArrayList<ArrayList<Float>> eegData = new ArrayList<>(nbChannels);
        for (int i = 0; i < nbChannels; i++) {
            eegData.add(new ArrayList<Float>());
        }
        if(rawData.length % 250 != 0){
            throw new IllegalArgumentException("Data size is invalid");
        }
        int rawDataIndex = 0;

        for(int i = 0; i < rawData.length/DIVIDER; i++) {
            for (int eegDataIndex = 0; eegDataIndex < max; eegDataIndex++) {
                eegData = (protocol.equals(BtProtocol.BLUETOOTH_LE)? convertRawDataBLE(rawData, eegData, rawDataIndex, eegDataIndex) : convertRawDataSPP(rawData, eegData, rawDataIndex, eegDataIndex) );
                rawDataIndex += nbBytesData;
            }
        }
        return eegData;
    }

    /**
     * Method called by convertRawDataToEEG to convert a single raw data if SPP protocol is used
     * Fill the eeg data output by adding the raw data that has been converted
     * 0xFFFFFF values are computed a NaN values
     * @param rawData the raw data coming from SPP
     * @return the eeg data as float lists, with NaN values for missing data
     */
    private static ArrayList<ArrayList<Float>> convertRawDataSPP(byte[] rawData, ArrayList<ArrayList<Float>> eegData, int currentRawDataIndex, int currentEEGDataIndex) {
        Log.e(TAG, "converting SPP data:" + Arrays.toString(rawData)); //todo remove

        final float voltage = (float) (0.536d * Math.pow(10, -6)) / 24;

        if(currentEEGDataIndex == 0){
            //Here is status parsing, which can be directly updated to matrix
            Float temp = (float) (rawData[currentRawDataIndex + 2] & 1);
            eegData.get(currentEEGDataIndex).add(temp);
        }else{
            //Here are data from sensors, whom need to be transformed to float
            int temp = (rawData[currentRawDataIndex] & 0xFF) << 16 | (rawData[currentRawDataIndex + 1] & 0xFF) << 8 | (rawData[currentRawDataIndex + 2] & 0xFF);
            if(temp == 0x00FFFFFF){
                //Value is incorrect it's transformed to NaN for graphs
                eegData.get(currentEEGDataIndex).add(Float.NaN);
            }else{
                if ((temp & 0x00800000) > 0) { // value is negative
                    temp = temp | 0xFF000000;
                }
                else{
                    // value is positive
                    temp = (temp & 0x00FFFFFF);
                }
                eegData.get(currentEEGDataIndex).add(temp * voltage);
            }
        }
        
        return eegData;
    }
    
    /**
     * Method called by convertRawDataToEEG to convert a single raw data if BLE protocol is used
     * Fill the eeg data output by adding the raw data that has been converted
     * 0xFFFF values are computed a NaN values
     * @param rawData is the raw data coming from BLE
     * @param eegData is the converted data
     * @param currentRawDataIndex is the index of current the raw data
     * @return the eeg data as float lists, with NaN values for missing data
     */
    private static ArrayList<ArrayList<Float>> convertRawDataBLE(byte[] rawData, ArrayList<ArrayList<Float>> eegData, int currentRawDataIndex, int currentEEGDataIndex){
        Log.e(TAG, "converting BLE data:" + Arrays.toString(rawData)); //todo remove

        final float voltageADS1298 = (float) (0.286d * Math.pow(10, -6)) / EEG_AMP_GAIN; //12; // for ADS 1298

        //Here are data from sensors, whom need to be transformed to float
        int temp;
        temp = ((rawData[currentRawDataIndex] & 0xFF) << (SHIFT_MELOMIND)) | ((rawData[currentRawDataIndex+1] & 0xFF) << (SHIFT_MELOMIND-8));
        if(temp == 0x0000FFFF){
            //Value is incorrect it's transformed to NaN for graphs
            eegData.get(currentEEGDataIndex).add(Float.NaN);
        }else{
            if ((temp & CHECK_SIGN_MELOMIND) > 0) { // value is negative
                temp = (temp | NEGATIVE_MASK_MELOMIND );
            }
            else{
                // value is positive
                temp = (temp & POSITIVE_MASK_MELOMIND);
            }
        }

        eegData.get(currentEEGDataIndex).add(temp * voltageADS1298);
        return eegData;
    }
}
