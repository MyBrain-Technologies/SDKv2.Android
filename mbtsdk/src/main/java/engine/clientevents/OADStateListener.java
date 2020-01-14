package engine.clientevents;

import androidx.annotation.IntRange;
import androidx.annotation.Keep;

import core.device.oad.OADState;

/**
 * Listener used to receive a notification when the OAD update state changes during a firmware update
 * @param <U> Error triggered if something went wrong during the firmware update
 */
@Keep
public interface OADStateListener<U extends BaseError> extends BaseErrorEvent<U>{

    int MINIMUM_PROGRESS = 0;
    int MAXIMUM_PROGRESS = 100;

    /**
     * Callback triggered when a new step of the OAD (Over the Air Download)
     * is completed during a firmware update.
     * See {@link OADState} for all possible states
     */
    void onStateChanged(OADState newState);

    /**
     * Callback triggered when the firmware transfer progress changes
     * @param progress returns the current update progress, as a percentage
     * and is computed from the number of packets transfered
     * A progress of 0 means that the transfer has not started yet.
     * A progress of 100 means that the transfer is complete.
     */
    void onProgressPercentChanged(@IntRange(from = MINIMUM_PROGRESS, to = MAXIMUM_PROGRESS) int progress);
}