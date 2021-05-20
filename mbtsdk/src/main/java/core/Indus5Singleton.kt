package core

import android.bluetooth.BluetoothDevice
import core.device.model.MbtDevice
import core.device.model.MelomindQPlusDevice
import features.MbtDeviceType

/**
 * TODO: remove this later, this class is used to quick develop function of Q+ indus5
 */
object Indus5Singleton {

    @JvmStatic
    var mbtDevice: MbtDevice = MelomindQPlusDevice("unset", "unset")

    private var isMelomindIndus5 = false

    fun setMelomindIndus5(isIndus5 : Boolean) {
        isMelomindIndus5 = isIndus5
    }

    fun isIndus5(): Boolean {
        return isMelomindIndus5
    }
}