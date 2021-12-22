package com.mybraintech.sdk.util;

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

}
