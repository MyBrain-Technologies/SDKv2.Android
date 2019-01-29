package eventbus.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.bluetooth.BtState;
import engine.clientevents.BaseError;

public class NewConnectionStateEvent {
    private BtState newState;
    @Nullable private BaseError error;

    public NewConnectionStateEvent(@NonNull BtState newState,@Nullable BaseError error){
        this.newState = newState;
        this.error = error;
    }


    public BtState getNewState() {
        return newState;
    }

    @Nullable
    public BaseError getError() {
        return error;
    }
}
