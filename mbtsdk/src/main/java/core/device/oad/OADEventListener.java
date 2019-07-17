package core.device.oad;

import android.support.annotation.IntRange;
import android.support.annotation.Keep;

import engine.clientevents.BaseError;
import engine.clientevents.BaseErrorEvent;

/**
 * Listener used to receive a notification when an OAD event occurs
 * @param <U> Error triggered if something went wrong during the firmware update
 */
@Keep
public interface OADEventListener<U extends BaseError> extends BaseErrorEvent<U>{

    /**
     * Callback triggered when a new step of the OAD (Over the Air Download)
     * is completed during a firmware update.
     * See {@link OADState} for all possible states
     */
    void onStateChanged(OADState newState);

    /**
     * Callback triggered when the Bluetooth unit receives
     * a response to its OAD validation request from the headset device.
     * The response returns :
     * - true if the headset device accepts the OAD update.
     * - false if the headset device rejects the OAD update.
     * @param isValidated is true if the headset device accepts the OAD update, false otherwise
     */
    void onFirmwareValidation(boolean isValidated);

    /**
     * Callback triggered when the Bluetooth unit is informed
     * that a sent packet has not been received by the headset device
     * @param packetIndex is the identifier of the packet,
     *                   that allow the SDK to resend the corresponding packet
     */
    void onPacketLost(int packetIndex);

    /**
     * Callback triggered when the current firmware has checked the CRC (Cyclic Redundancy Check)
     * in order to make sure that all the packets have been received and
     * that any corruption occurred while transferring the binary file.
     * @param isTransferSuccess is :
     *                          - true if all the packets have been well transferred and no corruption occurred.
     *                          - false if all the packets have been well transferred and no corruption occurred
     */
    void onReadbackReceived(boolean isTransferSuccess);

    void onRebootPerformed();

    void onReconnectionPerformed();

    void onUpdateComplete(boolean isSuccess);



}