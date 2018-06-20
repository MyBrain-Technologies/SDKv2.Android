package core.eeg.acquisition;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;

import core.bluetooth.BtProtocol;
import core.eeg.storage.RawEEGSample;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static features.MbtFeatures.getNbChannels;

/**
 * MbtDataConversion is responsible for managing conversion from raw EEG data acquired by the Bluetooth headset into readable EEG values
 *
 * @author Manon LETERME on 25/10/16
 * @version Sophie ZECRI 25/05/2018
 */
public class MbtDataConversion {
    private static final String TAG = MbtDataConversion.class.getName();

    private static int EEG_AMP_GAIN = 8;
    private static final short SHIFT_BLE = 8 + 4; //mandatory 8 to switch from 24 bits to 32 bits + variable part which fits fw config
    private static final short SHIFT_DC_OFFSET = 16;
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
     * @param rawEEGdataList the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     * @param protocol the bluetooth protocol used to acquire data from the headset : Low Energy or Serial Port Profile.
     * @return the eeg data as float lists, with NaN values for missing data
     * @throws IllegalArgumentException if the number of components of the raw EEG data array is not modulo 250
     */
    @NonNull
    public static ArrayList<ArrayList<Float>> convertRawDataToEEG(@NonNull ArrayList<RawEEGSample> rawEEGdataList, @NonNull BtProtocol protocol) {
        /*if ((rawEEGdataList.size()*getEEGByteSize()) % getSampleRate() != 0)
            throw new IllegalArgumentException("EEG Data size is invalid "+ rawEEGdataList.size());*/

        ArrayList<ArrayList<Float>> eegData = new ArrayList<>();

        ArrayList<Float> consolidatedEEGSample;

        for (RawEEGSample singleRawEEGdata : rawEEGdataList){ // for each channel of the headset
            consolidatedEEGSample = new ArrayList<Float>();

            if(singleRawEEGdata.getBytesEEG() == null){

                consolidatedEEGSample.add(Float.NaN); //... fill the EEG data matrix with a NaN value for
            }else{
                for (byte[] bytes : singleRawEEGdata.getBytesEEG()) {
                    int temp = 0x0000000;
                    for (int i = 0; i < bytes.length; i++){
                        temp |= (bytes[i] & 0xFF) << ((protocol.equals(BLUETOOTH_LE)) ? (SHIFT_BLE - i*8) : (16 - i*8));
                    }
                    temp = ((temp & ((protocol.equals(BLUETOOTH_LE)) ? CHECK_SIGN_BLE : CHECK_SIGN_SPP)) > 0) ? (temp | ((protocol.equals(BLUETOOTH_LE)) ? NEGATIVE_MASK_BLE : NEGATIVE_MASK_SPP )) : (temp & ((protocol.equals(BLUETOOTH_LE)) ? POSITIVE_MASK_BLE : POSITIVE_MASK_SPP));
                    consolidatedEEGSample.add(temp * ((protocol.equals(BLUETOOTH_LE)) ? VOLTAGE_BLE : VOLTAGE_SPP)); //fill the EEG data matrix with the converted EEG data

                    //Here are data from sensors, whom need to be transformed to float
                }
            }
            eegData.add(consolidatedEEGSample);
        }
        return eegData;
    }

    public static float convertDCOffsetToEEG(byte[] offset){
        int digit = 0x00000000;
        digit = ((offset[0] & 0xFF) << (SHIFT_DC_OFFSET)) | ((offset[1] & 0xFF) << (SHIFT_DC_OFFSET-8));

        if ((digit & CHECK_SIGN_BLE) > 0) { // value is negative
            digit = (int) (digit | NEGATIVE_MASK_BLE );
        }
        else{
            // value is positive
            digit = (int) (digit & POSITIVE_MASK_BLE);
        }

        return digit * VOLTAGE_BLE;
    }


}
