package com.mybraintech.sdk.core.bluetooth;

public class DataConversionUtils {

    public static short getBatteryPercentageFromByteValue(byte value) {
        final short level;
        switch (value) {
            case (byte) 0:
                level = 0;
                break;
            case (byte) 1:
                level = 15;
                break;
            case (byte) 2:
                level = 30;
                break;
            case (byte) 3:
                level = 50;
                break;
            case (byte) 4:
                level = 65;
                break;
            case (byte) 5:
                level = 85;
                break;
            case (byte) 6:
                level = 100;
                break;
            case (byte) 0xFF:
            default:
                level = -1;
                break;
        }
        return level;
    }

}
