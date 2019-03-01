package engine.clientevents;

import android.support.annotation.Keep;

import core.bluetooth.BtState;

@Keep
public interface ConnectionStateListener<U extends BaseError> extends BaseErrorEvent<U>{
        /**
         * Callback indicating the current state of the bluetooth communication
         * See {@link BtState} for all possible states
         */
        void onDeviceConnected();
        void onDeviceDisconnected();
    }