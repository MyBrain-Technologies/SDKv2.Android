package engine.clientevents;

import android.support.annotation.NonNull;

import core.bluetooth.BtState;

public interface ConnectionStateListener<U extends BaseException> extends BaseErrorEvent<U>{
        /**
         * Callback indicating the current state of the bluetooth communication
         * See {@link BtState} for all possible states
         */
        void onStateChanged(@NonNull final BtState newState);
    }