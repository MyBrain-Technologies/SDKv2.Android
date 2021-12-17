package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import no.nordicsemi.android.ble.observer.BondingObserver
import timber.log.Timber

class MbtBleBroadcastReceiver : BroadcastReceiver() {

    private var lastBondState: Int = BluetoothDevice.BOND_NONE
    private var lastBluetoothDevice: BluetoothDevice? = null

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
                lastBluetoothDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Timber.d("ACTION_BOND_STATE_CHANGED : ${lastBluetoothDevice?.name}: ${lastBluetoothDevice?.address}: ${lastBluetoothDevice?.bondState}")
                lastBondState = lastBluetoothDevice?.bondState ?: BluetoothDevice.BOND_NONE
                when (lastBondState) {
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

    fun isBonding(bluetoothDevice: BluetoothDevice): Boolean {
        return (lastBluetoothDevice == bluetoothDevice) && (lastBondState == BluetoothDevice.BOND_BONDING)
    }
}