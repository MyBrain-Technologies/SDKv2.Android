package engine.clientevents;

import androidx.annotation.Keep;

import core.bluetooth.BluetoothState;
import core.device.model.MbtDevice;

@Keep
public interface ConnectionStateListener<U extends BaseError> extends BaseErrorEvent<U>{
        /**
         * Callback indicating the current state of the bluetooth communication
         * See {@link BluetoothState} for all possible states
         */
        void onDeviceConnected(MbtDevice device);
        void onDeviceDisconnected(MbtDevice device);
    }