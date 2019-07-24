package core.device.oad;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.apache.commons.lang.StringUtils;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import command.OADCommands;
import core.device.event.OADEvent;
import core.device.model.FirmwareVersion;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import eventbus.events.FirmwareUpdateClientEvent;
import utils.BitUtils;
import utils.LogUtils;
import utils.MbtAsyncWaitOperation;
import utils.OADExtractionUtils;

/**
 * OAD Manager class is responsible for managing the OAD (Over the Air Download) update process.
 * This process contains several steps that are handled one after another (no parallel tasks).
 * Each of these steps are represented with a state machine so that we can know which step is the current step at any moment.
 * This process uses Bluetooth to transfer a binary file that contains a firmware to the peripheral headset device.
 * The peripheral headset device can then install the received firmware.
 * As the whole file can not be sent in a single request, the OAD Manager prepares & chunks it into small packets that are sent one after another.
 * It also communicates with the current firmware
 */
public final class OADManager {

    private final static String TAG = OADManager.class.getName();

    /**
     * Expected number of packets of the OAD binary file (chunks of the file that hold the firmware to install) to send to the headset device
     */
    public static final int EXPECTED_NB_PACKETS_BINARY_FILE = 14223;

    /**
     * Size of the index of a packet of the OAD binary file (chunks of the file that hold the firmware to install)
     */
    public static final int OAD_INDEX_PACKET_SIZE = 2;

    /**
     * Size of the content of a packet of the OAD binary file (chunks of the file that hold the firmware to install)
     */
    public static final int OAD_PAYLOAD_PACKET_SIZE = 18;

    /**
     * Size of a packet of the OAD binary file (chunks of the file that hold the firmware to install)
     */
    public static final int OAD_PACKET_SIZE = OAD_INDEX_PACKET_SIZE + OAD_PAYLOAD_PACKET_SIZE;

    private final Context context;

    /**
     * Interface used to notify its listener when an OAD event occurs
     */
    private OADContract oadContract;

    /**
     * Return the current state of the current OAD update process.
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    private OADState currentState;
    /**
     * Packet counter that is incremented at every sent OAD packet (chunk of file that holds the firmware to install)
     */
    private PacketCounter packetCounter;

    private OADContext oadContext;

    MbtAsyncWaitOperation<Boolean> waiter;

    public OADManager(Context context, OADContract oadContract, Object initData) {
        this.context = context;
        this.oadContext = new OADContext();
        this.oadContract = oadContract;
        onOADStateChanged(OADState.INITIALIZING, initData);
    }

    /**
     * Switch to the OAD update state that follow the current state
     * @param actionData is an optional data associated to the action to perform
     */
    private void switchToNextStep(@Nullable Object actionData) {
        LogUtils.d(TAG, "Switch to next step");
        onOADStateChanged(getCurrentState().nextState(), actionData);
    }

    /**
     * Change the current state, execute the associated action, and notify the client of this change
     * @param state the new state
     * @param actionData the associated data
     */
    void onOADStateChanged(OADState state, @Nullable Object actionData) {
        LogUtils.d(TAG, "OAD State changed : "+currentState +" > "+state);
        currentState = state;
        oadContract.notifyClient(new FirmwareUpdateClientEvent(currentState));
        if(currentState != null)
            currentState.executeAction(this, actionData);
    }

    /**
     * Abort the current OAD update
     */
    void abort() {
        onOADStateChanged(OADState.ABORTED, null);
    }

    /**
     * Initialize the OAD update process
     * Extract from the binary file that holds the new firmware:
     * - the firmware version
     * - the number of OAD packets (chunks of binary file)
     * @param firmwareVersion is the firmware version to install
     * @return true if the
     */
    void init(FirmwareVersion firmwareVersion){
        LogUtils.d(TAG, "Initialize the OAD update for version "+firmwareVersion);
        String oadFilePath = OADExtractionUtils.getFileNameForFirmwareVersion(firmwareVersion.getFirmwareVersionAsString());
        oadContext.setOADfilePath(oadFilePath);
        try {
            byte[] content = OADExtractionUtils.extractFileContent(context.getAssets(), oadContext.getOADfilePath());
            Pair<Boolean, String> validityStatusAndError = checkFileContentValidity(content);
            if(!validityStatusAndError.first){
                onError(OADError.ERROR_INIT_FAILED, validityStatusAndError.second);
                return;
            }

            packetCounter = new PacketCounter(content.length, OAD_PACKET_SIZE);
            oadContext.setNbPacketsToSend(packetCounter.nbPacketToSend);
            oadContext.setPacketsToSend(OADExtractionUtils.extractOADPackets(packetCounter, content));
            oadContext.setFirmwareVersion(OADExtractionUtils.extractFirmwareVersionFromContent(content));
        } catch (FileNotFoundException e) {
            onError(OADError.ERROR_INIT_FAILED, e.getMessage());
            return;
        }
        switchToNextStep(null);
    }

