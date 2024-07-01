package com.mybraintech.sdk.core.bluetooth.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mybraintech.sdk.core.bluetooth.MbtAudioDeviceInterface
import com.mybraintech.sdk.core.listener.ConnectionListener
import timber.log.Timber

class MbtBleBroadcastReceiver : BroadcastReceiver() {

    private var isRegistered = false
    var targetDevice: BluetoothDevice? = null
    var targetAudioDevice:String = ""
    var connectionListener: ConnectionListener? = null
    var audioConnectionListener: MbtAudioDeviceInterface? = null
    private var lastBondState: Int = -1


    override fun onReceive(context: Context?, intent: Intent?) {

        val action = intent?.action
        Timber.d("MbtBleBroadcastReceiver onReceive action:$action")

        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
//                onActionStateChanged(intent)
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                onActionBondStateChanged(intent)
            }

            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Timber.d("MbtBleBroadcastReceiver onReceive ===========================================")
                    Timber.d("MbtBleBroadcastReceiver onReceive receiver bluetooth name:${it.name}")
                    Timber.d("MbtBleBroadcastReceiver onReceive receiver bluetooth targetAudioDevice:${targetAudioDevice}")
                    val bondState = it.bondState
                    Timber.d("MbtBleBroadcastReceiver onReceive receiver bluetooth bond stats:${it.bondState}")
                    // Optionally, add logic to connect to a specific device
                    //scan the list of bluetooth devices. if the name is the same Audio name of connected BLE headset. request pair
                    if (it.name == targetAudioDevice) {
                        audioConnectionListener?.onMbtAudioDeviceFound(it, action?:"", bondState)

                    }
                }
            }
        }
    }
    fun registerAudioDevice(
        listner: MbtAudioDeviceInterface
    ) {
        audioConnectionListener = listner
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

        context.registerReceiver(
            this,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        val actionConnectedFilter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        val actionBondFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(this, actionConnectedFilter)
        context.registerReceiver(this, actionBondFilter)


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
        Timber.d("onActionBondStateChanged targetDevice:$targetDevice")
        if (targetDevice == null) {
            Timber.d("ignore intent : target device is null")
            return
        }

        val intentDevice =
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        Timber.d("onActionBondStateChanged : ${intentDevice?.name}: ${intentDevice?.address}: ${intentDevice?.bondState}")
        val currentState = intentDevice?.bondState ?: -1
        Timber.d("currentState:$currentState")
        val bondChangedDeviceName = intentDevice?.name
        if (bondChangedDeviceName == targetAudioDevice) {
            if (currentState == BluetoothDevice.BOND_BONDED) {
                Timber.d("onDeviceReady audio call from onActionBondStateChanged")
                connectionListener?.onDeviceReady("audio connected")
            }
        }
        if (targetDevice?.address != intentDevice?.address) {
            Timber.d("ignore intent : not target device")
            return
        }
        when (currentState) {
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
                if (lastBondState == BluetoothDevice.BOND_BONDING) {
                    connectionListener?.onBondingFailed(targetDevice!!)
                }
            }
        }
        lastBondState = currentState
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