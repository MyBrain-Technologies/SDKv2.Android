package core.device.oad;

import android.content.Context;

import java.io.FileNotFoundException;

import core.device.MbtDeviceManager;
import core.device.model.MbtDevice;
import engine.clientevents.OADStateListener;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
import utils.LogUtils;
import utils.OADExtractionUtils;


public final class OADManager {

    private final static String TAG = OADManager.class.getName();

    private final static String BINARY_HOOK = "mm-ota-";
    private final static String BINARY_FORMAT = ".bin";
    private final static String FWVERSION_REGEX = "_";

    public final static String CURRENT_FW_VERSION = BINARY_HOOK + BuildConfig.FIRMWARE_VERSION + BINARY_FORMAT;

    public static final int NB_PACKETS_TO_SEND = 14223;

    static final int OAD_PACKET_SIZE = 18;
    static final int OAD_INDEX_SIZE = 2;
    static final int OAD_BUFFER_SIZE = OAD_INDEX_SIZE + OAD_PACKET_SIZE;

    private static final int FIRMWARE_VERSION_LENGTH = 3;
    private static final String FIRMWARE_VERSION_SPLITTER = "\\.";

    private final Context context;
    private final MbtDeviceManager deviceManager;

    private OADState currentState;
    private OADStateListener stateListener;
    PacketCounter packetEngine;

    private OADContext oadContext;

    public OADManager(Context context, MbtDeviceManager deviceManager) {
        this.context = context;
        this.deviceManager = deviceManager;
        this.oadContext = new OADContext();

    }

    public void startOADupdate(){
        init(CURRENT_FW_VERSION);

    }

    void notifyOADStateChanged(OADState state) {
        currentState = state;
    }

    void abort(String reason) {

        notifyOADStateChanged(OADState.ABORTED);
    }

    /**
     * Initialize the OAD update process
     * Extract from the binary file that holds the new firmware:
     * - the firmware version
     * - the number of OAD packets (chunks of binary file)
     * @param oadFilePath is the path of the binary file that holds the new firmware
     * @return true if the
     */
    public boolean init(String oadFilePath){
        this.packetEngine = new PacketCounter();
        oadContext.setFilePath(oadFilePath);
        try {
            byte[] content = OADExtractionUtils.extractFileContent(context.getAssets(), oadFilePath);
            String firmwareVersion = OADExtractionUtils.extractFirmwareVersion(content);
            oadContext.setNbBytesToSend(content.length);
            oadContext.setFirmwareVersion(firmwareVersion);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true; //todo if valid/invalid true /false
    }

    public boolean requestFirmwareValidation(int nbPacketToSend, String firmwareVersion){

        return true;//todo
    }

    public boolean transferOADFile(){

        return true;//todo
    }

    public boolean reboot(){

        return true;//todo
    }

    public boolean reconnect(){

        return true;//todo
    }

    public void verifyFirmwareVersion(){
        isFirmwareVersionUpToDate(deviceManager.getmCurrentConnectedDevice().getFirmwareVersion());
    }

    /**
     * Return true if the firmware version is equal to the last released version.
     */
    public boolean isFirmwareVersionUpToDate(String firmwareVersion){
        boolean isUpToDate = true;
        String[] deviceFirmwareVersion = firmwareVersion.split(FIRMWARE_VERSION_SPLITTER);
        String[] OADFileFirmwareVersion = getLastFirmwareVersion();

        if(deviceFirmwareVersion.length < FIRMWARE_VERSION_LENGTH || firmwareVersion.equals(MbtDevice.DEFAULT_FW_VERSION)){
            LogUtils.e(TAG, "Firmware version is invalid");
            return true;
        }

        //Compare it to latest binary file either from server or locally
        if(OADFileFirmwareVersion == null){
            LogUtils.e(TAG, "No binary file found");
            return true;
        }
        if(OADFileFirmwareVersion.length > FIRMWARE_VERSION_LENGTH){ //trimming initial array
            String[] firmwareVersionToCopy = new String[FIRMWARE_VERSION_LENGTH];
            System.arraycopy(OADFileFirmwareVersion, 0, firmwareVersionToCopy, 0, firmwareVersionToCopy.length);
            OADFileFirmwareVersion = firmwareVersionToCopy.clone();
        }

        for (String character : OADFileFirmwareVersion) {
            if(character == null){
                LogUtils.e(TAG, "error when parsing fw version");
                return true;
            }
        }

        for(int i = 0; i < deviceFirmwareVersion.length; i++){

            if(Integer.parseInt(deviceFirmwareVersion[i]) > Integer.parseInt(OADFileFirmwareVersion[i])){ //device value is stricly superior to bin value so it's even more recent
                break;
            }else if(Integer.parseInt(deviceFirmwareVersion[i])< Integer.parseInt(OADFileFirmwareVersion[i])){ //device value is inferior to bin. update is necessary
                isUpToDate = false;
                LogUtils.i(TAG, "update is necessary");
                break;
            }
        }

        return isUpToDate;
    }

    private String[] getLastFirmwareVersion() {
        return null; //todo
    }

    public OADState getCurrentState() {
        return currentState;
    }

    public void setStateListener(OADStateListener stateListener) {
        this.stateListener = stateListener;
    }
}
