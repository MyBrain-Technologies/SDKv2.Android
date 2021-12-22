package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mybraintech.sdk.core.listener.ConnectionListener
import timber.log.Timber

class MbtBleBroadcastReceiver : BroadcastReceiver() {

    private var isRegistered = false
    var targetDevice: BluetoothDevice? = null
    var connectionListener: ConnectionListener? = null
    private var lastBondState: Int = -1

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
//                onActionStateChanged(intent)
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                onActionBondStateChanged(intent)
            }
        }
    }

    fun register(
        context: Context,
        bluetoothDevice: BluetoothDevice,
        connectionListener: ConnectionListener
    ) {
        if (isRegistered) {
            // new we can remove old register receiver.
            // Old register receiver is not removed on disconnection to keep listening to target device in case "First connection bonding"
            try {
                context.unregisterReceiver(this)
            } catch (e: Exception) {
                Timber.w(e)
            }
            isRegistered = false
        }
        setState(bluetoothDevice, connectionListener)
        context.registerReceiver(
            this,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        context.registerReceiver(
            this,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
        isRegistered = true
    }

    private fun onActionStateChanged(intent: Intent) {
        val bluetoothState = intent.getIntExtra(
            BluetoothAdapter.EXTRA_STATE,
            -1
        )
        Timber.d("ACTION_STATE_CHANGED : is state on = ${bluetoothState == BluetoothAdapter.STATE_ON}")
        when (bluetoothState) {
            BluetoothAdapter.STATE_ON -> {
            }
            BluetoothAdapter.STATE_OFF -> {
            }
        }
    }

    private fun onActionBondStateChanged(intent: Intent) {
        if (targetDevice == null) {
            Timber.d("ignore intent : target device is null")
            return
        }
        val intentDevice =
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        Timber.d("onActionBondStateChanged : ${intentDevice?.name}: ${intentDevice?.address}: ${intentDevice?.bondState}")
        if (targetDevice?.address != intentDevice?.address) {
            Timber.d("ignore intent : not target device")
            return
        }
        lastBondState = intentDevice?.bondState ?: -1
        when (lastBondState) {
            -1 -> {
                Timber.e("fatal error: bond state is not recognized")
            }
            BluetoothDevice.BOND_BONDED -> {
                connectionListener?.onBonded(targetDevice!!)
            }
            BluetoothDevice.BOND_BONDING -> {
                connectionListener?.onBondingRequired(targetDevice!!)
            }
            BluetoothDevice.BOND_NONE -> {
                connectionListener?.onBondingFailed(targetDevice!!)
            }
        }
    }

    private fun resetState() {
        targetDevice = null
        connectionListener = null
        lastBondState = -1
    }

    private fun setState(bluetoothDevice: BluetoothDevice, listener: ConnectionListener) {
        this.targetDevice = bluetoothDevice
        this.connectionListener = listener
        lastBondState = -1
    }
}