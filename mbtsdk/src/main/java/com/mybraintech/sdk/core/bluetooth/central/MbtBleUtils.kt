package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context

object MbtBleUtils {
    fun getGattConnectedDevices(context: Context): List<BluetoothDevice> {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT) ?: emptyList()
    }

    fun getBondedDevices(context: Context): List<BluetoothDevice> {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter.bondedDevices.toList()
    }

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