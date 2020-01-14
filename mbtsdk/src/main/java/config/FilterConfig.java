package config;

import androidx.annotation.Keep;
@Keep
public enum FilterConfig {
    NOTCH_FILTER_50HZ(50),
    NOTCH_FILTER_60HZ(60),
    NOTCH_FILTER_DEFAULT(70);

    private int numVal;

    FilterConfig(int numVal) {
        this.numVal = numVal;
    }

    public int getNumVal() {
        return numVal;
    }
}



