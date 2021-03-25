package mbtsdk.com.mybraintech.sdkv2

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.full.activity_test_2.*
import timber.log.Timber

class TestActivity : AppCompatActivity() {

    lateinit var mbtClient: MbtClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_2)

        initSdk()
        initView()
    }

    private fun initView() {
        btn_connect_reconnect.setOnClickListener {
            connectBle(100)
        }
        btn_disconnect.setOnClickListener {
            disconnectBle(100)
        }
        btn_read_firmware_version.setOnClickListener {
            readFirmwareVersion()
        }
        btn_read_battery.setOnClickListener {
            readBatteryLevel()
        }
        btn_start_streaming.setOnClickListener {
            startStreamingEeg()
        }
    }

    private fun startStreamingEeg() {
        val eegListener = object : EegListener<BaseError> {
            override fun onError(error: BaseError?, additionalInfo: String?) {
                Timber.e(error)
            }

            override fun onNewPackets(eegPackets: MbtEEGPacket) {
                txt_eeg_package.text = eegPackets.toString()
            }

            override fun onNewStreamState(streamState: StreamState) {
                txt_stream_state.text = streamState.name
            }
        }
        val streamConfig = StreamConfig.Builder(eegListener)
                .createForDevice(MbtDeviceType.MELOMIND)
        mbtClient.startStream(streamConfig)
    }

    private fun readFirmwareVersion() {
        txt_firmware_version.text = "not implemented yet"
    }

    private fun readBatteryLevel() {
        mbtClient.readBattery(object : DeviceBatteryListener<BaseError> {
            override fun onError(error: BaseError?, additionalInfo: String?) {
                Timber.e(error)
                txt_battery_level.text = error?.message ?: "unknown error"
            }

            override fun onBatteryLevelReceived(level: String?) {
                txt_battery_level.text = level ?: "null"
            }

        })
    }

    private fun initSdk() {
        mbtClient = MbtClient.init(this)
        connectBle()
    }

    private fun connectBle(delay: Long = 300) {
        clearText()
        Handler().postDelayed(connectBleRunnable, delay)
    }

    private fun disconnectBle(delay: Long = 300) {
        clearText()
        Handler().postDelayed(disconnectBleRunnable, delay)
    }

    private fun clearText() {
        txt_is_connected.text = "..."
    }

    private val connectionStateListener = object
        : ConnectionStateListener<BaseError> {
        override fun onError(error: BaseError?, additionalInfo: String?) {
            Timber.e(error)
            txt_is_connected.text = error?.message ?: "unknown error"

        }

        override fun onDeviceConnected(device: MbtDevice?) {
            Timber.i("onDeviceConnected")
            txt_is_connected.text = "connected"
        }

        override fun onDeviceDisconnected(device: MbtDevice?) {
            Timber.i("onDeviceDisconnected")
            txt_is_connected.text = "disconnected"

        }

    }

    private val connectBleRunnable = Runnable {
        val config = ConnectionConfig.Builder(connectionStateListener)
                .createForDevice(MbtDeviceType.MELOMIND)
        mbtClient.connectBluetooth(config)
    }

    private val disconnectBleRunnable = Runnable {
        val config = ConnectionConfig.Builder(connectionStateListener)
                .createForDevice(MbtDeviceType.MELOMIND)
        mbtClient.connectBluetooth(config)
    }
}