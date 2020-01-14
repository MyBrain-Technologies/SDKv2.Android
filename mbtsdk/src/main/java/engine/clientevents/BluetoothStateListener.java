package engine.clientevents;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import core.bluetooth.BtState;
import core.device.model.MbtDevice;

@Keep
public interface BluetoothStateListener extends ConnectionStateListener<BaseError> {

    void onNewState(BtState newState, @Nullable MbtDevice device);
}