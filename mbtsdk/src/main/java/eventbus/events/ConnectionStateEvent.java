package eventbus.events;

import android.support.annotation.NonNull;

import core.bluetooth.BtState;

public class ConnectionStateEvent {
    private BtState newState;
    private String additionalInfo;

    public ConnectionStateEvent(@NonNull BtState newState){
        this.newState = newState;
    }
    public ConnectionStateEvent(@NonNull BtState newState, String additionalInfo){
        this.newState = newState;
        this.additionalInfo = additionalInfo;
    }

    public BtState getNewState() {
        return newState;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
}
