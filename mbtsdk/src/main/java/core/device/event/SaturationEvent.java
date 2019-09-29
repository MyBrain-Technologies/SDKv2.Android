package core.device.event;

import android.support.annotation.Keep;

@Keep
public class SaturationEvent {

    private int saturationCode;

    public SaturationEvent(int saturationValue){
        this.saturationCode = saturationValue;
    }

    public int getSaturationCode() {
        return saturationCode;
    }
}
