package com.mybraintech.sdk.core.bluetooth.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mybraintech.sdk.core.bluetooth.MbtAudioDeviceInterface
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.util.BLE_CONNECTED_STATUS
import timber.log.Timber

class MbtBleBroadcastReceiver (val audioAdapter:BluetoothAdapter?): BroadcastReceiver() {

    private var isRegistered = false
    private var broadCastSetup = false
    var bleTargetDevice: BluetoothDevice? = null
    var targetAudioAddress:String = ""
    var connectionListener: ConnectionListener? = null
    var audioConnectionListener: MbtAudioDeviceInterface? = null
    private var lastBondState: Int = -1
    private val TAG = "MbtBleBroadcastReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        Timber.d("$TAG Dev_debug onReceive action:$action")

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
                    val currentDeviceName = it.name?:""
                    val currentDeviceAddress = it.address?:""
                    val currentUUID = it.uuids?:""
                    val bondState = it.bondState

                    Timber.d("$TAG Dev_debug onReceive receiver bluetooth name:${currentDeviceName} mac:${currentDeviceAddress} uuids:${currentUUID} bondState:${bondState}")
                    if (currentDeviceName.isNotEmpty()) {
                        audioConnectionListener?.onMbtAudioDeviceFound(
                            it,
                            action ?: "",
                            bondState
                        )
                    } else {
//                        Timber.d("$TAG Dev_debug onReceive receiver device is empty ")
//                        val emptyDevice = audioAdapter?.getRemoteDevice(currentDeviceAddress)
//                        Timber.d("$TAG Dev_debug onReceive receiver device is emptyDevice:${emptyDevice} emptyDeviceName:${emptyDevice?.name}")
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
    fun setupBluetoothBroadcast(context: Context) {
        if (!broadCastSetup) {

            Timber.d("$TAG Dev_debug setupBluetoothBroadcast do the setup")
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

            context.registerReceiver(
                this,
                IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
            )


            broadCastSetup = true
        } else {
            Timber.d("$TAG Dev_debug setupBluetoothBroadcast already setup")

        }
    }
    fun register(
        context: Context,
        bluetoothDevice: BluetoothDevice,
        connectionListener: ConnectionListener
    ) {
        Timber.d("$TAG Dev_debug register connectionListener= ${connectionListener} isRegistered:$isRegistered")
        if (isRegistered) {
            // new we can remove old register receiver.
//             Old register receiver is not removed on disconnection to keep listening to target device in case "First connection bonding"
            try {
                context.unregisterReceiver(this)
            } catch (e: Exception) {
                Timber.w(e)
            }
            isRegistered = false
            broadCastSetup = false
        } else {
            setState(bluetoothDevice, connectionListener)
            setupBluetoothBroadcast(context)


            isRegistered = true
        }
    }

    private fun onActionStateChanged(intent: Intent) {
        val bluetoothState = intent.getIntExtra(
            BluetoothAdapter.EXTRA_STATE,
            -1
        )
        Timber.d("$TAG ACTION_STATE_CHANGED : is state on = ${bluetoothState == BluetoothAdapter.STATE_ON}")
        when (bluetoothState) {
            BluetoothAdapter.STATE_ON -> {
            }
            BluetoothAdapter.STATE_OFF -> {
            }
        }
    }

    private fun onActionBondStateChanged(intent: Intent) {
        Timber.d("$TAG Dev_debug onActionBondStateChanged bleTargetDevice:${bleTargetDevice?.name} lastBondState:$lastBondState")
        if (bleTargetDevice == null) {
            Timber.d("$TAG Dev_debug onActionBondStateChanged ignore intent : target device is null")
            return
        }

        val intentDevice =
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val currentDeviceName = intentDevice?.name
        val currentDeviceAddress = intentDevice?.address
        val bleTargetDeviceAddress = bleTargetDevice?.address
        val currentState = intentDevice?.bondState ?: -1
        Timber.d("$TAG Dev_debug onActionBondStateChanged currentState:$currentState currentDeviceAddress:$currentDeviceAddress bleTargetDeviceAddress:$bleTargetDeviceAddress currentDeviceName:$currentDeviceName")

        if (bleTargetDeviceAddress != intentDevice?.address) {
            Timber.d("$TAG Dev_debug onActionBondStateChanged ignore intent : not target device")
            return
        }
        when (currentState) {
            -1 -> {
                Timber.e("$TAG Dev_debug onActionBondStateChanged fatal error: bond state is not recognized")
            }
            BluetoothDevice.BOND_BONDED -> {
                Timber.i("$TAG Dev_debug onActionBondStateChanged called BLE_CONNECTED_STATUS")
                connectionListener?.onDeviceReady(BLE_CONNECTED_STATUS)
                connectionListener?.onBonded(bleTargetDevice!!)
            }
            BluetoothDevice.BOND_BONDING -> {
                connectionListener?.onBondingRequired(bleTargetDevice!!)
            }
            BluetoothDevice.BOND_NONE -> {
                if (lastBondState == BluetoothDevice.BOND_BONDING) {
                    connectionListener?.onBondingFailed(bleTargetDevice!!)
                }
            }
        }
        lastBondState = currentState
    }

    private fun resetState() {
        bleTargetDevice = null
        connectionListener = null
        lastBondState = -1
    }

    private fun setState(bluetoothDevice: BluetoothDevice, listener: ConnectionListener) {
        Timber.d("$TAG Dev_debug setState bluetoothDevice:$bluetoothDevice listener:$listener")
        this.bleTargetDevice = bluetoothDevice
        this.connectionListener = listener
        lastBondState = -1
    }
}