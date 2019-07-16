package core.device.oad;

import android.support.annotation.IntRange;

public enum OADState {

     /**
     * State triggered when the client requests an OAD firmware update.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    INIT(0),

    /**
     * State triggered when an OAD request is ready
     * to be submitted for validation by the headset device that is out-of-date.
     * The SDK is then waiting for a return response that validate or invalidate the OAD request.
     */
    FIRMWARE_VALIDATION(2, 10000),

    /**
     * State triggered once the out-of-date headset device has validated the OAD request
     * to start the OAD packets transfer.
     */
    TRANSFERRING(5,600000),

    /**
     * State triggered once the transfer is complete
     * (all the packets have been transferred by the SDK to the out-of-date headset device).
     * The SDK is then waiting that the headset device returns a success or failure transfer state.
     * For example, it might return a failure state if any corruption occurred while transferring the binary file.
     */
    AWAITING_DEVICE_READBACK(105,10000),

    /**
     * State triggered when the SDK has received a success transfer response from the headset device
     * and when it detects that the previously connected headset device is disconnected.
     * The SDK needs to reset the mobile device Bluetooth (disable then enable)
     * and clear the pairing keys of the updated headset device.
     */
    REBOOTING(110,20000),

    /**
     * State triggered when the SDK is reconnecting the updated headset device.
     */
    RECONNECTING(115),

    /**
     * State triggered when the headset device is reconnected.
     * The SDK checks that update has succeeded by reading the current firmware version
     * and compare it to the OAD file one.
     */
    VERIFYING_FIRMWARE_VERSION(117),

    /**
     * State triggered when an OAD update is completed (final state)
     */
    COMPLETE(120),

    /**
     * State triggered when the SDK encounters a problem
     * that is blocking and that keeps from doing any OAD update.
     */
    ABORTED();

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

    OADState() {

    }

    OADState(int progress) {
        this.progress = progress;
    }

    OADState(int progress, int timeout) {
        this.progress = progress;
        this.timeout = timeout;
    }

    public void setProgress(@IntRange(from = MINIMUM_INTERNAL_PROGRESS, to = MAXIMUM_INTERNAL_PROGRESS) int progress) {
        this.progress = progress;
    }

    public int convertToProgress() {
       return progress * 100 / 120;
    }

    public int getMaximumDuration() {
        return timeout;
    }
}
