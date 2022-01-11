package com.mybraintech.sdk.sample

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.MbtClientFactory
import com.mybraintech.sdk.core.acquisition.eeg.MbtEEGPacket2
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.DeviceInformation
import com.mybraintech.sdk.core.model.EEGParams
import com.mybraintech.sdk.core.model.EnumMBTDevice
import com.mybraintech.sdk.core.model.MbtDevice
import com.mybraintech.sdk.sample.databinding.ActivityQplusBinding
import com.mybraintech.sdk.util.toJson
import timber.log.Timber

class QplusActivity : AppCompatActivity(), ConnectionListener, BatteryLevelListener {

    private lateinit var binding: ActivityQplusBinding
    lateinit var mbtClient: MbtClient
    var mbtDevice: MbtDevice? = null
    val buffer = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQplusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mbtClient = MbtClientFactory.createMbtClient(applicationContext, EnumMBTDevice.Q_PLUS)

        initView()
    }

    private fun initView() {
        binding.btnCount.setOnClickListener {
            val bluetoothManager =
                this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val gattCount = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            buffer.appendLine("gatt btnCount = ${gattCount.size}")
            binding.txtStatus.text = buffer.toString()
        }

        binding.btnIsConnected.setOnClickListener {
            val bleConnectionStatus = mbtClient.getBleConnectionStatus()
            buffer.appendLine("getBleConnectionStatus = ${bleConnectionStatus.mbtDevice.toJson()} | ${bleConnectionStatus.isConnectionEstablished}")
            binding.txtStatus.text = buffer.toString()
        }

        binding.btnScan.setOnClickListener {
            addResultText("scanning...")
            mbtClient.startScan(object : ScanResultListener {
                override fun onMbtDevices(mbtDevices: List<MbtDevice>) {
                    Timber.i("onMbtDevices size = ${mbtDevices.size}")
                    addResultText("found devices")
                    for (device in mbtDevices) {
                        Timber.i("device ${device.bluetoothDevice.name}")
                    }
                    mbtClient.stopScan()
                    mbtDevice = mbtDevices[0]
                }

                override fun onOtherDevices(otherDevices: List<BluetoothDevice>) {
                    Timber.i("onOtherDevices size = ${otherDevices.size}")
                    for (device in otherDevices) {
                        if (device.name != null) {
                            Timber.d("onOtherDevices name = ${device.name}")
                        }
                    }
                }

                override fun onScanError(error: Throwable) {
                    Timber.e(error)
                    addResultText("onScanError")
                }

            })
        }

        binding.btnConnect.setOnClickListener {
            if (mbtDevice != null) {
                mbtClient.connect(mbtDevice!!, this)
            } else {
                Timber.e("please scan first")
                addResultText("please scan first")
            }
        }

        binding.btnDisconnect.setOnClickListener {
            mbtClient.disconnect()
        }

        binding.btnReadBattery.setOnClickListener {
            mbtClient.getBatteryLevel(object : BatteryLevelListener {
                override fun onBatteryLevel(float: Float) {
                    addResultText("battery $float percent")
                }

                override fun onBatteryLevelError(error: Throwable) {
                    addResultText("battery error : ${error.message}")
                }

            })
        }

        binding.btnReadDeviceInfos.setOnClickListener {
            mbtClient.getDeviceInformation(object : DeviceInformationListener {
                override fun onDeviceInformation(deviceInformation: DeviceInformation) {
                    addResultText(deviceInformation.toJson())
                }

                override fun onDeviceInformationError(error: Throwable) {
                    addResultText("onDeviceInformationError = ${error.message}")
                }

            })
        }

        binding.btnClearText.setOnClickListener {
            buffer.clear()
            binding.txtStatus.text = ""
        }

        binding.btnStartEeg.setOnClickListener {
            mbtClient.startEEG(
                object : EEGListener {
                    override fun onEegPacket(mbtEEGPacket2: MbtEEGPacket2) {
                        Timber.d("onEegPacket : ${mbtEEGPacket2.timeStamp}")
                    }

                    override fun onEegError(error: Throwable) {
                        Timber.e(error)
                    }
                },
                EEGParams(
                    sampleRate = 250,
                    isStatusEnabled = false,
                    isQualityCheckerEnabled = true)
            )
        }

        binding.btnStopEeg.setOnClickListener {
            mbtClient.stopEEG()
        }
    }

    fun addResultText(text: String) {
        buffer.appendLine(text)
        binding.txtStatus.text = buffer.toString()
    }

    override fun onServiceDiscovered() {
        addResultText("onServiceDiscovered")
    }

    override fun onBondingRequired(device: BluetoothDevice) {
        addResultText("onBondingRequired")
    }

    override fun onBonded(device: BluetoothDevice) {
        addResultText("onBonded")
    }

    override fun onBondingFailed(device: BluetoothDevice) {
        addResultText("onBondingFailed")
    }

    override fun onDeviceReady() {
        addResultText("onDeviceReady")
    }

    override fun onConnectionError(error: Throwable) {
        buffer.appendLine("onConnectionError = ${error.message}")
        binding.txtStatus.text = buffer.toString()
    }

    override fun onDeviceDisconnected() {
        addResultText("onDeviceDisconnected")
    }

    //----------------------------------------------------------------------------
    // MARK: battery
    //----------------------------------------------------------------------------
    override fun onBatteryLevel(float: Float) {
        buffer.appendLine("onBatteryLevel = $float")
        binding.txtStatus.text = buffer.toString()
    }

    override fun onBatteryLevelError(error: Throwable) {
        Timber.e(error)
    }


}