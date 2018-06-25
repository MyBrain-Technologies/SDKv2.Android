package features;

import android.support.annotation.Keep;

/**
 * This enum contains all MBT devices that can be scanned by this SDK.
 * It contains 3 different types of device:
 * <p>- {@link #MELOMIND} means that a melomind device will be connected and that the bluetooth support will be
 * bluetooth low energy (BLE)</p>
 * <p>- {@link #VPRO} means that a vpro device will be connected and that the bluetooth support will be classic SPP bluetooth</p>
 *
 * <p>By default, {@link #ALL} is selected. It means you can connect to any of these devices. The scanner will be classic, and the bluetooth protocol will automatically depends on
 * the first discovered headset.</p>
 */
@Keep
public enum ScannableDevices{
    /**
     *
     */
    MELOMIND,

    /**
     *
     */
    VPRO,

    /**
     *
     */
    ALL
}
