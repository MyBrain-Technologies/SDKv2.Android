package engine.clientevents;

import android.content.BroadcastReceiver;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import core.bluetooth.BtState;

@Keep
public interface ConnectionStateListener<U extends BaseError> extends BaseErrorEvent<U>{
        /**
         * Callback indicating the current state of the bluetooth communication
         * See {@link BtState} for all possible states
         */
        void onStateChanged(@NonNull final BtState newState);
    }