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
public class MbtDataConversion2 {
    private static final short SHIFT_BLE = 8 + 4; //mandatory 8 to switch from 24 bits to 32 bits + variable part which fits fw config
    private static final short SHIFT_DC_OFFSET = 16;
    private static final int CHECK_SIGN_BLE = (int) (0x80 << SHIFT_BLE);
    private static final int CHECK_SIGN_SPP = (int) 0x00800000;
    private static final int NEGATIVE_MASK_BLE = (int) (0xFFFFFFFF << (32 - SHIFT_BLE));
    private static final int NEGATIVE_MASK_SPP = (int) 0xFF000000;
    private static final int POSITIVE_MASK_BLE = (int) (~NEGATIVE_MASK_BLE);
    private static final int POSITIVE_MASK_SPP = (int) (~NEGATIVE_MASK_SPP);
    private static final float INCORRECT_VALUE_BLE = 0x0000FFFF;
    private static final float INCORRECT_VALUE_SPP = 0x00FFFFFF;

    /**
     * the bluetooth protocol used to acquire data from the headset : Low Energy or Serial Port Profile.
     */
    private EnumBluetoothProtocol protocol;
    private int eegAmpGain = 8;
    private int nbChannels;
    private float bleVoltage = (float) ((0.286d * Math.pow(10, -6)) / eegAmpGain);
    private float sppVoltage = (float) ((0.536d * Math.pow(10, -6)) / 24); //todo VPRO VOLTAGE

    private MbtDataConversion2(EnumBluetoothProtocol protocol, int nbChannels, byte gain) {

        Timber.d("Dev_debug MbtDataConversion2 constructor protocol:%s nbChannels:%s gain:%s", protocol,nbChannels,gain);
        this.protocol = protocol;
        this.nbChannels = nbChannels;
        if (gain != 0) {
            eegAmpGain = AmpGainConfig2.getGainFromByteValue(gain);
            Timber.d("Dev_debug MbtDataConversion2  eegAmpGain:%s",eegAmpGain);
            bleVoltage = (float) ((0.286d * Math.pow(10, -6)) / eegAmpGain);

            Timber.d("Dev_debug MbtDataConversion2  bleVoltage:%s",bleVoltage);
            sppVoltage = (float) ((0.536d * Math.pow(10, -6)) / 24); //todo VPRO VOLTAGE
            Timber.d("Dev_debug MbtDataConversion2  sppVoltage:%s",sppVoltage);
        }

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

//        Timber.d("Dev_debug MbtDataConversion2 convertRawDataToEEG rawEEGdataList :%s",rawEEGdataList);
        ArrayList<Float> consolidatedEEGSample;

        for (RawEEGSample2 singleRawEEGdata : rawEEGdataList) { // for each acquisition of the headset
            consolidatedEEGSample = new ArrayList<>();
//            Timber.d("Dev_debug MbtDataConversion2 convertRawDataToEEG singleRawEEGdata :%s",singleRawEEGdata);
            if (singleRawEEGdata.getEegData() == null) {

                for (int nbChannel = 0; nbChannel < nbChannels; nbChannel++) {
                    consolidatedEEGSample.add(Float.NaN); //... fill the EEG data matrix with a NaN value for
                }
            } else {
                for (byte[] bytes : singleRawEEGdata.getEegData()) {
                    int temp = 0x0000000;
                    for (int i = 0; i < bytes.length; i++) {
                        temp |= (bytes[i] & 0xFF) << ((isBle()) ? (SHIFT_BLE - i * 8) : (16 - i * 8));
                    }
                    temp = ((temp & ((isBle()) ? CHECK_SIGN_BLE : CHECK_SIGN_SPP)) > 0) ? (temp | ((isBle()) ? NEGATIVE_MASK_BLE : NEGATIVE_MASK_SPP)) : (temp & ((isBle()) ? POSITIVE_MASK_BLE : POSITIVE_MASK_SPP));

//                    Timber.d("Dev_debug MbtDataConversion2 convertRawDataToEEG in singleRawEEGdata temp(before voltage):%s",temp);
                    float dataTobeAdded = temp * ((isBle()) ? bleVoltage : sppVoltage);
                    consolidatedEEGSample.add(dataTobeAdded); //fill the EEG data matrix with the converted EEG data
//                    Timber.d("Dev_debug MbtDataConversion2 convertRawDataToEEG in singleRawEEGdata temp(after voltage):%s",dataTobeAdded);
                    //Here are data from sensors, whom need to be transformed to float
                }
            }
            eegData.add(consolidatedEEGSample);
        }


        return eegData;
    }

    public float convertRawDataToDcOffset(byte[] offset) {
        if (offset == null || offset.length < 2)
            return -1;

        int digit = 0x00000000;
        digit = ((offset[0] & 0xFF) << (SHIFT_DC_OFFSET)) | ((offset[1] & 0xFF) << (SHIFT_DC_OFFSET - 8));

        if ((digit & CHECK_SIGN_BLE) > 0) {
            digit = (int) (digit | NEGATIVE_MASK_BLE);// value is negative
        } else {
            digit = (int) (digit & POSITIVE_MASK_BLE);// value is positive
        }

        return digit * bleVoltage;
    }

    private boolean isBle() {
        return protocol.equals(EnumBluetoothProtocol.BLE);
    }

    public static class Builder {
        private EnumBluetoothProtocol protocol = EnumBluetoothProtocol.BLE;
        private byte gain = 0;
        private int nbChannels = 4;

        public Builder setProtocol(@NonNull EnumBluetoothProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setChannelNumber(int size) {
            this.nbChannels = size;
            return this;
        }

        public Builder setGain(byte gain) {
            boolean isVerifiedWithTeam = false;
            if (isVerifiedWithTeam) {
                this.gain = gain;
                return this;
            } else {
                throw new UnsupportedOperationException("https://mybrain.atlassian.net/browse/MA-1480");
            }
        }

        @NonNull
        public MbtDataConversion2 build() {
            return new MbtDataConversion2(protocol, nbChannels, gain);
        }
    }

    @NonNull
    static public MbtDataConversion2 generateInstance(EnumMBTDevice deviceType) {
        switch (deviceType) {
            case Q_PLUS:
            case HYPERION: {
                return new MbtDataConversion2(EnumBluetoothProtocol.BLE, 4, (byte) 0);
            }
            case MELOMIND: {
                return new MbtDataConversion2(EnumBluetoothProtocol.BLE, 2, (byte) 0);
            }
            default: {
                throw new RuntimeException("Illegal device type : deviceType = " + deviceType.toString());
            }
        }
    }
}
