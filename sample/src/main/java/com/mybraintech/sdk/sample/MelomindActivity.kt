package com.mybraintech.sdk.sample

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

class MelomindActivity : AppCompatActivity(), ConnectionListener, BatteryLevelListener {

    private lateinit var binding: ActivityQplusBinding
    lateinit var mbtClient: MbtClient
    var mbtDevice: MbtDevice? = null
    var deviceInformation: DeviceInformation? = null
    private val sb = StringBuilder()

    var eegCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQplusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mbtClient = MbtClientManager.getMbtClient(applicationContext, EnumMBTDevice.MELOMIND)

        initView()

        requestPermissions()
    }

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
                    this@MelomindActivity.deviceInformation = deviceInformation
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

        binding.btnStartEegWithTrigger.setOnClickListener {
            onBtnStartEEGClicked(true)
        }

        binding.btnStartEegWithoutTrigger.setOnClickListener {
            onBtnStartEEGClicked(false)
        }

        binding.btnStopEeg.setOnClickListener {
            mbtClient.stopEEG()
            eegCount = 0
        }

        binding.btnStartRecording.setOnClickListener {
            onBtnStartRecordingClicked()
        }

        binding.btnStopRecording.setOnClickListener {
            mbtClient.stopEEGRecording()
        }
    }

    private fun onBtnStartEEGClicked(isStatusEnabled: Boolean) {
        mbtClient.startEEG(
            EEGParams(
                isTriggerStatusEnabled = isStatusEnabled,
                isQualityCheckerEnabled = true
            ),
            object : EEGListener {
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
            },
        )
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

        mbtClient.startEEGRecording(
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
                            this@MelomindActivity,
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
        return this.contains(this@MelomindActivity.packageName)
    }

    //----------------------------------------------------------------------------
    // MARK: permissions
    //----------------------------------------------------------------------------

    private fun requestPermissions(activityIntent: Intent? = null) {
        var permissions =
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                Timber.w("requires BLUETOOTH_CONNECT and BLUETOOTH_SCAN")
//                arrayOf(
//                    Manifest.permission.ACCESS_COARSE_LOCATION,
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN
//                )
//            } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
            )
//            }

        if (!hasPermissions(this, permissions)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, SplashActivity.REQUEST_CODE_PERMISSIONS)
            }
        } else {
            Timber.i("launch activity")
            activityIntent?.let {
                startActivity(it)
            }
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == SplashActivity.REQUEST_CODE_PERMISSIONS) {
            requestPermissions() // request permissions util all is permitted
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

}