package features;

import android.support.annotation.Keep;

import core.bluetooth.BtProtocol;

/**
 * This enum contains all MBT devices that can be scanned by this SDK.
 * It contains 3 different types of device:
 * <p>- {@link #MELOMIND} means that a melomind device will be connected and that the bluetooth support will be
 * bluetooth low energy (BLE)</p>
 * <p>- {@link #VPRO} means that a vpro device will be connected and that the bluetooth support will be classic SPP bluetooth</p>
 *
 * the first discovered headset.</p>
 */
@Keep
public enum MbtDeviceType {
    /**
     *
     */
    MELOMIND(BtProtocol.BLUETOOTH_LE),

    /**
     *
     */
    VPRO(BtProtocol.BLUETOOTH_SPP);


    private BtProtocol protocol;

    MbtDeviceType(BtProtocol protocol) {
        this.protocol = protocol;
    }

    public BtProtocol getProtocol() {
        return protocol;
    }

    public boolean useLowEnergyProtocol(){
        return this.protocol.equals(BtProtocol.BLUETOOTH_LE);
    }
}
