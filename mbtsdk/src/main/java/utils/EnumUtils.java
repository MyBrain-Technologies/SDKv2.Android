package utils;

import core.bluetooth.BtState;
import core.device.model.DeviceInfo;
import engine.clientevents.BaseError;

public class EnumUtils {

    public final static Integer NONE_ORDER_ENUM = -1;
    public final static Integer LAST_ORDER_BLUETOOTH_STATE = BtState.CONNECTED_AND_READY.ordinal();



    /**
     * @param currentState getCurrentState should be passed in parameter
     * @return the step that follow (in chronological order, based on the enum value) the step given in parameter
     */
    public static BtState getNextConnectionStep(BtState currentState){
        BtState nextState = null;
        for (BtState state : BtState.values()) {
            if(!state.getOrder().equals(LAST_ORDER_BLUETOOTH_STATE) && !state.getOrder().equals(EnumUtils.NONE_ORDER_ENUM) && state.getOrder().equals(currentState.getOrder()))
                nextState = state;
        }
        return nextState;
    }
}
