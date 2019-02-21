package eventbus.events;

import android.support.annotation.NonNull;

import core.bluetooth.BtState;

public class ConnectionStateEvent {
    private BtState newState;
    private String additionnalInfo;

    public ConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }
    public ConnectionStateEvent(@NonNull BtState newState, String additionnalInfo){
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
