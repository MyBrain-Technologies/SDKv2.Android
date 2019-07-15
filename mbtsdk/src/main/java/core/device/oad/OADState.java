package core.device.oad;

public enum OADState {

    /**
     * Initial pending state that describe a state
     * where the SDK is waiting for a headset device connection.
     * This state is triggered when any headset device is disconnected.
     */
    IDLE,

    /**
     * State triggered when a device has just been connected
     * if the firmware version is lower than the last released version.
     */
    UPDATE_NEEDED,

    /**
     * State triggered when the client requests an OAD firmware update.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    PREPARING_REQUEST,

    /**
     * State triggered when an OAD request is ready (preparation complete)
     * to be submitted for validation by the headset device that is out-of-date.
     */
    SENDING_VALIDATION_REQUEST,

    /**
     * State triggered when the validation request is sent
     * to the headset device that is out-of-date.
     * The SDK is then waiting for a return response that validate or invalidate the OAD request.
     */
    VALIDATION_REQUEST_SENT,

    /**
     * State triggered when the out-of-date headset device validate the OAD request.
     */
    DEVICE_VALIDATED,

    /**
     * State triggered once the out-of-date headset device has validated the OAD request
     * to start the OAD packets transfer.
     */
    TRANSFER_STARTED,

    /**
     * State triggered when all the packets have been transferred by the SDK to the out-of-date headset device.
     */
    TRANSFER_COMPLETE,

    /**
     * State triggered once the transfer is complete.
     * The SDK is then waiting that the headset device returns a success or failure transfer state.
     * For example, it might return a failure state if any corruption occurred while transferring the binary file.
     */
    WAITING_DEVICE_READBACK,

    /**
     * State triggered when the SDK receives a success transfer state from the headset device.
     */
    READBACK_SUCCESS,

    /**
     * State triggered when the SDK has received a success transfer response from the headset device
     * and when it detects that the previously connected headset device is disconnected.
     * The SDK needs to reset the mobile device Bluetooth (disable then enable)
     * and clear the pairing keys of the updated headset device.
     */
    REBOOT,

    /**
     * State triggered when the SDK is reconnecting the updated headset device.
     */
    RECONNECTING,

    /**
     * State triggered when the headset device is reconnected.
     * The SDK checks that update has succeeded by reading the current firmware version
     * and compare it to the OAD file one.
     */
    VERIFYING_FIRMWARE_VERSION,

    /**
     * State triggered :
     * - when a device has just been connected, if the firmware version is equal to the last released version.
     * - when an OAD update is completed (final state)
     */
    UP_TO_DATE,

    /**
     * State triggered when the headset device invalidate the OAD request prepared by the SDK.
     */
    DEVICE_REJECTED,

    /**
     * State triggered when the SDK encounters a problem
     * that is blocking and that keeps from doing any OAD update.
     */
    ABORTED,

}
