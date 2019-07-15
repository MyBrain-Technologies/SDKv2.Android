package core.device.oad;

import android.content.Context;
import android.support.annotation.NonNull;

import core.device.model.MbtDevice;
import engine.clientevents.OADStateListener;
import utils.LogUtils;


public final class OADManager {

    private final static String TAG = OADManager.class.getName();

    static final int BUFFER_LENGTH = 14223;
    static final int CHUNK_NB_BYTES = 14223;
    static final int FILE_LENGTH_NB_BYTES = 4;
    static final int FIRMWARE_VERSION_NB_BYTES = 2;

    static final int FIRMWARE_VERSION_LENGTH = 3;
    static final String FIRMWARE_VERSION_SPLITTER = "\\.";

    private final Context context;

    private OADState currentState;
    private OADStateListener stateListener;

    public OADManager(Context context) {
        this.context = context;
    }

    void notifyOADStateChanged(@NonNull OADState state) {
        currentState = state;
        if(state.triggersReset())
            reset();
    }

    void reset() {
        notifyOADStateChanged(null);
    }

    void abort(String reason) {

        notifyOADStateChanged(OADState.ABORTED);
    }

    public boolean prepareOADFile(String oadFilePath){

        return true;//todo
    }

    public boolean initiateOADRequest(){

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

    public boolean verifyFirmwareVersion(){

        return true;//todo
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
