package features;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.net.PortUnreachableException;
import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.bluetooth.BtProtocol;
import utils.FirmwareUtils;

import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static core.bluetooth.BtProtocol.BLUETOOTH_SPP;
import static features.ScannableDevices.MELOMIND;


/**
 * This class contains all the non customizable features that stay constant
 */

@Keep
public final class MbtFeatures{

    public static final int MIN_SCAN_DURATION = 10000;

    private static String TAG = MbtFeatures.class.getName();


    public static final int DEVICE_NAME_MAX_LENGTH = 5+10;

    public final static int DEFAULT_SAMPLE_RATE = 250;
    public final static int DEFAULT_SAMPLE_PER_PACKET = 4;
    public static int DEFAULT_SAMPLE_PER_NOTIF = DEFAULT_SAMPLE_PER_PACKET;

    public final static int DEFAULT_EEG_PACKET_LENGTH = 250;

    public final static int DEFAULT_CLIENT_PACKET_SIZE = 250;
    public final static int DEFAULT_CLIENT_NOTIFICATION_PERIOD = 1000;
    public static final int MIN_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS = 200;
    public static final int MAX_CLIENT_NOTIFICATION_PERIOD_IN_MILLIS = 60000;

    public static final int MIN_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS = 1000;
    public static final int MAX_CLIENT_NOTIFICATION_PERIOD_WITH_QUALITIES_IN_MILLIS = 1000;

    public final static long DEFAULT_BATTERY_READ_PERIOD = 20000;

    public final static int DEFAULT_MAX_SCAN_DURATION_IN_MILLIS = 30000;
    public final static int DEFAULT_MAX_CONNECTION_DURATION_IN_MILLIS = 40000;

    // MELOMIND & VPRO FEATURES
    public static final String MELOMIND_DEVICE_NAME_PREFIX = "melo_";
    public static final String A2DP_DEVICE_NAME_PREFIX = "audio_";
    public static final String A2DP_DEVICE_NAME_PREFIX_LEGACY = "melo_";
    public static final String VPRO_DEVICE_NAME_PREFIX = "VPro";

    public static final String MELOMIND_DEVICE_NAME = "melomind";
    public static final String VPRO_DEVICE_NAME = "vpro";
    public static final String ALL_DEVICE_NAME = "All";

    public final static int MELOMIND_NB_CHANNELS = 2;
    public final static int VPRO_NB_CHANNELS = 9;

    public final static int DEFAULT_BLE_NB_STATUS_BYTES = 0;
    public final static int DEFAULT_SPP_NB_STATUS_BYTES = 3;
    private static int nbStatusBytes = -1;

    public static final int DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE = 40;

    public final static int DEFAULT_BLE_NB_BYTES = 2;
    public final static int DEFAULT_SPP_NB_BYTES = DEFAULT_SPP_NB_STATUS_BYTES;

    public final static int DEFAULT_BLE_RAW_DATA_INDEX_SIZE = DEFAULT_BLE_NB_BYTES;
    public final static int DEFAULT_SPP_RAW_DATA_INDEX_SIZE = DEFAULT_SPP_NB_BYTES;

    public final static int DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = DEFAULT_BLE_NB_BYTES * MELOMIND_NB_CHANNELS;
    public final static int DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES = DEFAULT_SPP_NB_BYTES * VPRO_NB_CHANNELS;

    public final static int DEFAULT_BLE_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET * DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES + DEFAULT_BLE_NB_STATUS_BYTES;; //1 packet contains 4 samples per packet 2 channels 2 bytes data + the status bytes
    public final static int DEFAULT_SPP_RAW_DATA_PACKET_SIZE = DEFAULT_SAMPLE_PER_PACKET * DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES; //4 samples per packet 9 channels 3 bytes data
    private static int packetSize = -1;

    public final static int DEFAULT_BLE_RAW_DATA_BUFFER_SIZE = DEFAULT_EEG_PACKET_LENGTH * DEFAULT_BLE_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES;  //250*4 = 2000
    public final static int DEFAULT_SPP_RAW_DATA_BUFFER_SIZE = DEFAULT_EEG_PACKET_LENGTH * DEFAULT_SPP_RAW_DATA_BYTES_PER_WHOLE_CHANNELS_SAMPLES; //250*27 = 6750

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_LOCATIONS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.P3, MbtAcquisitionLocations.P4));
    public final static ArrayList<MbtAcquisitionLocations> VPRO_LOCATIONS = new ArrayList<>(); //init values with server data

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_REFERENCES = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M1));
    public final static ArrayList<MbtAcquisitionLocations> VPRO_REFERENCES = new ArrayList<>(); //init values with server data

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_GROUNDS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M2));
    public final static ArrayList<MbtAcquisitionLocations> VPRO_GROUNDS = new ArrayList<>();//init values with server data

    public final static String INTENT_CONNECTION_STATE_CHANGED = "connectionStateChanged";

    public static final int DEFAULT_NUMBER_OF_DATA_TO_DISPLAY = 500;
    public static int getNbChannels(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_NB_CHANNELS : VPRO_NB_CHANNELS);
    }

    @NonNull
    public static String getDeviceName(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_DEVICE_NAME : VPRO_DEVICE_NAME);
    }

    @NonNull
    public static BtProtocol getBluetoothProtocol(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? BLUETOOTH_LE : BLUETOOTH_SPP);
    }

    @NonNull
    public static ArrayList<MbtAcquisitionLocations> getLocations(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_LOCATIONS : VPRO_LOCATIONS);
    }

    @NonNull
    public static ArrayList<MbtAcquisitionLocations> getReferences(){
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? MELOMIND_REFERENCES : VPRO_REFERENCES);
    }

    @NonNull
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
    public static int getEEGByteSize() {
        return (MbtConfig.getScannableDevices().equals(MELOMIND) ? DEFAULT_BLE_NB_BYTES : DEFAULT_SPP_NB_BYTES);
    }

    /**
     * Gets the raw data packet size
     * @return the raw data packet size
     */
    public static int getRawDataPacketSize() {
        if(packetSize == -1)
            packetSize = (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_RAW_DATA_PACKET_SIZE : DEFAULT_SPP_RAW_DATA_PACKET_SIZE;
        return packetSize;
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
     * Gets the number of bytes corresponding to one status data
     * @return the number of bytes corresponding to one status data
     */
    public static int getNbStatusBytes() {
        if(nbStatusBytes == -1)
            nbStatusBytes = (MbtConfig.getScannableDevices().equals(MELOMIND))? DEFAULT_BLE_NB_STATUS_BYTES : DEFAULT_SPP_NB_STATUS_BYTES;
        return nbStatusBytes;
    }

    /**
     * Gets the number of samples per packet
     * @return the number of samples per packet
     */
    public static int getSamplePerPacket() {
        return DEFAULT_SAMPLE_PER_PACKET;
    }

    /**
     * Sets a value to the number of samples per EEG data packet notification
     * @param samplePerNotification the number of samples per EEG data packet notification
     */
    public static void setSamplePerNotification(int samplePerNotification) {
        DEFAULT_SAMPLE_PER_NOTIF = samplePerNotification;
    }

    /**
     * Gets the number of samples per EEG data packet notification
     * @return the number of samples per EEG data packet notification
     */
    public static int getSamplePerNotification() {
        return DEFAULT_SAMPLE_PER_NOTIF;
    }
}
