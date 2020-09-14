package features;

import androidx.annotation.Keep;

import core.bluetooth.BluetoothProtocol;

/**
 * This enum contains all MBT devices that can be scanned by this SDK.
 * It contains 2 different types of device:
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
    MELOMIND(BluetoothProtocol.LOW_ENERGY),

    /**
     *
     */
    VPRO(BluetoothProtocol.SPP);


    private BluetoothProtocol protocol;

    MbtDeviceType(BluetoothProtocol protocol) {
        this.protocol = protocol;
    }

    public BluetoothProtocol getProtocol() {
        return protocol;
    }

    public boolean useLowEnergyProtocol(){
        return this.protocol.equals(BluetoothProtocol.LOW_ENERGY);
    }
}
