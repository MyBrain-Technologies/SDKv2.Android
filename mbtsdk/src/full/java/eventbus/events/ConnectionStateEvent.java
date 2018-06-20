package eventbus.events;

import android.support.annotation.NonNull;

import core.bluetooth.BtState;

public class ConnectionStateEvent {
    private BtState newState;

    public ConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }


    public BtState getNewState() {
        return newState;
    }
}
