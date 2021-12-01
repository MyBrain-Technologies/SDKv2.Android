package com.mybraintech.sdk.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mybraintech.sdk.core.bluetooth.BluetoothManager
import com.mybraintech.sdk.core.bluetooth.central.MBTScanOption
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.sample.databinding.ActivityBluetoothManagerBinding
import timber.log.Timber
import java.lang.StringBuilder

class BluetoothManagerActivity : AppCompatActivity(), ConnectionListener, BatteryLevelListener {

    private lateinit var binding: ActivityBluetoothManagerBinding
    lateinit var bluetoothManager: BluetoothManager
    val buffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = BluetoothManager(applicationContext)
        bluetoothManager.setConnectionListener(this)
        bluetoothManager.setBatteryLevelListener(this)

        initView()
    }

    private fun initView() {
        binding.btnConnect.setOnClickListener {
            bluetoothManager.connect(
                scanOption = MBTScanOption(
                    name = null,
                    isIndus5 = true
                )
            )
        }

        binding.btnDisconnect.setOnClickListener {
            bluetoothManager.disconnect()
        }

        binding.btnReadBattery.setOnClickListener {
            bluetoothManager.getBatteryLevel()
        }
    }

    //----------------------------------------------------------------------------
    // MARK: bluetooth manager listeners
    //----------------------------------------------------------------------------
    override fun onDeviceConnectionStateChanged(isConnected: Boolean) {
        buffer.appendLine("isConnected = $isConnected")
        binding.txtStatus.text = buffer.toString()
    }

    override fun onDeviceBondStateChanged(isBonded: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onDeviceReady() {
        TODO("Not yet implemented")
    }

    override fun onConnectionError(error: Throwable) {
        TODO("Not yet implemented")
    }

    override fun onScanFailed(errorCode: Int) {
        TODO("Not yet implemented")
    }

    //----------------------------------------------------------------------------
    // MARK: battery
    //----------------------------------------------------------------------------
    override fun onBatteryLevel(float: Float) {
        buffer.appendLine("onBatteryLevel = $float")
        binding.txtStatus.text = buffer.toString()
    }

    override fun onBatteryLevelError(error: Throwable) {
        TODO("Not yet implemented")
    }


}