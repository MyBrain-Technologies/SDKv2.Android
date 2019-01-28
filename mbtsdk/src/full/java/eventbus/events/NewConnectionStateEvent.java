package eventbus.events;

import android.support.annotation.NonNull;

import core.bluetooth.BtState;

public class NewConnectionStateEvent {
    private BtState newState;

    public NewConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }


    public BtState getNewState() {
        return newState;
    }
}
