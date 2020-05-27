package engine.clientevents;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import core.bluetooth.BluetoothState;
import core.device.model.MbtDevice;

@Keep
public interface BluetoothStateListener extends ConnectionStateListener<BaseError> {

    void onNewState(BluetoothState newState, @Nullable MbtDevice device);
}