    /**
     * Check that the OAD file content extracted is valid to start the OAD update
     * @param fileContent the OAD file content to check
     * @return a pair that associates the validity status to a String message that holds an invalidity reason if the status is invalid
     */
    private Pair<Boolean, String> checkFileContentValidity(byte[] fileContent){
        String additionalErrorInfo = StringUtils.EMPTY;
        if(fileContent == null)
            additionalErrorInfo = "Impossible to read the OAD binary file";

        if(fileContent.length != EXPECTED_NB_PACKETS_BINARY_FILE)
            additionalErrorInfo = "Expected length is " + EXPECTED_NB_PACKETS_BINARY_FILE + ", but found length was " + fileContent.length + ".";

        return new Pair<>(additionalErrorInfo.isEmpty(), additionalErrorInfo);
    }

    void requestFirmwareValidation(short nbPacketToSend, FirmwareVersion firmwareVersion){
        OADCommands.RequestFirmwareValidation requestFirmwareValidation = new OADCommands.RequestFirmwareValidation(
                firmwareVersion,
                nbPacketToSend);
        if(oadContract != null)
            oadContract.requestFirmwareValidation(requestFirmwareValidation);
    }

    /**
     *  The whole OAD binary file previously chucked in packets is sent to the current firmware
     *  The packets are sent one after another using the WRITE_NO_RESPONSE capability.
     *  This means that the firmware will not notify the client that the packet is correctly received.
     * @param timeout maximum duration allocated to transfer all the OAD packets to the connected peripheral headsdet device.
     */
    void transferOADFile(int timeout){
        sendOADPacket(packetCounter.getIndexOfNextPacketToSend());

            boolean isTransferSuccess = waitUntilTimeout(timeout);
            if(isTransferSuccess)
                switchToNextStep(null);
            else
                onError(OADError.ERROR_TIMEOUT_UPDATE, "Transfer timed out.");
    }

