package com.mybraintech.sdk.core

import android.content.Context
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.core.acquisition.IMSAcquisier
import com.mybraintech.sdk.core.acquisition.eeg.EEGAcquisier
import com.mybraintech.sdk.core.acquisition.eeg.SignalProcessingManager
import com.mybraintech.sdk.core.bluetooth.IMbtBleManager
import com.mybraintech.sdk.core.bluetooth.central.Indus5BleManager
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.core.listener.ScanResultListener
import com.mybraintech.sdk.core.model.BleConnectionStatus
import com.mybraintech.sdk.core.model.MbtDevice

/**
 * DO NOT USE THIS CLASS OUTSIDE OF THE SDK
 * MbtClientV2 is new class to support Q+ device, Melomind device...
 */
class MbtClientV2(private val context: Context) : MbtClient {

    private val mbtBleManager : IMbtBleManager = Indus5BleManager(context)
    private var eegAcquisier: EEGAcquisier? = null
    private var imsAcquisier: IMSAcquisier? = null
    private var signalProcessingManager: SignalProcessingManager? = null

    override fun getBleConnectionStatus(): BleConnectionStatus {
        return mbtBleManager.getBleConnectionStatus()
    }

    override fun startScan(scanResultListener: ScanResultListener) {
        mbtBleManager.startScan(scanResultListener)
    }

    override fun stopScan() {
        mbtBleManager.stopScan()
    }

    override fun connect(mbtDevice: MbtDevice, connectionListener: ConnectionListener) {
        mbtBleManager.connectMbt(mbtDevice, connectionListener)
    }

    override fun disconnect() {
        mbtBleManager.disconnectMbt()
    }
}