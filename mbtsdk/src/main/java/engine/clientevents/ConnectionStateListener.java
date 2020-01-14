package engine.clientevents;

import androidx.annotation.Keep;

import core.bluetooth.BtState;
import core.device.model.MbtDevice;

@Keep
public interface ConnectionStateListener<U extends BaseError> extends BaseErrorEvent<U>{
        /**
         * Callback indicating the current state of the bluetooth communication
         * See {@link BtState} for all possible states
         */
        void onDeviceConnected(MbtDevice device);
        void onDeviceDisconnected(MbtDevice device);
    }