    /**
     * Send an OAD packet to the peripheral headset device a Bluetooth write characteristic operation.
     * @param packetIndex is the index of the packet to send
     */
    private void sendOADPacket(int packetIndex) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(oadContract != null)
                    oadContract.transferPacket(oadContext.getPacketsToSend().get(packetIndex));
            }
        },100);
    }

    /**
     * Method triggered when an OAD packet has been well sent in Bluetooth to the peripheral headset device.
     */
    private void onOADPacketSent(){
        packetCounter.incrementNbPacketsSent();
        onProgressChanged();
        if(packetCounter.areAllPacketsSent()) {
            stopWaiting(true);
        }else
            sendOADPacket(packetCounter.getIndexOfNextPacketToSend());
    }

    /**
     * Wait an OAD event to be triggered until timeout
     */
    private boolean waitUntilTimeout(int timeout){
        try {
            waiter = new MbtAsyncWaitOperation<>();
            return waiter.waitOperationResult(timeout);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.e(TAG,""+e);
        }
        return false;
    }

    /**
     * Stop the current waiter instance
     * @param isSuccess is the status/response provided by the stopWaiting caller
     */
    private void stopWaiting(boolean isSuccess){
        waiter.stopWaitingOperation(isSuccess);
    }

    /**
     * Method triggered when a message/response of the headset device
     * related to the OAD update process
     * is received by the Device unit Manager
     */
    public void onOADEvent(OADEvent event) {
        LogUtils.d(TAG, "on OAD event "+event.toString());
        switch (event){
            case LOST_PACKET:
                packetCounter.resetNbPacketsSent(event.getEventDataAsByteArray()); //Packet index is set back to the requested value
                break;

            case DISCONNECTED:
                onError(BluetoothError.ERROR_NOT_CONNECTED, null);
                break;

                case PACKET_TRANSFERRED:
                onOADPacketSent();
                break;

            default:
                if(waiter.isWaiting())
                    stopWaiting(true);
                switchToNextStep(event.getEventData());
                break;
        }
    }

    private void onProgressChanged(){
        currentState.incrementProgress();
        oadContract.notifyClient(new FirmwareUpdateClientEvent(currentState.convertToProgress()));
    }

    private void onError(BaseError error, String additionalInfo){
        oadContract.notifyClient(new FirmwareUpdateClientEvent(error, additionalInfo));
        abort();
    }

    /**
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    public enum OADState {

        /**
         * State triggered when the client requests an OAD firmware update.
         * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
         * the file is chucked into small packets.
         */
        INITIALIZING(0){

            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                FirmwareVersion firmwareVersion = getFirmwareVersion(actionData);
                if(firmwareVersion != null)
                    oadManager.init(firmwareVersion);
                else
                    oadManager.onError(getError(), null);
            }

            @Override
            public OADState nextState() {
                return INITIALIZED;
            }

            @Override
            public BaseError getError() {
                return OADError.ERROR_INVALID_FIRMWARE_VERSION;
            }

            private FirmwareVersion getFirmwareVersion(Object actionData){
                return new FirmwareVersion(actionData.toString());
            }
        },

        /**
         * State triggered when an OAD request is ready
         * to be submitted for validation by the headset device that is out-of-date.
         * The SDK is then waiting for a return response that validate or invalidate the OAD request.
         */
        INITIALIZED(2){

            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                OADCommands.RequestFirmwareValidation requestFirmwareValidation = new OADCommands.RequestFirmwareValidation(
                        oadManager.oadContext.getFirmwareVersion(),
                        oadManager.oadContext.getNbPacketsToSend());
                oadManager.oadContract.requestFirmwareValidation(requestFirmwareValidation);
            }

            @Override
            public OADState nextState() {
                return READY_TO_TRANSFER;
            }
        },

        /**
         * State triggered once the out-of-date headset device has validated the OAD request
         * to start the OAD packets transfer.
         */
        READY_TO_TRANSFER(2, 60000){

            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                if(isValidationSuccess(actionData))
                    oadManager.transferOADFile(this.getMaximumDuration());
                else
                    oadManager.onError(getError(), null);
            }

            @Override
            public OADState nextState() {
                return TRANSFERRED;
            }

            @Override
            public BaseError getError() {
                return OADError.ERROR_FIRMWARE_REJECTED_UPDATE;
            }

            private boolean isValidationSuccess(Object actionData){
                return !BitUtils.isZero(((byte[])actionData)[0]);
            }
        },

        /**
         * State triggered once the transfer is complete
         * (all the packets have been transferred by the SDK to the out-of-date headset device).
         * The SDK is then waiting that the headset device returns a success or failure transfer state.
         * For example, it might return a failure state if any corruption occurred while transferring the binary file.
         */
        TRANSFERRED(110, 10000){

            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                boolean isSuccess = oadManager.waitUntilTimeout(this.getMaximumDuration());
                if(!isSuccess)
                    oadManager.onError(OADError.ERROR_TIMEOUT_UPDATE, "Readback timed out.");
            }

            @Override
            public OADState nextState() {
                return AWAITING_DEVICE_REBOOT;
            }
        },

        /**
         * State triggered when the SDK has received a success transfer response from the headset device
         * and when it detects that the previously connected headset device is disconnected.
         * The SDK needs to reset the mobile device Bluetooth (disable then enable)
         * and clear the pairing keys of the updated headset device.
         */
        AWAITING_DEVICE_REBOOT(110,20000){

            @Override
            public void executeAction(OADManager oadManager,Object actionData) {
                if(isReadbackSuccess(actionData)) {
                    boolean isSuccess = oadManager.waitUntilTimeout(this.getMaximumDuration());
                    if(!isSuccess)
                        oadManager.onError(OADError.ERROR_TIMEOUT_UPDATE, "Reboot timed out.");
                }else
                    oadManager.onError(getError(), null);
            }

            @Override
            public OADState nextState() {
                return READY_TO_RECONNECT;
            }

            @Override
            public BaseError getError() {
                return OADError.ERROR_TRANSFER_FAILED;
            }

            private boolean isReadbackSuccess(Object actionData){
                return !BitUtils.isZero(((byte[])actionData)[0]);
            }
        },

        /**
         * State triggered when the SDK has detected that the previously connected headset device is disconnected.
         * The SDK needs to reset the mobile device Bluetooth (disable then enable)
         * and clear the pairing keys of the updated headset device.
         */
        READY_TO_RECONNECT(110){

            @Override
            public void executeAction(OADManager oadManager,Object actionData) {
                oadManager.oadContract.clearBluetooth();
                oadManager.switchToNextStep(null);
            }

            @Override
            public OADState nextState() {
                return RECONNECTING;
            }

            @Override
            public BaseError getError() {
                return BluetoothError.ERROR_REBOOT_FAILED;
            }
        },

        /**
         * State triggered when the SDK is reconnecting the updated headset device.
         */
        RECONNECTING(115, 20000){
            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                oadManager.oadContract.reconnect();
                oadManager.waitUntilTimeout(this.getMaximumDuration());
            }

            @Override
            public OADState nextState() {
                return RECONNECTION_PERFORMED;
            }
        },

        /**
         * State triggered when the headset device is reconnected.
         * The SDK checks that update has succeeded by reading the current firmware version
         * and compare it to the OAD file one.
         */
        RECONNECTION_PERFORMED(117){
            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                if(isReconnectionSuccess(actionData)){
                    boolean isEqual = oadManager.oadContract.compareFirmwareVersion(oadManager.oadContext.getFirmwareVersion());
                    if(isEqual)
                        oadManager.switchToNextStep(null);
                    else
                        oadManager.onError(OADError.ERROR_WRONG_FIRMWARE_VERSION, null);
                }else
                    oadManager.onError(getError(), null);
            }

            @Override
            public OADState nextState() {
                return COMPLETED;
            }

            @Override
            public BaseError getError() {
                return OADError.ERROR_RECONNECT_FAILED;
            }

            private boolean isReconnectionSuccess(Object actionData){
                return (boolean)actionData;
            }
        },

        /**
         * State triggered when an OAD update is completed (final state)
         */
        COMPLETED(120){
            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                oadManager.oadContract.stopOADUpdate();
            }
        },

        /**
         * State triggered when the SDK encounters a problem
         * that is blocking and that keeps from doing any OAD update.
         */
        ABORTED(0){
            @Override
            public void executeAction(OADManager oadManager, Object actionData) {
                oadManager.oadContract.stopOADUpdate();
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

        /**
         * Maximum amount of time to allocate for the state.
         * A timeout error is triggered if the step is not complete within this allocated time.
         */
        private int timeout;

        OADState(int progress) {
            this.progress = progress;
        }

        OADState(int progress, int timeout) {
            this.progress = progress;
            this.timeout = timeout;
        }

        /**
         * (Method to override)
         * Execute a specific action according to the current state
         * Data can be passed in input to specify a
         * @param actionData bundle that contains all the data necessary to perform the action
         *                   Several parameters can be stored in the bundle.
         *                   Null bundle can also be passed if the action doesn't need any data.
         */
        public abstract void executeAction(OADManager oadManager, @Nullable Object actionData);

        /**
         * Return the next state according to the current state
         * Return null if the last step has been reached
         * @return the state that follows the current state in the OAD update process
         */
        public OADState nextState(){
            return null;
        }

        /**
         * Return the error to raise in case the current state encountered a problem.
         * Return null if no error is raisable
         * @return the error to raise in case the current state encountered a problem.
         */
        public BaseError getError(){
            return null;
        }

        /**
         * Get the maximum amount of time to allocate for the state.
         * @return the maximum amount of time to allocate for the state.
         */
        public int getMaximumDuration() {
            return timeout;
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
         * Increment the OAD update progress
         * A progress of 0 means that the transfer has not started yet.
         * A progress of 100 means that the transfer is complete.
         */
        public int incrementProgress() {
            return this.progress++;
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
    }

    /**
     * Return the current state of the current OAD update process.
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    public OADState getCurrentState(){
        return currentState;
    }

}
