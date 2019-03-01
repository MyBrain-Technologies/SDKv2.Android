package engine.clientevents;

import core.bluetooth.BtState;

public interface BluetoothStateListener extends ConnectionStateListener<BaseError> {

    void onNewState(BtState newState);
}