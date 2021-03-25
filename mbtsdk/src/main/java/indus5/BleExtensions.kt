package indus5

import android.bluetooth.BluetoothGattDescriptor
import android.media.audiofx.AudioEffect
import java.util.*

fun Int.translateBluetoothGattState() : String {
    return when (this) {
        0 -> "STATE_DISCONNECTED"
        1 -> "STATE_CONNECTING"
        2 -> "STATE_CONNECTED"
        3 -> "STATE_DISCONNECTING"
        else -> "NULL"
    }
}