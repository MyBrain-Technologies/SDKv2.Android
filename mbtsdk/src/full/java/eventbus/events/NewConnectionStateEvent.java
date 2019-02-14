package eventbus.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.bluetooth.BtState;
import engine.clientevents.BaseError;

public class NewConnectionStateEvent {
    private BtState newState;

    public NewConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }


    public BtState getNewState() {
        return newState;
    }

}
