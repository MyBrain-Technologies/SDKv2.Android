package core.device.oad;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntRange;

import java.io.FileNotFoundException;

import command.OADCommands;
import core.bluetooth.requests.CommandRequestEvent;
import core.device.MbtDeviceManager;
import core.device.event.EventListener;
import core.device.event.OADEvent;
import core.device.model.FirmwareVersion;
import core.device.model.MbtDevice;
import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import engine.clientevents.OADStateListener;
import eventbus.MbtEventBus;
import utils.LogUtils;

/**
 * OAD Manager class is responsible for managing the OAD (Over the Air Download) update process.
 * This process contains several steps that are handled one after another (no parallel tasks).
 * Each of these steps are represented with a state machine so that we can know which step is the current step at any moment.
 * This process uses Bluetooth to transfer a binary file that contains a firmware to the peripheral headset device.
 * The peripheral headset device can then install the received firmware.
 * As the whole file can not be sent in a single request, the OAD Manager prepares & chunks it into small packets that are sent one after another.
 * It also communicates with the current firmware
 */
public final class OADManager implements EventListener.OADEventListener {

    private final static String TAG = OADManager.class.getName();

    private final static String BINARY_HOOK = "mm-ota-";
    private final static String BINARY_FORMAT = ".bin";
    private final static String FWVERSION_REGEX = "_";

    static final int BUFFER_LENGTH = 14223;
    static final int CHUNK_NB_BYTES = 14223;
    static final int FILE_LENGTH_NB_BYTES = 4;
    static final int FIRMWARE_VERSION_NB_BYTES = 2;


    /**
     * Key used to get the firmware version passed through the bundle passed by the {@link android.bluetooth.BluetoothManager}
     * to the current OAD manager in order to execute the action that matchs the current state.
     */
    final static String FIRMWARE_VERSION = "FIRMWARE_VERSION";
    /**
     * Key used to get the validation status passed through the bundle passed by the {@link android.bluetooth.BluetoothManager}
     * to the current OAD manager in order to execute the action that matchs the current state.
     */
    final static String VALIDATION_STATUS = "VALIDATION_STATUS";
    /**
     * Key used to get the readback status passed through the bundle passed by the {@link android.bluetooth.BluetoothManager}
     * to the current OAD manager in order to execute the action that matchs the current state.
     */
    final static String READBACK_STATUS = "READBACK_STATUS";

    /**
     * Key used to get the reboot status passed through the bundle passed by the {@link android.bluetooth.BluetoothManager}
     * to the current OAD manager in order to execute the action that matchs the current state.
     */
    final static String REBOOT_STATUS = "REBOOT_STATUS";

    /**
     * Key used to get the index of the lost packet passed through the bundle passed by the {@link android.bluetooth.BluetoothManager}
     * to the current OAD manager in order to execute the action that matchs the current state.
     */
    final static String LOST_PACKET = "LOST_PACKET";

    /**
     * Key used to get the reconnection status passed through the bundle passed by the {@link android.bluetooth.BluetoothManager}
     * to the current OAD manager in order to execute the action that matchs the current state.
     */
    final static String RECONNECTION_STATUS = "RECONNECTION_STATUS";


    private final Context context;
    private final MbtDeviceManager deviceManager;

    /**
     * Return the current state of the current OAD update process.
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    private OADState currentState;

    /**
     * Listener used to notify the SDK client when the current OAD state changes or when the SDK raises an error.
     */
    private OADStateListener stateListener;
    private EventListener.OADEventListener eventListener;

    public OADManager(Context context, MbtDeviceManager deviceManager) {
        this.context = context;
        this.deviceManager = deviceManager;

    }

    void switchToNextStep(@Nullable Bundle actionData) {
        notifyOADStateChanged(getCurrentState().getNextState(), actionData);
    }

    /**
     * Return the current state of the current OAD update process.
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    private OADState getCurrentState(){
        if(currentState == null)
                currentState = OADState.INIT;
        return currentState;
    }

    void notifyOADStateChanged(OADState state, @Nullable Bundle actionData) {
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
        MbtEventBus.postEvent(new CommandRequestEvent(requestFirmwareValidation));
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

    /**
     * Set a listener used to notify the SDK client when the current OAD state changes or when the SDK raises an error.
     */
    public void setStateListener(OADStateListener stateListener) {
        this.stateListener = stateListener;
    }

    /**
     * Callback triggered when a message/response of the headset device
     * related to the OAD update process
     * is received by the Device unit Manager
     */
    @Override
    public void onOADEvent(OADEvent oadEvent) {

        if(oadEvent.equals(OADEvent.LOST_PACKET)){
                int packetIndex = oadEvent.getEventData().getInt(oadEvent.getKey());
                sendOADPacket(packetIndex);
        }else
            switchToNextStep(oadEvent.getEventData());

    }

    /**
     * Callback triggered something went wrong during the OAD update process
     */
    @Override
    public void onError(BaseError error, String additionalInfo) {

    }

