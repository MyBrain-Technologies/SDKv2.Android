package com.mybraintech.sdk.core.model;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * stop using this class, it is renamed to {@code MbtEEGPacket}
 *
 * @see MbtEEGPacket
 */
@Keep
@Deprecated
public final class MbtEEGPacket2 extends MbtEEGPacket {

    public MbtEEGPacket2(MbtEEGPacket packetToClone) {
        super(packetToClone);
    }

    public MbtEEGPacket2(@NonNull ArrayList<ArrayList<Float>> channelsData, @Nullable ArrayList<Float> statusData) {
        super(channelsData, statusData);
    }
}
