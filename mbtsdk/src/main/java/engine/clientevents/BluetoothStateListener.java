package engine.clientevents;

import android.support.annotation.Keep;

import core.bluetooth.BtState;

@Keep
public interface BluetoothStateListener extends ConnectionStateListener<BaseError> {

    void onNewState(BtState newState);
}