package core.device.oad;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import command.OADCommands;
import core.device.model.FirmwareVersion;
import engine.clientevents.BaseError;
import engine.clientevents.OADError;
import utils.BitUtils;

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
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
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
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                OADCommands.RequestFirmwareValidation requestFirmwareValidation = new OADCommands.RequestFirmwareValidation(
                        oadManager.getOADContext().getFirmwareVersion(),
                        oadManager.getOADContext().getNbPacketsToSend());
                oadManager.getOADContract().requestFirmwareValidation(requestFirmwareValidation);
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
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                if(isValidationSuccess(actionData))
                    oadManager.transferOADFile(this.getMaximumDuration());
                else
                    oadManager.onError(getError(), null);
            }

            @Override
            public OADState nextState() {
                return TRANSFERRING;
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
         * State triggered once the transfer is started
         */
        TRANSFERRING(110){

            @Override
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) { }

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
        TRANSFERRED(110, 10000){

            @Override
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                boolean isSuccess = oadManager.waitUntilTimeout(this.getMaximumDuration());
                if(!isSuccess)
                    oadManager.onError(getError(), "Readback timed out.");
            }

            @Override
            public OADState nextState() {
                return AWAITING_DEVICE_REBOOT;
            }

            @Override
            public BaseError getError() {
                return OADError.ERROR_TIMEOUT_UPDATE;
            }
        },

        /**
         * State triggered when the SDK has received a success transfer response from the headset device
         * and when it detects that the previously connected headset device is disconnected.
         */
        AWAITING_DEVICE_REBOOT(110,20000){

            @Override
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                if(isReadbackSuccess(actionData)) {
                    boolean isSuccess = oadManager.waitUntilTimeout(this.getMaximumDuration());
                    if(!isSuccess)
                        oadManager.onError(getError(), "Reboot timed out.");
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
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                oadManager.getOADContract().clearBluetooth();
                oadManager.switchToNextStep(null);
            }

            @Override
            public OADState nextState() {
                return RECONNECTING;
            }
        },

        /**
         * State triggered when the SDK is reconnecting the updated headset device.
         */
        RECONNECTING(115, 20000){
            @Override
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                oadManager.getOADContract().reconnect();
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
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                if(isReconnectionSuccess(actionData)){
                    boolean isEqual = oadManager.getOADContract().compareFirmwareVersion(oadManager.getOADContext().getFirmwareVersion());
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
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
                oadManager.getOADContract().stopOADUpdate();
            }
        },

        /**
         * State triggered when the SDK encounters a problem
         * that is blocking and that keeps from doing any OAD update.
         */
        ABORTED(0){
            @Override
            public void executeAction(core.device.oad.OADManager oadManager, Object actionData) {
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
        public abstract void executeAction(core.device.oad.OADManager oadManager, @Nullable Object actionData);

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


