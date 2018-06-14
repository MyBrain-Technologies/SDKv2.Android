package core.device;

import android.support.annotation.NonNull;

public class RawDeviceMeasure {
    private byte[] rawMeasure;


    public RawDeviceMeasure(@NonNull byte[] rawMeasure){
        this.rawMeasure = rawMeasure;
    }

    public byte[] getRawMeasure() {
        return rawMeasure;
    }
}
