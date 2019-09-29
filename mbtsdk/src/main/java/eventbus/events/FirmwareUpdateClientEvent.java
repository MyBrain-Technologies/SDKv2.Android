package eventbus.events;

import core.device.oad.OADState;
import engine.clientevents.BaseError;

/**
 * Event posted when the OAD firmware update state changes or when an error occurs during the update.
 *
 * @author Sophie Zecri on 24/07/2019
 */
public class FirmwareUpdateClientEvent {

    private final static int UNDEFINED = -1;

    private BaseError error;
    private String additionalInfo;
    private OADState oadState;
    private int oadProgress = UNDEFINED;

    public FirmwareUpdateClientEvent(BaseError error, String additionalInfo) {
        this.error = error;
        this.additionalInfo = additionalInfo;
    }

    public FirmwareUpdateClientEvent(OADState oadState) {
        this.oadState = oadState;
        this.oadProgress = oadState.convertToProgress();
    }

    public FirmwareUpdateClientEvent(int oadProgress) {
        this.oadProgress = oadProgress;
    }

    public BaseError getError() {
        return error;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
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
                "error=" + error +
                ", additionalInfo='" + additionalInfo + '\'' +
                ", oadState=" + oadState +
                ", oadProgress=" + oadProgress +
                '}';
    }
}
