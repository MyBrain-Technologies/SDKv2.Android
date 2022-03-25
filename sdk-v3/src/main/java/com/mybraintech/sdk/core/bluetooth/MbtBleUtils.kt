package com.mybraintech.sdk.core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import timber.log.Timber

object MbtBleUtils {
    fun getGattConnectedDevices(context: Context): List<BluetoothDevice> {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT) ?: emptyList()
        } else {
            Timber.e("Permission PERMISSION_GRANTED is not granted!")
            emptyList()
        }
    }

    fun getBondedDevices(context: Context): List<BluetoothDevice> {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter.bondedDevices.toList()
        } else {
            Timber.e("Permission PERMISSION_GRANTED is not granted!")
            emptyList()
        }
    }

    fun isQPlus(device: BluetoothDevice, context: Context): Boolean {
        val qPlusPrefix1 = "melo_2"
        val qPlusPrefix2 = "qp_"
        val name = getDeviceName(device, context)
        return (name.startsWith(qPlusPrefix1) || name.startsWith(qPlusPrefix2))
    }

    fun getDeviceName(device: BluetoothDevice?, context: Context): String {
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            device?.name ?: ""
        } else {
            Timber.e("Permission PERMISSION_GRANTED is not granted!")
            ""
        }
    }
}