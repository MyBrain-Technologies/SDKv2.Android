package eventbus.events;

import android.util.Pair;

import core.device.oad.OADState;
import engine.clientevents.BaseError;

/**
 * Event posted when the OAD firmware update state changes or when an error occurs during the update.
 *
 * @author Sophie Zecri on 24/07/2019
 */
public class FirmwareUpdateClientEvent {

    private final static int UNDEFINED = -1;

    private Pair<BaseError, String> errorAndAdditionalInfo;
    private OADState oadState;
    private int oadProgress = UNDEFINED;

    public FirmwareUpdateClientEvent(BaseError error, String additionalInfo) {
        this.errorAndAdditionalInfo = new Pair<>(error, additionalInfo);
    }

    public FirmwareUpdateClientEvent(OADState oadState) {
        this.oadState = oadState;
        this.oadProgress = oadState.convertToProgress();
    }

    public FirmwareUpdateClientEvent(int oadProgress) {
        this.oadProgress = oadProgress;
    }

    public BaseError getError() {
        return errorAndAdditionalInfo != null ? errorAndAdditionalInfo.first : null;
    }

    public String getAdditionalInfo() {
        return errorAndAdditionalInfo != null ? errorAndAdditionalInfo.second : null;
    }

    public OADState getOadState() {
        return oadState;
    }

    public int getOadProgress() {
        return oadProgress;
    }

    @Override
    public String toString() {
        return "FirmwareUpdateClientEvent{" +
                "errorAndAdditionalInfo=" + errorAndAdditionalInfo +
                ", oadState=" + oadState +
                ", oadProgress=" + oadProgress +
                '}';
    }
}
