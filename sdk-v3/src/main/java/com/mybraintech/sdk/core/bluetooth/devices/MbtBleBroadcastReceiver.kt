package com.mybraintech.sdk.core.bluetooth.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mybraintech.sdk.core.listener.ConnectionListener
import timber.log.Timber

class MbtBleBroadcastReceiver : BroadcastReceiver() {

    private var isWaitingConsent: Boolean = false
    private var isBroadcastRegistered = false
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
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                onActionPairingRequest(intent)
            }
        }
    }

    fun register(
        context: Context,
        bluetoothDevice: BluetoothDevice,
        connectionListener: ConnectionListener
    ) {
        if (isBroadcastRegistered) {
            // new we can remove old register receiver.
            // Old register receiver is not removed on disconnection to keep listening to target device in case "First connection bonding"
            try {
                context.unregisterReceiver(this)
            } catch (e: Exception) {
                Timber.w(e)
            }
            isBroadcastRegistered = false
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
            IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
        )
        isBroadcastRegistered = true
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
        val currentState = intentDevice?.bondState ?: -1
        when (currentState) {
            -1 -> {
                Timber.e("fatal error: bond state is not recognized")
            }
            BluetoothDevice.BOND_BONDED -> {
                isWaitingConsent = false
                Timber.d("isWaitingConsent = $isWaitingConsent")
                connectionListener?.onBonded()
            }
            BluetoothDevice.BOND_BONDING -> {
                connectionListener?.onBondingRequired()
            }
            BluetoothDevice.BOND_NONE -> {
                if (lastBondState == BluetoothDevice.BOND_BONDING) {
                    connectionListener?.onBondingFailed()
                }
            }
        }
        lastBondState = currentState
    }

    private fun onActionPairingRequest(intent: Intent) {
        Timber.d("onActionPairingRequest")
        val intentDevice =
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        if (targetDevice?.address != intentDevice?.address) {
            Timber.d("ignore intent : not target device")
            return
        } else {
            val pairingCode =
                intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
            if (pairingCode == PAIRING_VARIANT_CONSENT) {
                isWaitingConsent = true
                connectionListener?.onParingRequest()
                Timber.d("isWaitingConsent = $isWaitingConsent")
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

    companion object {

        /**
         * we received this intent code when a new pairing request popup is showing on the Android device. User will accept this request to allow the bonding.
         * @see <a href="https://developer.android.com/reference/com/google/android/things/bluetooth/PairingParams#pairing_variant_consent">Android PairingParams</a>
         */
        private const val PAIRING_VARIANT_CONSENT = 3
    }
}