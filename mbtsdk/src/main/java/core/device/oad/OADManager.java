package core.device.oad;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;

import command.OADCommands;
import core.bluetooth.requests.CommandRequestEvent;
import core.device.MbtDeviceManager;
import core.device.model.MbtDevice;
import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import engine.clientevents.OADStateListener;
import eventbus.EventBusManager;
import utils.LogUtils;


public final class OADManager implements EventListener.OADEventListener {

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

    public OADManager(Context context, MbtDeviceManager deviceManager) {
        this.context = context;
        this.deviceManager = deviceManager;

    }

    void switchToNextStep() {
        notifyOADStateChanged(currentState.getNextState(), );
    }

    void notifyOADStateChanged(OADState state, @Nullable Object actionData) {
        currentState = state;
        if(currentState != null)
            currentState.executeAction(actionData);
    }

    void abort(String reason) {

        notifyOADStateChanged(OADState.ABORTED, null);
    }

    public boolean prepareOADFile(String oadFilePath){

        return true;//todo
    }

    final void startOADupdate(FirmwareVersion firmwareVersion){
        deviceManager.setOADEventListener(this);

        String oadFilePath = BINARY_HOOK + firmwareVersion.getFirmwareVersionAsString() + BINARY_FORMAT;
        init(oadFilePath);
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
        this.packetCounter = new PacketCounter();
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

    private void waitDeviceReadback() {
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

    @Override
    public void onOADEvent(OADEvent oadEvent) {
        switch (oadEvent){
            case INIT:

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

            case DISCONNECTED:
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
    }

    private void deinit() {
        deviceManager.setOADEventListener(null);
    }

    @Override
    public void onError(BaseError error, String additionalInfo) {
    }
    
    enum OADState implements BaseErrorEvent {

        /**
         * State triggered when the client requests an OAD firmware update.
         * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
         * the file is chunked into small packets.
         */
        INIT(0){
            @Override
            public void executeAction(Bundle actionData) {
                if(actionData.containsKey(FIRMWARE_VERSION)) {
                    FirmwareVersion firmwareVersion = ((FirmwareVersion) actionData.get(FIRMWARE_VERSION));
                    oadManager.startOADupdate(firmwareVersion);
                }else
                    onError(OADError.ERROR_INVALID_FIRMWARE_VERSION, null);
            }

            @Override
            public OADState getNextState() {
                return FIRMWARE_VALIDATION;
            }
        },

        /**
         * State triggered when an OAD request is ready
         * to be submitted for validation by the headset device that is out-of-date.
         * The SDK is then waiting for a return response that validate or invalidate the OAD request.
         */
        FIRMWARE_VALIDATION(2){
            @Override
            public void executeAction(Bundle actionData) {
                oadManager.requestFirmwareValidation(oadContext.getFirmwareVersion(), oadContext.getNbBytesToSend());
            }

            @Override
            public OADState getNextState() {
                return TRANSFERRING;
            }
        },

        /**
         * State triggered once the out-of-date headset device has validated the OAD request
         * to start the OAD packets transfer.
         */
        TRANSFERRING(5){
            @Override
            public void executeAction(Bundle actionData) {
                if(actionData.containsKey(IS_VALIDATION_SUCCESS) && actionData.getBoolean(IS_VALIDATION_SUCCESS))
                    oadManager.transferOADFile();
                else
                    onError(OADError.ERROR_FIRMWARE_REJECTED_UPDATE, null);
            }

            @Override
            public OADState getNextState() {
                return AWAITING_DEVICE_READBACK;
            }
        },

        /**
         * State triggered once the transfer is complete
         * (all the packets have been transferred by the SDK to the out-of-date headset device).
         * The SDK is then waiting that the headset device returns a success or failure transfer state.
         * For example, it might return a failure state if any corruption occurred while transferring the binary file.
         */
        AWAITING_DEVICE_READBACK(105){
            @Override
            public void executeAction(Bundle actionData) {
                if(actionData.containsKey(IS_READBACK_SUCCESS) && actionData.getBoolean(IS_READBACK_SUCCESS))
                    oadManager.waitDeviceReadback();
                else
                    onError(OADError.ERROR_UNCOMPLETE_UPDATE, null);
            }

            @Override
            public OADState getNextState() {
                return REBOOTING;
            }
        },

        /**
         * State triggered when the SDK has received a success transfer response from the headset device
         * and when it detects that the previously connected headset device is disconnected.
         * The SDK needs to reset the mobile device Bluetooth (disable then enable)
         * and clear the pairing keys of the updated headset device.
         */
        REBOOTING(110){
            @Override
            public void executeAction(Bundle actionData) {
                if(actionData.containsKey(IS_REBOOT_SUCCESS) && actionData.getBoolean(IS_REBOOT_SUCCESS))
                    oadManager.reboot();
                else
                    onError(BluetoothError.ERROR_REBOOT_FAILED, null);

            }

            @Override
            public OADState getNextState() {
                return RECONNECTING;
            }
        },

        /**
         * State triggered when the SDK is reconnecting the updated headset device.
         */
        RECONNECTING(115){
            @Override
            public void executeAction(Bundle actionData) {
                oadManager.reconnect();
            }

            @Override
            public OADState getNextState() {
                return VERIFYING_FIRMWARE_VERSION;
            }
        },

        /**
         * State triggered when the headset device is reconnected.
         * The SDK checks that update has succeeded by reading the current firmware version
         * and compare it to the OAD file one.
         */
        VERIFYING_FIRMWARE_VERSION(117){
            @Override
            public void executeAction(Bundle actionData) {
                if(actionData.containsKey(IS_RECONNECTION_SUCCESS) && actionData.getBoolean(IS_RECONNECTION_SUCCESS))
                    oadManager.verifyFirmwareVersion();
                else
                    onError(OADError.ERROR_RECONNECT_FAILED, null);

            }

            @Override
            public OADState getNextState() {
                return COMPLETE;
            }
        },

        /**
         * State triggered when an OAD update is completed (final state)
         */
        COMPLETE(120){
            @Override
            public void executeAction(Bundle actionData) {
                oadManager.deinit();
            }

            @Override
            public OADState getNextState() {
                return null;
            }
        },

        /**
         * State triggered when the SDK encounters a problem
         * that is blocking and that keeps from doing any OAD update.
         */
        ABORTED(0){
            @Override
            public void executeAction(Bundle actionData) {

            }

            @Override
            public OADState getNextState() {
                return null;
            }
        };

        private final int MINIMUM_INTERNAL_PROGRESS = 0;
        private final int MAXIMUM_INTERNAL_PROGRESS = 120;

        private final static String FIRMWARE_VERSION = "FIRMWARE_VERSION";
        private final static String IS_VALIDATION_SUCCESS = "IS_VALIDATION_SUCCESS";
        private final static String IS_RECONNECTION_SUCCESS = "IS_RECONNECTION_SUCCESS";
        private final static String IS_REBOOT_SUCCESS = "IS_REBOOT_SUCCESS";
        private final static String IS_READBACK_SUCCESS = "IS_READBACK_SUCCESS";

        /**
         * Corresponding progress in percentage
         * A progress of 0 means that the transfer has not started yet.
         * A progress of 100 means that the transfer is complete.
         */
        private int progress;

        OADManager oadManager;

        OADState(int progress) {
            this.progress = progress;
        }

        public abstract void executeAction(Bundle actionData);

        /**
         * Return the next state according to the current state
         * Return if the last step has been reached
         * @return
         */
        public abstract OADState getNextState();

        private boolean isLastState(){
            return this.equals(COMPLETE)
                    || this.ordinal() == OADState.values().length-1;
        }

        public void setProgress(@IntRange(from = MINIMUM_INTERNAL_PROGRESS, to = MAXIMUM_INTERNAL_PROGRESS) int progress) {
            this.progress = progress;
        }

        public int convertToProgress() {
            return progress * 100 / 120;
        }

        public boolean triggersReset(){
            return this.equals(ABORTED);
        }

        @Override
        public void onError(BaseError error, String additionalInfo) {
            oadManager.stateListener.onError(error,additionalInfo);
        }
    }

}
