package features;

import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.bluetooth.BtProtocol;


/**
 * This class contains all non customizable features that are constant
 */

public final class MbtFeatures{

    private static String TAG = MbtFeatures.class.getName();

    public static final String MELOMIND_DEVICE_NAME_PREFIX = "melo_";

    private static final String MELOMIND_DEVICE_NAME = "Melomind";
    private static final String VPRO_DEVICE_NAME = "VPro";
    private static final String ALL_DEVICE_NAME = "All";

    public static final int DEVICE_NAME_MAX_LENGTH = 10;

    public final static int DEFAULT_SAMPLE_RATE = 250;
    public final static int DEFAULT_SAMPLE_PER_NOTIF = 4;

    public final static int DEFAULT_EEG_PACKET_LENGTH = 250;

    private final static int MELOMIND_NB_CHANNELS = 2;
    private final static int VPRO_NB_CHANNELS = 9;

    public final static int MELOMIND_STATUS_SIZE = 0;
    public final static int VPRO_STATUS_SIZE = 3;

    public final static int DEFAULT_MELOMIND_NB_BYTES = 2;
    public final static int DEFAULT_VPRO_NB_BYTES = 3;

    private final static long DEFAULT_BATTERY_READ_PERIOD = 20000;

    public final static ArrayList<MbtAcquisitionLocations> VPRO_LOCATIONS = new ArrayList<>(); //init values with server data
    public final static ArrayList<MbtAcquisitionLocations> VPRO_REFERENCES = new ArrayList<>(); //init values with server data
    public final static ArrayList<MbtAcquisitionLocations> VPRO_GROUNDS = new ArrayList<>();//init values with server data

    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_LOCATIONS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.P3, MbtAcquisitionLocations.P4));
    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_REFERENCES = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M1));
    public final static ArrayList<MbtAcquisitionLocations> MELOMIND_GROUNDS = new ArrayList<>(Arrays.asList(MbtAcquisitionLocations.M2));

    public static int getNbChannels(){
        int nbChannels=0;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                nbChannels=MELOMIND_NB_CHANNELS;
                break;
            case VPRO:
                nbChannels=VPRO_NB_CHANNELS;
                break;
            case ALL:
                nbChannels=VPRO_NB_CHANNELS+MELOMIND_NB_CHANNELS;
                break;
            default:
                break;
        }
        return nbChannels;
    }

    public static String getDeviceName(){
        String deviceName=null;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                deviceName=MELOMIND_DEVICE_NAME;
                break;
            case VPRO:
                deviceName=VPRO_DEVICE_NAME;
                break;
            case ALL:
                deviceName=ALL_DEVICE_NAME;
                break;
            default:
                break;
        }
        return  deviceName;
    }



    public static int getStatusSize(){
        int statusSize=0;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                statusSize=MELOMIND_STATUS_SIZE;
                break;
            case VPRO:
                statusSize=VPRO_STATUS_SIZE;
                break;
            case ALL:
                statusSize=MELOMIND_STATUS_SIZE;
                break;
            default:
                break;
        }
        return statusSize;
    }

    public static BtProtocol getBluetoothProtocol(){
        BtProtocol protocol = null;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                protocol=BtProtocol.BLUETOOTH_LE;
                break;
            case VPRO:
                protocol=BtProtocol.BLUETOOTH_SPP;
                break;
            case ALL:
                protocol=BtProtocol.BLUETOOTH_SPP;
                break;
            default:
        }
        return protocol;
    }

    public static ArrayList<MbtAcquisitionLocations> getLocations(){
        ArrayList<MbtAcquisitionLocations> locations = null;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                locations=MELOMIND_LOCATIONS;
                break;
            case VPRO:
                locations=VPRO_LOCATIONS;
                break;
            case ALL:

                break;
            default:
                break;
        }
        return locations;
    }

    public static ArrayList<MbtAcquisitionLocations> getReferences(){
        ArrayList<MbtAcquisitionLocations> references = null;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                references=MELOMIND_REFERENCES;
                break;
            case VPRO:
                references=VPRO_REFERENCES;
                break;
            case ALL:

                break;
            default:
                break;
        }
        return references;
    }

    public static ArrayList<MbtAcquisitionLocations> getGrounds(){
        ArrayList<MbtAcquisitionLocations> grounds = null;
        switch(MbtConfig.getScannableDevices()){
            case MELOMIND:
                grounds=MELOMIND_GROUNDS;
                break;
            case VPRO:
                grounds=VPRO_GROUNDS;
                break;
            case ALL:

                break;
            default:
                break;
        }
        return grounds;
    }

    public static int getSampleRate() {
        return DEFAULT_SAMPLE_RATE;
    }

    public static int getNbBytes() {
        int nbBytes = 0;
        switch (MbtConfig.getScannableDevices()) {
            case MELOMIND:
                nbBytes=DEFAULT_MELOMIND_NB_BYTES;
                break;
            case VPRO:
                nbBytes=DEFAULT_VPRO_NB_BYTES;
                break;
            case ALL:
                nbBytes=DEFAULT_VPRO_NB_BYTES;
                break;
        }
        return nbBytes;
    }
}
