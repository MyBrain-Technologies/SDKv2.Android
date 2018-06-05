package features;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.bluetooth.BtProtocol;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static features.ScannableDevices.MELOMIND;


/**
 * This class contains all the non customizable features that stay constant
 */

public final class MbtFeatures{

    private static String TAG = MbtFeatures.class.getName();

    public static int DEFAULT_SAMPLE_PER_PACKET = 4;

    public static final int DEVICE_NAME_MAX_LENGTH = 10;

    public final static int DEFAULT_SAMPLE_RATE = 250;
    public final static int DEFAULT_SAMPLE_PER_NOTIF = 4;

    public final static int DEFAULT_EEG_PACKET_LENGTH = 250;

    public final static long DEFAULT_BATTERY_READ_PERIOD = 20000;

    // MELOMIND & VPRO FEATURES
    public static final String MELOMIND_DEVICE_NAME_PREFIX = "melo_";
    public static final String VPRO_DEVICE_NAME_PREFIX = "vpro_";

    public static final String MELOMIND_DEVICE_NAME = "Melomind";
    public static final String VPRO_DEVICE_NAME = "VPro";
    public static final String ALL_DEVICE_NAME = "All";

    public final static int MELOMIND_NB_CHANNELS = 2;
    public final static int VPRO_NB_CHANNELS = 9;

    public final static int DEFAULT_BLE_NB_STATUS_BYTES = 0;
    public final static int DEFAULT_SPP_NB_STATUS_BYTES = 3;

    public final static int DEFAULT_BLE_NB_BYTES = 2;
    public final static int DEFAULT_SPP_NB_BYTES = DEFAULT_SPP_NB_STATUS_BYTES;

    public final static int DEFAULT_BLE_RAW_DATA_INDEX_SIZE = DEFAULT_BLE_NB_BYTES;
    public final static int DEFAULT_SPP_RAW_DATA_INDEX_SIZE = DEFAULT_SPP_NB_BYTES;

    public final static int DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = DEFAULT_BLE_NB_BYTES * MELOMIND_NB_CHANNELS;
    public final static int DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = DEFAULT_SPP_NB_BYTES * VPRO_NB_CHANNELS;

    public final static int DEFAULT_BLE_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET * DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES; //4 samples per packet 2 channels 2 bytes data
    public final static int DEFAULT_SPP_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET * DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES; //4 samples per packet 9 channels 3 bytes data

    public final static int DEFAULT_BLE_RAW_DATA_BUFFER_SIZE = DEFAULT_EEG_PACKET_LENGTH * DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
    public final static int DEFAULT_SPP_RAW_DATA_BUFFER_SIZE = DEFAULT_EEG_PACKET_LENGTH * DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_LOCATIONS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.P3, MbtAcquisitionLocations.P4));
    public final static ArrayList<MbtAcquisitionLocations> VPRO_LOCATIONS = new ArrayList<>(); //init values with server data

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_REFERENCES = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M1));
    public final static ArrayList<MbtAcquisitionLocations> VPRO_REFERENCES = new ArrayList<>(); //init values with server data

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_GROUNDS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M2));
    public final static ArrayList<MbtAcquisitionLocations> VPRO_GROUNDS = new ArrayList<>();//init values with server data

    public static int getNbChannels(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_NB_CHANNELS : VPRO_NB_CHANNELS);
    }

    public static String getDeviceName(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_DEVICE_NAME : VPRO_DEVICE_NAME);
    }

    public static BtProtocol getBluetoothProtocol(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? BLUETOOTH_LE : BLUETOOTH_SPP);
    }

    public static ArrayList<MbtAcquisitionLocations> getLocations(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_LOCATIONS : VPRO_LOCATIONS);
    }

    public static ArrayList<MbtAcquisitionLocations> getReferences(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_REFERENCES : VPRO_REFERENCES);
    }

    public static ArrayList<MbtAcquisitionLocations> getGrounds(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_GROUNDS : VPRO_GROUNDS);
    }

    public static int getSampleRate() {
        return DEFAULT_SAMPLE_RATE;
    }

    /**
     * Gets the number of bytes for a EEG raw data in case the Bluetooth protocol used is Bluetooth Low Energy
     * @return the number of bytes for a EEG raw data in case the Bluetooth protocol used is Bluetooth Low Energy
     */
    public static int getNbBytes() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? DEFAULT_BLE_NB_BYTES : DEFAULT_SPP_NB_BYTES);
    }

    /**
     * Gets the raw data packet size
     * @return the raw data packet size
     */
    public static int getRawDataPacketSize() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_RAW_DATA_PACKET_SIZE : DEFAULT_SPP_RAW_DATA_PACKET_SIZE;
    }

    /**
     * Gets the raw data buffer size
     * @return the raw data buffer size
     */
    public static int getRawDataBufferSize() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_RAW_DATA_BUFFER_SIZE : DEFAULT_SPP_RAW_DATA_BUFFER_SIZE;
    }

    /**
     * Gets the raw data index size
     * @return the raw data index size
     */
    public static int getRawDataIndexSize() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_RAW_DATA_INDEX_SIZE : DEFAULT_SPP_RAW_DATA_INDEX_SIZE;
    }

    /**
     * Gets the number of bytes of a EEG raw data per whole channels samples
     * @return the number of bytes of a EEG raw data per whole channels samples
     */
    public static int getRawDataBytesPerWholeChannelsSamples() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES : DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;
    }

    /**
     * Gets the number of bytes corresponding to one status EEG data
     * @return the number of bytes corresponding to one status EEG data
     */
    public static int getNbStatusBytes() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_NB_STATUS_BYTES : DEFAULT_SPP_NB_STATUS_BYTES;
    }


    /**
     * Gets the number of samples per packet
     * @return the number of samples per packet
     */
    public static int getSamplePerPacket() {
        return DEFAULT_SAMPLE_PER_PACKET;
    }

    /**
     * Sets a value to the number of samples per EEG data packet
     * @param samplePerPacket the number of samples per EEG data packet
     */
    public void setSamplePerPacket(int samplePerPacket) {
        DEFAULT_SAMPLE_PER_PACKET = samplePerPacket;
    }

}
