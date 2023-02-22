package com.mybraintech.sdk.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context

object MbtBleUtils {

    private val firstGenerationHyperions = listOf(
        "qp_2220100001",
        "qp_2220100002",
        "qp_2220100003"
    )

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
        val isIndus5 = isIndus5(device)
        val isNotHyperion = !isHyperion(device)
        return (isIndus5 && isNotHyperion)
    }

    private fun isIndus5(device: BluetoothDevice): Boolean {
        val indus5Prefix = "qp_"
        val name = device.name ?: ""
        return name.startsWith(indus5Prefix)
    }

    fun isHyperion(device: BluetoothDevice): Boolean {
        val isFirstGenerationHyperion = firstGenerationHyperions.contains(device.name)
        val hasHyperionNamePattern = device.name?.startsWith("qp_9999") ?: false
        return (isFirstGenerationHyperion || hasHyperionNamePattern)
    }

    fun isBonded(bluetoothDevice: BluetoothDevice): Boolean {
        return (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED)
    }
}