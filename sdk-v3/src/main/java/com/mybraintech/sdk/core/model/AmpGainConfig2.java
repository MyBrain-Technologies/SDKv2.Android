package com.mybraintech.sdk.core.model;

import androidx.annotation.Keep;

@Keep
public enum AmpGainConfig2 {
    AMP_GAIN_X12_DEFAULT(10, 12),
    AMP_GAIN_X8_MEDIUM(20, 8),
    AMP_GAIN_X6_LOW(30, 6),
    AMP_GAIN_X4_VLOW(40, 4);

    private int numVal;

    private int gain;

    AmpGainConfig2(int numVal, int gain) {
        this.numVal = numVal;
        this.gain = gain;
    }

    public int getNumVal() {
        return numVal;
    }

    public static int getGainFromByteValue(byte numVal){
        int gain = 0;

        if (numVal == AmpGainConfig2.AMP_GAIN_X12_DEFAULT.numVal)
            gain = AmpGainConfig2.AMP_GAIN_X12_DEFAULT.gain;

        else if (numVal == AmpGainConfig2.AMP_GAIN_X8_MEDIUM.numVal)
            gain = AmpGainConfig2.AMP_GAIN_X8_MEDIUM.gain;

        else if (numVal == AmpGainConfig2.AMP_GAIN_X6_LOW.numVal)
            gain = AmpGainConfig2.AMP_GAIN_X6_LOW.gain;

        else if (numVal == AmpGainConfig2.AMP_GAIN_X4_VLOW.numVal)
            gain = AmpGainConfig2.AMP_GAIN_X4_VLOW.gain;

        return gain;
    }
}