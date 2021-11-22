package com.mybraintech.sdk.core.bluetooth.central

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mybraintech.sdk.core.listener.ConnectionListener
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import timber.log.Timber

interface IBluetoothConnectable {
    fun connectMbt(device: BluetoothDevice)
    fun disconnectMbt()
}

interface IBluetoothCentral {
    fun connect(scanOption: MBTScanOption)
    fun setConnectionListener(connectionListener: ConnectionListener? = null)
}

class BluetoothCentral(val context: Context, val bluetoothConnectable: IBluetoothConnectable) : IBluetoothCentral, ScanCallback() {

    //----------------------------------------------------------------------------
    // MARK: - Properties
    //----------------------------------------------------------------------------

    private var mConnectionListener: ConnectionListener? = null
    var isScanning: Boolean = false
    var scanOption: MBTScanOption? = null
    val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()

    // TODO: Find right type for this set
    private var discoveredPeripherals: MutableSet<Any> = HashSet()

//  private val peripheralValidator = PeripheralValidator()

    private val bluetoothObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
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
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    Timber.d("ACTION_BOND_STATE_CHANGED : ${device?.name}: ${device?.address}: ${device?.bondState}")
                    when (device?.bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                        }
                        BluetoothDevice.BOND_NONE -> {
                        }
                    }
                }

            }
        }
    }

    /******************** Callbacks ********************/

//  var onDiscoverPeripheral: ((CBPeripheral) -> Void)? = null
//  var onConnectToPeripheral: ((PeripheralResult) -> Void)? = null
    var onError: ((Error) -> Void)? = null
//  var onDisconnect: ((CBPeripheral, Error?) -> Void)? = null

    //----------------------------------------------------------------------------
    // MARK: - Initialization
    //----------------------------------------------------------------------------

    init {
        //subscribe to bluetooth intents
        context.registerReceiver(
            bluetoothObserver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        context.registerReceiver(
            bluetoothObserver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )
    }


    //----------------------------------------------------------------------------
    // MARK: - Scanning
    //----------------------------------------------------------------------------

    private fun startScan() {
        discoveredPeripherals.clear()

        val settings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(5000)
            .setUseHardwareBatchingIfSupported(true)
            .build()
        val filters = if (isScanningIndus5()) {
            null
        } else {
            TODO("implement filter for indus2/3")
//            val filters: MutableList<ScanFilter> = ArrayList()
//            filters.add(ScanFilter.Builder().setServiceUuid(mUuid).build())
        }
        scanner.startScan(filters, settings, this)
    }

    fun stopScan() {
        scanner.stopScan(this)
    }

    private fun isScanningIndus5(): Boolean {
        return scanOption?.isIndus5 == true
    }

    private fun isIndus5ScanResult(result: ScanResult): Boolean {
        val INDUS5_PREFIX_1 = "melo_2"
        val INDUS5_PREFIX_2 = "qp_"
        val name = result.device.name
        return if (name == null) {
            false
        } else {
            (name.startsWith(INDUS5_PREFIX_1) || (name.startsWith(INDUS5_PREFIX_2)))
        }
    }

    private fun handleNewDiscoveredPeripherals(results: List<ScanResult>) {
        for (result in results) {
            Timber.d("handleNewDiscoveredPeripherals : name = ${result.device.name} | address = ${result.device.address} ")
            if (isScanningIndus5()) {
                if (handleIndus5(result)) {
                    Timber.d("found indus5 device, stop running scan, start connection process...")
                    stopScan()
                    break
                }
            } else {
                TODO("not yet implemented")
            }
        }

    }

    //----------------------------------------------------------------------------
    // MARK: - Connection
    //----------------------------------------------------------------------------

    override fun setConnectionListener(connectionListener: ConnectionListener?) {
        mConnectionListener = connectionListener
    }

    override fun connect(scanOption: MBTScanOption) {
        if (!isScanning) {
            isScanning = true
            this.scanOption = scanOption
            startScan()
        }
    }

    fun disconnect() {

    }

    private fun handleConnectionFailure(errorCode: Int) {
        // ...
        mConnectionListener?.onScanFailed(errorCode)
    }

    //----------------------------------------------------------------------------
    // MARK: scan callback
    //----------------------------------------------------------------------------
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        Timber.d("onScanResult : name = ${result.device.name} | address = ${result.device.address} ")
        handleNewDiscoveredPeripherals(listOf(result))
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>) {
        Timber.d("onBatchScanResults : size = ${results.size}")
        handleNewDiscoveredPeripherals(results)
    }

    override fun onScanFailed(errorCode: Int) {
        handleConnectionFailure(errorCode)
    }

    /**
     * @return true if device is indus5 (connection process will start)
     */
    private fun handleIndus5(result: ScanResult): Boolean {
        if (isIndus5ScanResult(result)) {
            if ((scanOption?.name == null) || (scanOption?.name == result.device.name)) {
                bluetoothConnectable.connectMbt()
                return true
            }
        }
        return false
    }
}