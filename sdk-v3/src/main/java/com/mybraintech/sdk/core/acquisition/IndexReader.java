package com.mybraintech.sdk.core.acquisition;

/**
 * allow to decode EEG, IMS index from raw BLE frame
 */
public class IndexReader {
    static public Long decodeIndex(byte[] bleFrame) {
        return (long) ((bleFrame[0] & 0xff) << 8 | (bleFrame[1] & 0xff));
    }
}
