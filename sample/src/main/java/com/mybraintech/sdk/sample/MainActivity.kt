package com.mybraintech.sdk.sample

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.mybraintech.sdk.MbtClient
import com.mybraintech.sdk.MbtClientManager
import com.mybraintech.sdk.core.bluetooth.MbtBleUtils
import com.mybraintech.sdk.core.listener.*
import com.mybraintech.sdk.core.model.*
import com.mybraintech.sdk.sample.databinding.ActivityQplusBinding
import com.mybraintech.sdk.util.toJson
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), ConnectionListener, BatteryLevelListener {

    private lateinit var binding: ActivityQplusBinding
    lateinit var mbtClient: MbtClient
    var mbtDevice: MbtDevice? = null
    var deviceInformation: DeviceInformation? = null
    private val sb = StringBuilder()

    var eegCount = 0
    var imsCount = 0

    companion object {
        val DEVICE_TYPE_KEY = "DEVICE_TYPE"
        val MELOMIND_DEVICE = "MELOMIND"
        val Q_PLUS_DEVICE = "Q_PLUS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQplusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra(DEVICE_TYPE_KEY)
        mbtClient = if (type == MELOMIND_DEVICE) {
            MbtClientManager.getMbtClient(applicationContext, EnumMBTDevice.MELOMIND)
        } else {
            MbtClientManager.getMbtClient(applicationContext, EnumMBTDevice.Q_PLUS)
        }

        supportActionBar?.title = type

        initView()
    }

    @SuppressLint("MissingPermission")
    private fun initView() {
        binding.btnIsConnected.setOnClickListener {
            val bleConnectionStatus = mbtClient.getBleConnectionStatus()
            addResultText("getBleConnectionStatus = ${bleConnectionStatus.mbtDevice.toJson()} | ${bleConnectionStatus.isConnectionEstablished}")
        }

        binding.btnScan.setOnClickListener {
            addResultText("scanning...")
            mbtClient.startScan(object : ScanResultListener {
                override fun onMbtDevices(mbtDevices: List<MbtDevice>) {
                    Timber.i("onMbtDevices size = ${mbtDevices.size}")
                    for (device in mbtDevices) {
                        Timber.i("device ${device.bluetoothDevice.name}")
                    }
                    mbtClient.stopScan()
                    mbtDevice = mbtDevices[0]
                    addResultText("found devices ${mbtDevice?.bluetoothDevice?.name}")
                    addResultText("stop scan")
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

        binding.btnStopScan.setOnClickListener {
            mbtClient.stopScan()
            addResultText("stop scan")
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

        binding.btnDeBond.setOnClickListener {
            mbtDevice?.let { nnDevice ->
                MbtBleUtils.getBondedDevices(applicationContext).find {
                    nnDevice.bluetoothDevice.address == it.address
                }?.let { bonded ->
                    try {
                        bonded::class.java.getMethod("removeBond").invoke(bonded)
                        addResultText("debonded!!")
                    } catch (e: Exception) {
                        Timber.e("Removing bond has been failed. ${e.message}")
                    }
                }
            }
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
                    this@MainActivity.deviceInformation = deviceInformation
                    addResultText(deviceInformation.toJson())
                }

                override fun onDeviceInformationError(error: Throwable) {
                    addResultText("onDeviceInformationError = ${error.message}")
                }

            })
        }

        binding.btnClearText.setOnClickListener {
            sb.clear()
            binding.txtStatus.text = ""
        }

        binding.btnStartStreaming.setOnClickListener {
            onBtnStartEEGClicked()
        }

        binding.btnStopStreaming.setOnClickListener {
            mbtClient.stopStreaming()
            eegCount = 0
            imsCount = 0
        }

        binding.btnStartRecording.setOnClickListener {
            onBtnStartRecordingClicked()
        }

        binding.btnStopRecording.setOnClickListener {
            mbtClient.stopRecording()
        }
    }

    private fun onBtnStartEEGClicked() {

        val streamingParams = StreamingParams.Builder()
            .setEEG(binding.chbEeg.isChecked)
            .setQualityChecker(true)
            .setTriggerStatus(binding.chbTrigger.isChecked)
            .setAccelerometer(binding.chbIms.isChecked)
            .build()

        Timber.i("streamingParams : isEEGEnabled = ${streamingParams.isEEGEnabled} | isAccelerometerEnabled = ${streamingParams.isAccelerometerEnabled}")
        mbtClient.setEEGListener(
            object : EEGListener {
                override fun onEEGStatusChange(isEnabled: Boolean) {
                    Timber.i("onEEGStatusChange : $isEnabled")
                }

                override fun onEegPacket(mbtEEGPacket2: MbtEEGPacket2) {
                    Timber.d("onEegPacket : ${mbtEEGPacket2.timeStamp}")
                    runOnUiThread {
                        eegCount++
                        binding.txtEegCount.text = eegCount.toString()
                        if (mbtClient.isRecordingEnabled()) {
                            binding.txtRecordingCount.text =
                                mbtClient.getRecordingBufferSize().toString()
                        }
                    }
                }

                override fun onEegError(error: Throwable) {
                    Timber.e(error)
                }
            })

        mbtClient.setAccelerometerListener(
            object : AccelerometerListener {
                override fun onIMSStatusChange(isEnabled: Boolean) {
                    Timber.i("onIMSStatusChange = $isEnabled")
                }

                override fun onAccelerometerPacket(imsPacket: ImsPacket) {
                    Timber.i("onAccelerometerPacket : Size = ${imsPacket.positions.size}")
                    runOnUiThread {
                        imsCount++
                        binding.txtImsCount.text = imsCount.toString()
                        if (mbtClient.isRecordingEnabled()) {
                            binding.txtRecordingCount.text =
                                mbtClient.getRecordingBufferSize().toString()
                        }
                    }
                }

                override fun onAccelerometerError(error: Throwable) {
                    Timber.e(error)
                }

            }
        )
        mbtClient.startStreaming(streamingParams)
    }

    private fun onBtnStartRecordingClicked() {
        if (deviceInformation == null) {
            addResultText("Please retrieve device information before start recording")
            return
        }

        val name = "${deviceInformation?.productName}-${getTimeNow()}.json"
        var folder = File(Environment.getExternalStorageDirectory().toString() + "/MBT_SAMPLES")
        folder.mkdirs()
        if (!folder.isDirectory || !folder.canWrite()) {
            addResultText("do not have permission on external storage, file will be created in private memory")
            folder = cacheDir
        }
        val outputFile = File(folder, name)

        mbtClient.startRecording(
            RecordingOption(
                outputFile,
                KwakContext().apply { ownerId = "1" },
                deviceInformation!!,
                "record-" + UUID.randomUUID().toString()
            ),
            object : RecordingListener {
                override fun onRecordingSaved(outputFile: File) {
                    val path = outputFile.path
                    Timber.i("output file path = $path")
                    addResultText("output file path = $path")

                    if (path.isPrivateMemory()) {
                        val contentUri: Uri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "com.mybraintech.sdk.sample",
                            outputFile
                        )
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/json"
                            data = contentUri
                            putExtra(Intent.EXTRA_STREAM, contentUri);
                            clipData = ClipData.newRawUri("", contentUri)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }.also {
                            startActivity(it)
                        }
                    }
                }

                override fun onRecordingError(error: Throwable) {
                    Timber.e(error)
                    addResultText(error.message ?: "onRecordingError")
                }

            }
        )
    }

    fun addResultText(text: String) {
        runOnUiThread {
            sb.appendLine(text)
            binding.txtStatus.text = sb.toString()
        }
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
        sb.appendLine("onConnectionError = ${error.message}")
        binding.txtStatus.text = sb.toString()
    }

    override fun onDeviceDisconnected() {
        deviceInformation = null
        addResultText("onDeviceDisconnected")
    }

    //----------------------------------------------------------------------------
    // MARK: battery
    //----------------------------------------------------------------------------
    override fun onBatteryLevel(float: Float) {
        sb.appendLine("onBatteryLevel = $float")
        binding.txtStatus.text = sb.toString()
    }

    override fun onBatteryLevelError(error: Throwable) {
        Timber.e(error)
    }

    @SuppressLint("SimpleDateFormat")
    fun getTimeNow(): String {
        try {
            val date = Date()
            val tf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return tf.format(date)
        } catch (e: Exception) {
            Timber.e(e)
            return System.currentTimeMillis().toString()
        }
    }

    private fun String.isPrivateMemory(): Boolean {
        return this.contains(this@MainActivity.packageName)
    }
}


