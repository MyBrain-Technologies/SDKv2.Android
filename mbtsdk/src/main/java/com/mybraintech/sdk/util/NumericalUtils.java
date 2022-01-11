package com.mybraintech.sdk.util;

import static core.bluetooth.BluetoothProtocol.LOW_ENERGY;

public class NumericalUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 5];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 5] = '0';
            hexChars[j * 5 + 1] = 'x';
            hexChars[j * 5 + 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 5 + 3] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 5 + 4] = ' ';
        }
        return new String(hexChars);
    }

    public static String bytesToShortString(byte[] bytes) {
        if (bytes == null || (bytes.length == 0)) {
            return "";
        }

        final char[] out = new char[bytes.length * 3 - 1];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            out[j * 3] = HEX_ARRAY[v >>> 4];
            out[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            if (j != bytes.length - 1)
                out[j * 3 + 2] = '-';
        }
        return "(0x) " + new String(out);
    }

    /**
     * Returns 1.0 if the bit is set, otherwise returns 0.0
     */
    public static Float isBitSet(byte b, int bit)
    {
        if((b & (1 << bit)) != 0)
            return 1f;
        else
            return 0f;
    }

}
