package core.device;

public class SaturationEvent {

    private int saturationCode;

    public SaturationEvent(int saturationValue){
        this.saturationCode = saturationValue;
    }

    public int getSaturationCode() {
        return saturationCode;
    }
}
