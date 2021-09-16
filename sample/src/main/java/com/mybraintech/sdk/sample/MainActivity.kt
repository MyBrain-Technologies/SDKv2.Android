package com.mybraintech.sdk.sample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.app.ActivityCompat
import com.mybraintech.sdk.sample.databinding.ActivityMainBinding
import config.ConnectionConfig
import config.StreamConfig
import core.bluetooth.StreamState
import core.device.model.MbtDevice
import core.eeg.storage.MbtEEGPacket
import engine.MbtClient
import engine.clientevents.BaseError
import engine.clientevents.ConnectionStateListener
import engine.clientevents.DeviceBatteryListener
import engine.clientevents.EegListener
import features.MbtDeviceType
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS: Int = 1
    lateinit var mbtClient: MbtClient
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSdk()
        initView()

        nextPageOrRequestPermission()

    }

    private fun nextPageOrRequestPermission() {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)

        if (!hasPermissions(this, permissions)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, REQUEST_CODE_PERMISSIONS)
            }
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            nextPageOrRequestPermission()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun initView() {
        binding.btnConnectReconnect.setOnClickListener {
            connectBle(100)
        }
        binding.btnDisconnect.setOnClickListener {
            disconnectBle(100)
        }
        binding.btnReadFirmwareVersion.setOnClickListener {
            readFirmwareVersion()
        }
        binding.btnReadBattery.setOnClickListener {
            readBatteryLevel()
        }
        binding.btnStartStreaming.setOnClickListener {
            startStreamingEeg()
        }
        binding.btnStartStreaming.setOnClickListener {

        }
    }

    private fun startStreamingEeg() {
        val eegListener = object : EegListener<BaseError> {
            override fun onError(error: BaseError?, additionalInfo: String?) {
                Timber.e(error)
            }

            override fun onNewPackets(eegPackets: MbtEEGPacket) {
                binding.txtEegPackage.text = eegPackets.toString()
            }

            override fun onNewStreamState(streamState: StreamState) {
                binding.txtStreamState.text = streamState.name
            }
        }
        val streamConfig = StreamConfig.Builder(eegListener)
            .createForDevice(MbtDeviceType.MELOMIND)
        mbtClient.startStream(streamConfig)
    }

    private fun readFirmwareVersion() {
        binding.txtFirmwareVersion.text = "not implemented yet"
    }

    private fun readBatteryLevel() {
        mbtClient.readBattery(object : DeviceBatteryListener<BaseError> {
            override fun onError(error: BaseError?, additionalInfo: String?) {
                Timber.e(error)
                binding.txtBatteryLevel.text = error?.message ?: "unknown error"
            }

            override fun onBatteryLevelReceived(level: String?) {
                runOnUiThread {  }
                binding.txtBatteryLevel.text = level ?: "null"
            }

        })
    }

    private fun initSdk() {
        mbtClient = MbtClient.init(this)
        connectBle()
    }

    private fun connectBle(delay: Long = 300) {
        clearText()

        val connectBleRunnable = Runnable {
            val config = ConnectionConfig.Builder(connectionStateListener)
                .createForDevice(MbtDeviceType.MELOMIND_Q_PLUS)

            if (!binding.edtDeviceName.text.isNullOrBlank()) {
                config.deviceName = binding.edtDeviceName.text.trim().toString()
                Timber.i("scan with name = ${config.deviceName}")
            } else {
                Timber.i("scan without name")
            }

            mbtClient.connectBluetooth(config)
        }

        Handler().postDelayed(connectBleRunnable, delay)
    }

    private fun disconnectBle(delay: Long = 300) {
        clearText()
        Handler().postDelayed(disconnectBleRunnable, delay)
    }

    private fun clearText() {
        binding.txtIsConnected.text = "..."
    }

    private val connectionStateListener = object
        : ConnectionStateListener<BaseError> {
        override fun onError(error: BaseError?, additionalInfo: String?) {
            Timber.e(error)
            binding.txtIsConnected.text = error?.message ?: "unknown error"

        }

        override fun onDeviceConnected(device: MbtDevice?) {
            Timber.i("onDeviceConnected")
            runOnUiThread {
                binding.txtIsConnected.text = "connected"
            }
        }

        override fun onDeviceDisconnected(device: MbtDevice?) {
            Timber.i("onDeviceDisconnected")
            runOnUiThread {
                binding.txtIsConnected.text = "disconnected"
            }
        }

    }

    private val disconnectBleRunnable = Runnable {
        val config = ConnectionConfig.Builder(connectionStateListener)
            .createForDevice(MbtDeviceType.MELOMIND_Q_PLUS)
        mbtClient.connectBluetooth(config)
    }
}