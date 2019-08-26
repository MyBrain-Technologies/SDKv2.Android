package core.device.oad;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.util.Log;

import command.OADCommands;
import core.device.model.FirmwareVersion;
import engine.clientevents.OADError;
import utils.AsyncUtils;
import utils.BitUtils;
import utils.LogUtils;
import utils.OADExtractionUtils;

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
    INITIALIZING(){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            FirmwareVersion firmwareVersion = getFirmwareVersion(actionData);
            if(firmwareVersion != null)
                oadManager.init(firmwareVersion);
            else
                oadManager.onError(OADError.ERROR_INVALID_FIRMWARE_VERSION, null);
        }

        @Override
        int getDefaultProgress() {
            return 0;
        }

        @Override
        public OADState nextState() {
            return INITIALIZED;
        }

        private FirmwareVersion getFirmwareVersion(Object actionData){
            if(actionData instanceof String)
                return new FirmwareVersion((String)actionData);
            else if (actionData instanceof FirmwareVersion)
                return (FirmwareVersion) actionData;
            return null;
        }
    },

    /**
     * State triggered when an OAD request is ready
     * to be submitted for validation by the headset device that is out-of-date.
     * The SDK is then waiting for a return response that validate or invalidate the OAD request.
     */
    INITIALIZED(13000){//2 sec added to take into account delay induced by the important number of intermediaries involved in the OAD update process

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            OADCommands.RequestFirmwareValidation requestFirmwareValidation = new OADCommands.RequestFirmwareValidation(
                    oadManager.getOADContext().getFirmwareVersionAsByteArray(),
                    oadManager.getOADContext().getNbPacketsToSend());
            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    oadManager.getOADContract().requestFirmwareValidation(requestFirmwareValidation);
                }
            });
            boolean isSuccess = oadManager.waitUntilTimeout(getMaximumDuration());
            if(!isSuccess)
                oadManager.onError(OADError.ERROR_TIMEOUT_UPDATE, "Validation timed out.");
        }

        @Override
        int getDefaultProgress() {
            return 1;
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
    READY_TO_TRANSFER(){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            if(isValidationSuccess(actionData))
                oadManager.transferOADFile();
            else
                oadManager.onError(OADError.ERROR_FIRMWARE_REJECTED_UPDATE, null);
        }

        @Override
        public OADState nextState() {
            return TRANSFERRING;
        }

        private boolean isValidationSuccess(Object actionData){
            return !BitUtils.isZero(((byte[])actionData)[0]);
        }

        @Override
        int getDefaultProgress() {
            return 2;
        }
    },

    /**
     * State triggered once the transfer is started
     */
    TRANSFERRING(600000){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    boolean isTransferSuccess = oadManager.waitUntilTimeout(getMaximumDuration());
                    if(isTransferSuccess)
                        oadManager.switchToNextStep(null);
                    else
                        oadManager.onError(OADError.ERROR_TIMEOUT_UPDATE, "Transfer timed out.");
                }
            });
        }

        @Override
        int getDefaultProgress() {
            return 3;
        }

        @Override
        public OADState nextState() {
            return TRANSFERRED;
        }

    },

    /**
     * State triggered once the transfer is complete
     * (all the packets have been transferred by the SDK to the out-of-date headset device).
     * The SDK is then waiting that the headset device returns a success or failure transfer state.
     * For example, it might return a failure state if any corruption occurred while transferring the binary file.
     */
    TRANSFERRED(12000){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            AsyncUtils.executeAsync(new Runnable() {
                @Override
                public void run() {
                    boolean isSuccess = oadManager.waitUntilTimeout(getMaximumDuration());
                    if (!isSuccess)
                        oadManager.onError(OADError.ERROR_TIMEOUT_UPDATE, "Readback timed out.");
                }
            });
        }

        @Override
        int getDefaultProgress() {
            return 100 + TRANSFERRING.getDefaultProgress();
        }

        @Override
        public OADState nextState() {
            return AWAITING_DEVICE_REBOOT;
        }

    },

    /**
     * State triggered when the SDK has received a success transfer response from the headset device
     * and when it detects that the previously connected headset device is disconnected.
     */
    AWAITING_DEVICE_REBOOT(20000){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            if (isReadbackSuccess(actionData)) {
                boolean isSuccess = oadManager.waitUntilTimeout(getMaximumDuration());
                if (!isSuccess)
                    oadManager.onError(OADError.ERROR_TIMEOUT_UPDATE, "Reboot timed out.");
            } else
                oadManager.onError(OADError.ERROR_TRANSFER_FAILED, null);
        }

        @Override
        int getDefaultProgress() {
            return 107;
        }

        @Override
        public OADState nextState() {
            return READY_TO_RECONNECT;
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
    READY_TO_RECONNECT(){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            oadManager.getOADContract().clearBluetooth();
        }

        @Override
        int getDefaultProgress() {
            return 110;
        }

        @Override
        public OADState nextState() {
            return RECONNECTING;
        }
    },

    /**
     * State triggered when the SDK is reconnecting the updated headset device.
     */
    RECONNECTING(23000){
        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            oadManager.reconnect(getMaximumDuration());
        }

        @Override
        int getDefaultProgress() {
            return 113;
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
    RECONNECTION_PERFORMED(){

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);

            if(isReconnectionSuccess(actionData)){
                boolean isEqual = oadManager.getOADContract()
                        .verifyFirmwareVersion( new FirmwareVersion(
                                OADExtractionUtils.extractFirmwareVersionFromFileName(oadManager.getOADContext().getOADfilepath())));
                if(isEqual)
                    oadManager.switchToNextStep(null);
                else
                    oadManager.onError(OADError.ERROR_WRONG_FIRMWARE_VERSION, null);
            }else
                oadManager.onError(OADError.ERROR_RECONNECT_FAILED, null);
        }

        @Override
        int getDefaultProgress() {
            return 117;
        }

        @Override
        public OADState nextState() {
            return COMPLETED;
        }

        private boolean isReconnectionSuccess(Object actionData){
            return (boolean)actionData;
        }
    },

    /**
     * State triggered when an OAD update is completed (final state)
     */
    COMPLETED(){
        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);
            oadManager.getOADContract().stopOADUpdate();
        }

        @Override
        int getDefaultProgress() {
            return 120;
        }
    },

    /**
     * State triggered when the SDK encounters a problem
     * that is blocking and that keeps from doing any OAD update.
     */
    ABORTED(){
        @Override
        int getDefaultProgress() {
            return 0;
        }

        @Override
        public void executeAction(OADManager oadManager, Object actionData) {
            super.executeAction(oadManager, actionData);
            oadManager.getOADContract().stopOADUpdate();
        }
    };

    private final int MINIMUM_INTERNAL_PROGRESS = 0;
    private final int MAXIMUM_INTERNAL_PROGRESS = 120;

    /**
     * Corresponding progress in percentage
     * A progress of 0 means that the transfer has not started yet.
     * A progress of 100 means that the transfer is complete.
     */
    private float progress;

    /**
     * Corresponding default progress included between 0 and 120
     * A progress of 0 means that the transfer has not started yet.
     * A progress of 120 means that the transfer is complete.
     */
    abstract int getDefaultProgress();

    /**
     * Maximum amount of time to allocate for the state.
     * A timeout error is triggered if the step is not complete within this allocated time.
     */
    private int timeout;

    OADState() {
        this.progress = getDefaultProgress();
    }

    OADState(int timeout) {
        this.timeout = timeout;
        this.progress = getDefaultProgress();
    }

    /**
     * (Method to override)
     * Execute a specific action according to the current state
     * Data can be passed in input to specify a
     * @param actionData bundle that contains all the data necessary to perform the action
     *                   Several parameters can be stored in the bundle.
     *                   Null bundle can also be passed if the action doesn't need any data.
     */
    public void executeAction(OADManager oadManager, @Nullable Object actionData){
        LogUtils.d("Execute action", "for state "+this.name());
        this.progress = getDefaultProgress();
    }

    /**
     * Return the next state according to the current state
     * Return null if the last step has been reached
     * @return the state that follows the current state in the OAD update process
     */
    public OADState nextState(){
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
    public void incrementProgress() {
        this.progress += (100f / OADExtractionUtils.EXPECTED_NB_PACKETS);
    }

    /**
     * Get the OAD update progress according to the current state.
     * @return the OAD update progress in percent.
     * A progress of 0 means that the transfer has not started yet.
     * A progress of 100 means that the transfer is complete.
     */
    public int convertToProgress() {
        return (int)progress * 100 / 120;
    }
}


