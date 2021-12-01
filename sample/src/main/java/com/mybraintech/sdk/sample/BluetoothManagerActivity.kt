package com.mybraintech.sdk.sample

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mybraintech.sdk.core.bluetooth.MbtBleManager
import com.mybraintech.sdk.core.listener.BatteryLevelListener
import com.mybraintech.sdk.core.listener.ConnectionListener
import com.mybraintech.sdk.sample.databinding.ActivityBluetoothManagerBinding
import indus5.MbtClientIndus5
import timber.log.Timber

class BluetoothManagerActivity : AppCompatActivity(), ConnectionListener, BatteryLevelListener {

    private lateinit var binding: ActivityBluetoothManagerBinding
    lateinit var mbtBleManager: MbtBleManager
    val buffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mbtBleManager = MbtBleManager(applicationContext)
        mbtBleManager.init(true)

        mbtBleManager.setConnectionListener(this)
        mbtBleManager.setBatteryLevelListener(this)

        initView()
    }

    private fun initView() {
        binding.btnCount.setOnClickListener {
            val bluetoothManager =
                this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btnCount = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            buffer.appendLine("btnCount = ${btnCount.size}")
            binding.txtStatus.text = buffer.toString()
        }

        binding.btnIsConnected.setOnClickListener {
            buffer.appendLine("hasConnectedDevice = ${mbtBleManager.hasConnectedDevice()}")
            binding.txtStatus.text = buffer.toString()
        }

        binding.btnConnect.setOnClickListener {
            mbtBleManager.connect()
        }

        binding.btnDisconnect.setOnClickListener {
            mbtBleManager.disconnect()
        }

        binding.btnReadBattery.setOnClickListener {
            mbtBleManager.getBatteryLevel()
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

    override fun onScanFailed(error: Throwable) {
        Timber.e(error)
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