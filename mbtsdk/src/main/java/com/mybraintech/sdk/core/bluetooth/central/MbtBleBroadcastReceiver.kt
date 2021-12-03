package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class MbtBleBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val bluetoothState = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    -1
                )
                Timber.d("ACTION_STATE_CHANGED : is state on = ${bluetoothState == BluetoothAdapter.STATE_ON}")
//                    when (bluetoothState) {
//                        BluetoothAdapter.STATE_ON -> {
//                        }
//                        BluetoothAdapter.STATE_OFF -> {
//                        }
//                    }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Timber.d("ACTION_BOND_STATE_CHANGED : ${device?.name}: ${device?.address}: ${device?.bondState}")
                when (device?.bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        Timber.i("BOND_BONDED")
//                            if (MbtBleUtils.isIndus5(device)) {
//                                Timber.i("new indus5 device is bonded, start connection")
//                                enqueueConnect(device)
//                            }
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Timber.i("BOND_BONDING")
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Timber.i("BOND_NONE")
                    }
                }
            }
        }
    }
}