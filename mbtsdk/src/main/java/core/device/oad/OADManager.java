package core.device.oad;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;

import command.OADCommands;
import core.bluetooth.requests.CommandRequestEvent;
import core.device.MbtDeviceManager;
import core.device.model.MbtDevice;
import engine.clientevents.BaseError;
import engine.clientevents.OADStateListener;
import eventbus.EventBusManager;
import utils.LogUtils;


public final class OADManager {

    private final static String TAG = OADManager.class.getName();

    private final static String BINARY_HOOK = "mm-ota-";
    private final static String BINARY_FORMAT = ".bin";
    private final static String FWVERSION_REGEX = "_";

    static final int BUFFER_LENGTH = 14223;
    static final int CHUNK_NB_BYTES = 14223;
    static final int FILE_LENGTH_NB_BYTES = 4;
    static final int FIRMWARE_VERSION_NB_BYTES = 2;

    public static final int FIRMWARE_VERSION_LENGTH = 3;
    public static final String FIRMWARE_VERSION_SPLITTER = "\\.";

    private final Context context;
    private final MbtDeviceManager deviceManager;

    private OADState currentState;
    private OADStateListener stateListener;
    private EventListener.OADEventListener eventListener;

    public OADManager(Context context, MbtDeviceManager deviceManager) {
        this.context = context;
        this.deviceManager = deviceManager;

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

    private void initOADEventListener() {
        eventListener = new EventListener.OADEventListener() {
            @Override
            public void onOADEvent(OADEvent oadEvent) {
                switch (oadEvent){
                    case INIT:
                        if(oadEvent.getAssociatedValue() instanceof FirmwareVersion) {
                            FirmwareVersion firmwareVersion = ((FirmwareVersion) oadEvent.getAssociatedValue());
                            startOADupdate(firmwareVersion);
                        }
                        break;

                    case FIRMWARE_VALIDATION:
                        if(oadEvent.getAssociatedValue() instanceof Boolean){
                            boolean isValidated = (boolean) oadEvent.getAssociatedValue();
                            if(isValidated){
                                transferOADFile();
                            }
                        }
                        break;

                    case LOST_PACKET:
                        if(oadEvent.getAssociatedValue() instanceof Integer){
                            int packetIndex = (int) oadEvent.getAssociatedValue();
                            sendOADPacket(packetIndex);
                        }
                        break;

                    case CRC_READBACK:
                        if(oadEvent.getAssociatedValue() instanceof Boolean){
                            boolean isTransferSuccess = (boolean) oadEvent.getAssociatedValue();
                            if(isTransferSuccess){
                                reboot();
                            }
                        }
                        break;

                    case REBOOT_PERFORMED:
                        reconnect();
                        break;

                    case RECONNECTION_PERFORMED:
                        if(oadEvent.getAssociatedValue() instanceof Boolean){
                            boolean isReconnectionSuccess = (boolean) oadEvent.getAssociatedValue();
                            if(isReconnectionSuccess){
                                verifyFirmwareVersion();
                            }
                        }
                        break;

                    case UPDATE_COMPLETE:
                        deinit();
                        break;

                }
                deviceManager.setOADEventListener(eventListener);
            }

            @Override
            public void onError(BaseError error, String additionalInfo) {

            }
        };

    }

    void startOADupdate(FirmwareVersion firmwareVersion){
        String oadFilePath = BINARY_HOOK + firmwareVersion.getFirmwareVersionAsString() + BINARY_FORMAT;
        init(oadFilePath);
        requestFirmwareValidation(oadContext.getFirmwareVersion(), oadContext.getNbBytesToSend());
    }

    /**
     * Initialize the OAD update process
     * Extract from the binary file that holds the new firmware:
     * - the firmware version
     * - the number of OAD packets (chunks of binary file)
     * @param oadFilePath is the path of the binary file that holds the new firmware
     * @return true if the
     */
    void init(String oadFilePath){
        this.packetEngine = new PacketCounter();
        initOADEventListener();
        oadContext.setFilePath(oadFilePath);
        try {
            byte[] content = OADExtractionUtils.extractFileContent(context.getAssets(), oadFilePath);
            String firmwareVersion = OADExtractionUtils.extractFirmwareVersion(content);
            oadContext.setNbBytesToSend(content.length);
            oadContext.setFirmwareVersion(firmwareVersion);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    void deinit() {
        eventListener = null;
    }

    void requestFirmwareValidation(short nbPacketToSend, String firmwareVersion){
        OADCommands.RequestFirmwareValidation requestFirmwareValidation = new OADCommands.RequestFirmwareValidation(
                firmwareVersion,
                nbPacketToSend);
        EventBusManager.postEvent(new CommandRequestEvent(requestFirmwareValidation));
    }

    public boolean transferOADFile(){

        return true;//todo
    }

    void sendOADPacket(int packetIndex) {

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