    private void deinit() {
        deviceManager.setOADEventListener(null);
    }


    /**
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    public enum OADState implements BaseErrorEvent {

        /**
         * State triggered when the client requests an OAD firmware update.
         * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
         * the file is chunked into small packets.
         */
        INIT(0){
            @Override
            public void executeAction(Bundle actionData) {
                if(getFirmwareVersion(actionData) != null) {
                    FirmwareVersion firmwareVersion = ((FirmwareVersion) getFirmwareVersion(actionData));
                    oadManager.startOADupdate(firmwareVersion);
                }else
                    onError(OADError.ERROR_INVALID_FIRMWARE_VERSION, null);
            }

            @Override
            public OADState getNextState() {
                return FIRMWARE_VALIDATION;
            }

            private FirmwareVersion getFirmwareVersion(Bundle actionData){
                return (FirmwareVersion)getObjectFromBundleForKey(actionData,FIRMWARE_VERSION);
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
                if(isValidationSuccess(actionData))
                    oadManager.transferOADFile();
                else
                    onError(OADError.ERROR_FIRMWARE_REJECTED_UPDATE, null);
            }

            @Override
            public OADState getNextState() {
                return AWAITING_DEVICE_READBACK;
            }

            private boolean isValidationSuccess(Bundle actionData){
                return bundleContainsSuccessForKey(actionData, VALIDATION_STATUS);
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
                if(isReadbackSuccess(actionData))
                    oadManager.waitDeviceReadback();
                else
                    onError(OADError.ERROR_UNCOMPLETE_UPDATE, null);
            }

            @Override
            public OADState getNextState() {
                return REBOOTING;
            }

            private boolean isReadbackSuccess(Bundle actionData){
               return bundleContainsSuccessForKey(actionData,READBACK_STATUS);
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
                if(isRebootSuccess(actionData))
                    oadManager.reboot();
                else
                    onError(BluetoothError.ERROR_REBOOT_FAILED, null);

            }

            @Override
            public OADState getNextState() {
                return RECONNECTING;
            }

            private boolean isRebootSuccess(Bundle actionData){
                return bundleContainsSuccessForKey(actionData,REBOOT_STATUS);
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
                if(isReconnectionSuccess(actionData))
                    oadManager.verifyFirmwareVersion();
                else
                    onError(OADError.ERROR_RECONNECT_FAILED, null);

            }

            @Override
            public OADState getNextState() {
                return COMPLETE;
            }

            private boolean isReconnectionSuccess(Bundle actionData){
                return bundleContainsSuccessForKey(actionData,RECONNECTION_STATUS);
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

        /**
         * (Method to override)
         * Execute a specific action according to the current state
         * Data can be passed in input to specify a
         * @param actionData bundle that contains all the data necessary to perform the action
         *                   Several parameters can be stored in the bundle.
         *                   Null bundle can also be passed if the action doesn't need any data.
         */
        public abstract void executeAction(@Nullable Bundle actionData);

        /**
         * Return the next state according to the current state
         * Return null if the last step has been reached
         * @return the state that follows the current state in the OAD update process
         */
        public abstract OADState getNextState();

        /**
         * Return true is the current state is the last state of the OAD update process
         * or has no step to perform after the current one.
         * @return
         */
        private boolean isFinalState(){
            return getNextState() == null;
        }

        /**
         * Return true if the input bundle contains a boolean value associated to the input key
         * and if this boolean value is true.
         * @param key the key associated to the value to get
         * @return the value associated to the key
         */
        public boolean bundleContainsSuccessForKey(Bundle bundle, String key){
            return bundle.containsKey(key) && bundle.getBoolean(key);
        }

        /**
         * Return a value if the input bundle contains a String value associated to the input key
         * @param key the key associated to the value to get
         * @return the value associated to the key
         */
        public Object getObjectFromBundleForKey(Bundle bundle, String key){
            return (bundle.containsKey(key) ?
                    bundle.getParcelable(key) : null);
        }

        /**
         * Set the OAD update progress according to the current state
         * @param progress the OAD update progress in percent.
         * A progress of 0 means that the transfer has not started yet.
         * A progress of 100 means that the transfer is complete.
         */
        public void setProgress(@IntRange(from = MINIMUM_INTERNAL_PROGRESS, to = MAXIMUM_INTERNAL_PROGRESS) int progress) {
            this.progress = progress;
        }

        /**
         * Get the OAD update progress according to the current state.
         * @return the OAD update progress in percent.
         * A progress of 0 means that the transfer has not started yet.
         * A progress of 100 means that the transfer is complete.
         */
        public int convertToProgress() {
            return progress * 100 / 120;
        }

        /**
         * Callback triggered to notify the SDK client when the SDK raises an error.
         */
        @Override
        public void onError(BaseError error, String additionalInfo) {
            if(oadManager != null && oadManager.stateListener != null)
                oadManager.stateListener.onError(error,additionalInfo);
        }
    }

}
