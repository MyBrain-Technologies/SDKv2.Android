package core.bluetooth.lowenergy;

import android.support.annotation.Keep;

@Keep
public enum AmpGainConfig {
    AMP_GAIN_X12_DEFAULT(10),
    AMP_GAIN_X8_MEDIUM(20),
    AMP_GAIN_X6_LOW(30),
    AMP_GAIN_X4_VLOW(40);

    private int numVal;

    AmpGainConfig(int numVal) {
        this.numVal = numVal;
    }

    public int getNumVal() {
        return numVal;
    }
}