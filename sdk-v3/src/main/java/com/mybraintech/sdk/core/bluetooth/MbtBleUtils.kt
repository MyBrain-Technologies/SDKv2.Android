package com.mybraintech.sdk.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import timber.log.Timber

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

    fun isQPlus(device: BluetoothDevice): Boolean {
        val qPlusPrefix = "qp_"
        val name = device.name ?: ""
        return name.startsWith(qPlusPrefix)
    }

    fun isBonded(bluetoothDevice: BluetoothDevice): Boolean {
        return (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED)
    }
}