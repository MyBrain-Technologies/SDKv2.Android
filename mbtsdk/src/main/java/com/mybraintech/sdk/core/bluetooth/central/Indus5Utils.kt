package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.support.v18.scanner.ScanResult

object Indus5Utils {
    fun isIndus5(device: BluetoothDevice): Boolean {
        val INDUS5_PREFIX_1 = "melo_2"
        val INDUS5_PREFIX_2 = "qp_"
        val name = device.name
        return if (name == null) {
            false
        } else {
            (name.startsWith(INDUS5_PREFIX_1) || (name.startsWith(INDUS5_PREFIX_2)))
        }
    }

}