package com.mybraintech.sdk.core.acquisition.eeg;

import static core.bluetooth.BluetoothProtocol.LOW_ENERGY;

public class Indus5Utils {
    static public Long getIndex(byte[] eegFrame) {
        return (long) ((eegFrame[0] & 0xff) << 8 | (eegFrame[1] & 0xff));
    }
}
