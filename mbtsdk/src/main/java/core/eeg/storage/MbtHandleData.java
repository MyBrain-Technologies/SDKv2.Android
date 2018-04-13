package core.eeg.storage;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutCompat;

import java.util.ArrayList;

import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;


/**
 * Created by manon on 25/10/16.
 */
public class MbtHandleData {

    private static final short SHIFT_MELOMIND = 8+4; //mandatory 8 to switch from 24 bits to 32 bits + variable part which fits fw config
    private static final int CHECK_SIGN_MELOMIND = (int) (0x80 << SHIFT_MELOMIND);
    private static final int NEGATIVE_MASK_MELOMIND = (int) (0xFFFFFFFF << (32-SHIFT_MELOMIND));
    private static final int POSITIVE_MASK_MELOMIND = (int) (~NEGATIVE_MASK_MELOMIND);

    public static int eegAmpGain = 12;
    private MbtEEGManager eegManager;

    private static boolean checkEegDataSize(final ArrayList<ArrayList<Float>> eegData, @NonNull final int limit) {
        for (ArrayList<Float>  channel : eegData) {
            if (channel.size() < limit){
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the eeg data output from a simple raw data array
     * 0xFFFFFF values are computed a NaN values
     * @param dataArray the raw data coming from BLE or SPP
     * @return the eeg data as float lists, with NaN values for missing data
     */

    public static ArrayList<ArrayList<Float>> convertRawDataToEEG(@NonNull byte[] dataArray, BtProtocol protocol, int nbChannels) {
        int nbBytesData = 0;
        int max = 0;

        switch(protocol){
            case BLUETOOTH_LE:
                nbBytesData = 2;
                break;
            case BLUETOOTH_SPP:
                nbBytesData = 3;
                break;
        }
        int DIVIDER = nbBytesData*nbChannels;
        switch(protocol){
            case BLUETOOTH_LE:
                max = nbChannels;
                break;
            case BLUETOOTH_SPP:
                max = (dataArray.length/DIVIDER)/DIVIDER;
                break;
        }

        ArrayList<ArrayList<Float>> eegData = new ArrayList<>(nbChannels);
        for (int i = 0; i < nbChannels; i++) {
            eegData.add(new ArrayList<Float>());
        }
        if(dataArray.length % 250 != 0){
            throw new IllegalArgumentException("Data size is invalid");
        }
        int index = 0;

        for(int i = 0; i < dataArray.length/DIVIDER; i++) {
            for (int j = 0; j < max; j++) {
                switch(protocol){
                    case BLUETOOTH_LE:
                        index = handleBLE(dataArray, eegData, index, j);
                        break;
                    case BLUETOOTH_SPP:
                        index = handleSPP(dataArray, eegData, index, j);
                        break;
                }
            }
        }
        return eegData;
    }

    public static int handleSPP(byte[] dataArray, ArrayList<ArrayList<Float>> eegData, int index, int j ) {
        if(j == 0){
            //Here is status parsing, which can be directly updated to matrix
            Float temp = (float) (dataArray[index + 2] & 1);
            eegData.get(j).add(temp);
        }else{
            //Here are data from sensors, whom need to be transformed to float
            int temp = (dataArray[index] & 0xFF) << 16 | (dataArray[index + 1] & 0xFF) << 8 | (dataArray[index + 2] & 0xFF);
            if(temp == 0x00FFFFFF){
                //Value is incorrect it's transformed to NaN for graphs
                eegData.get(j).add(Float.NaN);
            }else{
                if ((temp & 0x00800000) > 0) { // value is negative
                    temp = (int) (temp | 0xFF000000 );
                }
                else{
                    // value is positive
                    temp = (int) (temp & 0x00FFFFFF);
                }
                eegData.get(j).add(temp * (float) (0.536d * Math.pow(10, -6)) / 24);
            }
        }
        index += 3;
        return index;
    }

    public static int handleBLE(byte[] dataArray, ArrayList<ArrayList<Float>> eegData, int index, int j){
        final float voltageADS1298 = (float) (0.286d * Math.pow(10, -6)) / eegAmpGain; //12; // for ADS 1298

        //Here are data from sensors, whom need to be transformed to float
        int temp = 0x00000000;
        temp = ((dataArray[index] & 0xFF) << (SHIFT_MELOMIND)) | ((dataArray[index+1] & 0xFF) << (SHIFT_MELOMIND-8));
        if(temp == 0x0000FFFF){
            //Value is incorrect it's transformed to NaN for graphs
            eegData.get(j).add(Float.NaN);
        }else{
            if ((temp & CHECK_SIGN_MELOMIND) > 0) { // value is negative
                temp = (int) (temp | NEGATIVE_MASK_MELOMIND );
            }
            else{
                // value is positive
                temp = (int) (temp & POSITIVE_MASK_MELOMIND);
            }
        }

        eegData.get(j).add(temp * voltageADS1298);
        index += 2;
        return index;
    }

}
