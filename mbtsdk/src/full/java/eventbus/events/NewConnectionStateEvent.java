package eventbus.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import core.bluetooth.BtState;
import engine.clientevents.BaseError;

public class NewConnectionStateEvent {
    private BtState newState;
    private String additionnalInfo;

    public NewConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }
    public NewConnectionStateEvent(@NonNull BtState newState, String additionnalInfo){
        this.newState = newState;
        this.additionnalInfo = additionnalInfo;
    }

    public BtState getNewState() {
        return newState;
    }

    public String getAdditionnalInfo() {
        return additionnalInfo;
    }
}
