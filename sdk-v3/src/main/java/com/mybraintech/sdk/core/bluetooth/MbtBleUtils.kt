package com.mybraintech.sdk.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.mybraintech.sdk.R

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

    fun isQPlus(context: Context, device: BluetoothDevice): Boolean {
        val isIndus5 = isIndus5(device)
        val isNotHyperion = !isHyperion(context, device)
        return (isIndus5 && isNotHyperion)
    }

    private fun isIndus5(device: BluetoothDevice): Boolean {
        val indus5Prefix = "qp_"
        val name = device.name ?: ""
        return name.startsWith(indus5Prefix)
    }

    fun isHyperion(context: Context, device: BluetoothDevice): Boolean {
        val hyperions = context.resources.getStringArray(R.array.hyperion_devices)
        return hyperions.contains(device.name)
    }

    fun isBonded(bluetoothDevice: BluetoothDevice): Boolean {
        return (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED)
    }
}