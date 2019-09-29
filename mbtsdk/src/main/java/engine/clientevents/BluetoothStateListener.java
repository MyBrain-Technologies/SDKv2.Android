package engine.clientevents;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import core.bluetooth.BtState;
import core.device.model.MbtDevice;

@Keep
public interface BluetoothStateListener extends ConnectionStateListener<BaseError> {

    void onNewState(BtState newState, @Nullable MbtDevice device);
}