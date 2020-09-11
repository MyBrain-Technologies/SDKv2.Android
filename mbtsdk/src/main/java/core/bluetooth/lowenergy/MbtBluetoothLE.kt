package core.bluetooth.lowenergy

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import command.BluetoothCommands.Mtu
import command.CommandInterface.MbtCommand
import command.DeviceCommand
import command.DeviceCommandEvent
import command.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED
import command.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_SUCCESS
import command.DeviceCommandEvent.MBX_CONNECT_IN_A2DP
import command.OADCommands.TransferPacket
import config.MbtConfig
import core.bluetooth.BluetoothInterfaces.IDeviceInfoMonitor
import core.bluetooth.BluetoothProtocol
import core.bluetooth.BluetoothState
import core.bluetooth.MbtBluetoothManager
import core.bluetooth.MbtDataBluetooth.MainBluetooth
import core.bluetooth.StreamState
import core.device.DeviceEvents.RawDeviceMeasure
import core.device.model.DeviceInfo
import core.device.model.MelomindDevice
import core.device.model.MelomindsQRDataBase
import engine.clientevents.BaseError
import engine.clientevents.BluetoothError
import engine.clientevents.ConnectionStateReceiver
import eventbus.events.BluetoothResponseEvent
import features.MbtFeatures
import utils.*
import java.util.*

/** This class contains all required methods to interact with a LE bluetooth peripheral, such as Melomind.
 *
 * In order to work [Manifest.permission.BLUETOOTH] and [Manifest.permission.BLUETOOTH_ADMIN] permissions are required
 * Created by Etienne on 08/02/2018.  */
class MbtBluetoothLE(manager: MbtBluetoothManager) : MainBluetooth(BluetoothProtocol.LOW_ENERGY, manager), IDeviceInfoMonitor {
  /** An internal event used to notify MbtBluetoothLE that A2DP has disconnected. */
  private val mbtGattController: MbtGattController = MbtGattController(this)
  lateinit var bluetoothLeScanner: BluetoothLeScanner
  var gatt: BluetoothGatt? = null

  private val receiver: ConnectionStateReceiver = object : ConnectionStateReceiver() {
    override fun onError(error: BaseError, additionalInfo: String) {}
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      if (action != null) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        LogUtils.d(TAG, "received intent " + action + " for device " + device?.name)
        if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
          if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
            Handler().postDelayed({
              if (currentState == BluetoothState.BONDING) updateConnectionState(true) //current state is set to BONDED & and future is completed
            }, 1000)
          }
        }
      }
    }
  }

  companion object {
    private val TAG = MbtBluetoothLE::class.java.simpleName
    private const val START = true
    private const val STOP = false
    private const val CONNECT_GATT_METHOD = "connectGatt"
    private const val REMOVE_BOND_METHOD = "removeBond"
    private const val REFRESH_METHOD = "refresh"

    var instance: MbtBluetoothLE? = null
    fun initInstance(manager: MbtBluetoothManager): MainBluetooth {
      return MbtBluetoothLE(manager).also { instance = it }
    }
  }

  /** Start bluetooth low energy scanner in order to find BLE device that matches the specific filters.
   *
   * **Note:** This method will consume your mobile/tablet battery. Please consider calling
   * [.stopScan] when scanning is no longer needed.
   * @return Each found device that matches the specified filters
   */
  override fun startScan(): Boolean {
    val filterOnDeviceService = true
    LogUtils.i(TAG, " start low energy scan on device " + manager.context.deviceNameRequested)
    val mFilters: MutableList<ScanFilter> = ArrayList()
    if (super.bluetoothAdapter == null || super.bluetoothAdapter?.bluetoothLeScanner == null) {
      Log.e(TAG, "Unable to get LE scanner")
      notifyConnectionStateChanged(BluetoothState.SCAN_FAILURE)
      return false
    } else bluetoothLeScanner = super.bluetoothAdapter!!.bluetoothLeScanner
    currentDevice = null
    if (filterOnDeviceService) {
      val filterService = ScanFilter.Builder()
          .setServiceUuid(ParcelUuid(MelomindCharacteristics.SERVICE_MEASUREMENT))
      if (manager.context.deviceNameRequested != null) filterService.setDeviceName(manager.context.deviceNameRequested)
      mFilters.add(filterService.build())
    }
    val settings = ScanSettings.Builder()
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    LogUtils.i(TAG, String.format("Starting Low Energy Scan with filtering on name '%s' and service UUID '%s'", manager.context.deviceNameRequested, MelomindCharacteristics.SERVICE_MEASUREMENT))
    bluetoothLeScanner.startScan(mFilters, settings, scanCallback)
    if (currentState == BluetoothState.READY_FOR_BLUETOOTH_OPERATION) manager.connecter.updateConnectionState(false) //current state is set to SCAN_STARTED
    return true //true : scan is started
  }

  /** callback used when scanning using bluetooth Low Energy scanner. */
  private val scanCallback: ScanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) { //Callback when a BLE advertisement has been found.
      AsyncUtils.Companion.executeAsync(Runnable {
        if (currentState == BluetoothState.SCAN_STARTED) {
          super.onScanResult(callbackType, result)
          currentDevice = result.device
          LogUtils.i(TAG, String.format("Stopping Low Energy Scan -> device detected " + "with name '%s' and MAC address '%s' ", currentDevice?.name, currentDevice?.address))
          updateConnectionState(true) //current state is set to DEVICE_FOUND and future is completed
        }
      })
    }

    override fun onScanFailed(errorCode: Int) { //Callback when scan could not be started.
      super.onScanFailed(errorCode)
      var msg = "Could not start scan. Reason -> "
      if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
        msg += "Scan already started!"
        notifyConnectionStateChanged(BluetoothState.SCAN_FAILED_ALREADY_STARTED)
      } else {
        msg += "Scan failed. No more details."
        notifyConnectionStateChanged(BluetoothState.SCAN_FAILURE)
      }
      LogUtils.e(TAG, msg)
    }
  }

  /** Stops the currently bluetooth low energy scanner.
   * If a lock is currently waiting, the lock is disabled.  */
  override fun stopScan() {
    LogUtils.i(TAG, "Stopping Low Energy scan")
    if (isAdapterReady
        && PermissionUtils.hasPermissions(context, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
      bluetoothLeScanner.stopScan(scanCallback)
    if (currentState != BluetoothState.DEVICE_FOUND && currentState != BluetoothState.DATA_BT_CONNECTING) currentDevice = null
  }

  /** This method sends a request to the headset to **`START`**
   * the EEG raw data acquisition process and
   * enables Bluetooth Low Energy notification to receive the raw data.
   *
   * **Note:** calling this method will start the raw EEG data acquisition process
   * on the headset which will **consume battery life**. Please consider calling
   * [.stopStream] when EEG raw data are no longer needed.
   * If there is already a streaming session in progress, nothing happens and true is returned.
   * @return              `true` if request has been sent correctly
   * `false` on immediate error
   */
  @Synchronized
  override fun startStream(): Boolean {
    return switchStream(START)
  }

  /**  Enable notifications on HeadsetStatus characteristic in order to have the saturation and DC Offset values  */
  fun activateDeviceStatusMonitoring(): Boolean {
    return if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_HEADSET_STATUS)) false else enableOrDisableNotificationsOnCharacteristic(true, gatt!!.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_HEADSET_STATUS))
  }

  /** This method sends a request to the headset to **`STOP`**
   * the EEG raw data acquisition process, therefore disabling the Bluetooth Low Energy notification
   * and cleaning reference to previously registered listener.
   *
   * Calling this method will **preserve battery life** by halting the raw EEG
   * data acquisition process on the headset.
   * If there is no streaming session in progress, nothing happens and true is returned.
   * @return true upon correct EEG disability request, false on immediate error
   */
  override fun stopStream(): Boolean {
    return switchStream(STOP)
  }

  /**  This method sends a request to the headset to **`START`** or **`STOP`**
   * the EEG raw data acquisition process and
   * **`ENABLES`** or **`DISABLED`**
   * Bluetooth Low Energy notification to receive the raw data.
   * If there is already a streaming session started or stopped, nothing happens and true is returned.
   * @return              `true` if request has been sent correctly
   * `false` on immediate error
   */
  private fun switchStream(isStart: Boolean): Boolean {
    if (isStreaming == isStart) return true
    if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)) return false
    try {
      Thread.sleep(50) //Adding small sleep to "free" bluetooth
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
    return enableOrDisableNotificationsOnCharacteristic(isStart,
        gatt!!.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)
            .getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
  }

  /**  Whenever there is a new headset status received, this method is called to notify the bluetooth manager about it.
   * @param payload the new headset status as a raw byte array. This byte array has to be parsed afterward.
   */
  fun notifyNewHeadsetStatus(payload: ByteArray) {
    manager.notifyEvent(RawDeviceMeasure(payload))
  }

  /** Enable or disable notifications on specific characteristic provinding this characteristic is "notification ready".
   * @param enableNotification enabling if set to true, false otherwise
   * @param characteristic the characteristic to enable or disable notification on.
   *
   * This operation is synchronous, meaning the thread running this method is blocked until the operation completes.
   * @return  `true` if the notification has been successfully established within the 2 seconds of allotted time,
   * or `false` for any error
   */
  @Synchronized
  fun enableOrDisableNotificationsOnCharacteristic(enableNotification: Boolean, characteristic: BluetoothGattCharacteristic): Boolean {
    if (!isConnected && currentState != BluetoothState.SENDIND_QR_CODE) return false
    LogUtils.i(TAG, "Now enabling local notification for characteristic: " + characteristic.uuid)
    if (!gatt!!.setCharacteristicNotification(characteristic, enableNotification)) {
      LogUtils.e(TAG, "Failed to enable local notification for characteristic: " + characteristic.uuid)
      return false
    }
    val notificationDescriptor = characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)
    if (notificationDescriptor == null) {
      LogUtils.e(TAG, String.format("Error: characteristic with " +
          "UUID <%s> does not have a descriptor (UUID <%s>) to enable notification remotely!",
          characteristic.uuid.toString(), MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString()))
      return false
    }
    LogUtils.i(TAG, "Now enabling remote notification for characteristic: " + characteristic.uuid)
    if (!notificationDescriptor.setValue(
            if (enableNotification) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
      val sb = StringBuilder()
      for (value in if (enableNotification) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) {
        sb.append(value.toInt())
        sb.append(';')
      }
      LogUtils.e(TAG, String.format("Error: characteristic's notification descriptor with " +
          "UUID <%s> could not store the ENABLE notification value <%s>.",
          MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString(), sb.toString()))
      return false
    }
    try {
      Thread.sleep(1000)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
    if (gatt?.writeDescriptor(notificationDescriptor) == false) {
      LogUtils.e(TAG, "Error: failed to initiate write descriptor operation in order to remotely " +
          "enable notification for characteristic: " + characteristic.uuid)
      return false
    }
    LogUtils.i(TAG, "Successfully initiated write descriptor operation in order to remotely " +
        "enable notification... now waiting for confirmation from headset.")
    val result = startWaitingOperation(MbtConfig.getBluetoothA2DpConnectionTimeout())
    return (if (result == null || result !is Boolean) false else result)
  }

  /**
   * Reset Bluetooth
   */
  fun resetBluetooth() {
    clearMobileDeviceCache()
    currentDevice?.let { unpairDevice(it) }
  }

  /**
   * This method uses reflexion to get the refresh hidden method from BluetoothGatt class. Is is used
   * to clean up the cache that Android system uses when connecting to a known BluetoothGatt peripheral.
   * It is recommanded to use it right after updating the firmware, especially when the bluetooth
   * characteristics have been updated.
   * @return true if method invocation worked, false otherwise
   */
  private fun clearMobileDeviceCache(): Boolean {
    LogUtils.d(TAG, "Clear the cache")
    try {
      val localMethod = gatt?.javaClass?.getMethod(REFRESH_METHOD)
      localMethod?.let{ return it.invoke(gatt) as Boolean }
    } catch (localException: Exception) {
      Log.e(TAG, "An exception occurred while refreshing device")
    }
    return false
  }

  /** This method removes bonding of the device.  */
  fun unpairDevice(device: BluetoothDevice) {
    try {
      val m = device.javaClass
          .getMethod(REMOVE_BOND_METHOD, *null as Array<Class<*>?>)
      m.invoke(device, null as Array<Any?>?)
    } catch (e: Exception) {
      Log.e(TAG, e.message)
    }
  }

  /** Starts the connect operation in order to connect the [bluetooth device][BluetoothDevice] (peripheral)
   * to the terminal (central).
   * If the operation starts successfully, a new [gatt][BluetoothGatt] instance will be stored.
   * @param context the context which the connection event takes place in.
   * @param device the bluetooth device to connect to.
   * @return true if operation has correctly started, false otherwise.
   */
  override fun connect(context: Context, device: BluetoothDevice): Boolean {
    LogUtils.i(TAG, " connect in Low Energy " + device.name + " address is " + device.address)
    BroadcastUtils.registerReceiverIntents(context, receiver, BluetoothDevice.ACTION_BOND_STATE_CHANGED)

    //Using reflexion here because min API is 21 and transport layer is not available publicly until API 23
    try {
      val connectGattMethod = device.javaClass
          .getMethod(CONNECT_GATT_METHOD,
              Context::class.java, Boolean::class.javaPrimitiveType, BluetoothGattCallback::class.java, Int::class.javaPrimitiveType)
      val transport = device.javaClass.getDeclaredField("TRANSPORT_LE").getInt(null)
      gatt = connectGattMethod.invoke(device, context, false, mbtGattController, transport) as BluetoothGatt?
      return true
    } catch (e: Exception) {
      val errorMsg = " -> " + e.message
      LogUtils.e(TAG, "Failed to find connectGatt method via reflexion$errorMsg")
    }
    return false
  }

  /** Disconnects from the currently connected [gatt instance][BluetoothGatt] and sets it to null  */
  override fun disconnect(): Boolean {
    LogUtils.i(TAG, "Disconnect in low energy")
    gatt?.disconnect()
    gatt = null
    return false
  }

  override fun isCurrentDeviceNameEqual(deviceName: String): Boolean {
    return gatt != null && gatt!!.device != null && gatt!!.device.name == deviceName
  }

  fun getBleDeviceNameFromA2dp(deviceName: String): String? {
    return (if (MelomindDevice.isDeviceNameValidForMelomind(deviceName))
      deviceName.replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX, MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX)
    else  //audio_ prefix is replaced by a melo_ prefix
      MelomindsQRDataBase(context, true)[deviceName])
  }

  override val isConnected: Boolean
    get() = currentState == BluetoothState.CONNECTED_AND_READY || currentState == BluetoothState.CONNECTED
  
  /** Starts a read operation on a specific characteristic
   * @param characteristic the characteristic to read
   * @return immediatly false on error, true true if read operation has started correctly
   */
  fun startReadOperation(characteristic: UUID): Boolean {
    if (!isConnected && currentState != BluetoothState.DISCOVERING_SUCCESS && !currentState.isReadingDeviceInfoState() && currentState != BluetoothState.BONDING) {
      notifyConnectionStateChanged(if (currentState == BluetoothState.BONDING) BluetoothState.BONDING_FAILURE else BluetoothState.READING_FAILURE)
      return false
    }
    val service = if (characteristic == MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION || characteristic == MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION || characteristic == MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER || characteristic == MelomindCharacteristics.CHARAC_INFO_MODEL_NUMBER) MelomindCharacteristics.SERVICE_DEVICE_INFOS else MelomindCharacteristics.SERVICE_MEASUREMENT
    if (!checkServiceAndCharacteristicValidity(service, characteristic)) {
      notifyConnectionStateChanged(if (currentState == BluetoothState.BONDING) BluetoothState.BONDING_FAILURE else BluetoothState.READING_FAILURE)
      return false
    }
    if (!gatt!!.readCharacteristic(gatt!!.getService(service).getCharacteristic(characteristic))) {
      LogUtils.e(TAG, "Error: failed to initiate read characteristic operation")
      if (currentState == BluetoothState.BONDING || currentState.isReadingDeviceInfoState()) notifyConnectionStateChanged(if (currentState == BluetoothState.BONDING) BluetoothState.BONDING_FAILURE else BluetoothState.READING_FAILURE)
      return false
    }
    //if(getCurrentState().isReadingDeviceInfoState())
    LogUtils.i(TAG, "Successfully initiated read characteristic operation")
    return true
  }

  /** Starts a write operation on a specific characteristic
   * @param characteristic the characteristic to perform write operation on
   * @param payload the payload to write to the characteristic
   * @return immediatly false on error, true otherwise
   */
  @Synchronized
  fun startWriteOperation(service: UUID, characteristic: UUID, payload: ByteArray?): Boolean {
    if (!checkServiceAndCharacteristicValidity(service, characteristic)) {
      LogUtils.e(TAG, "Error: failed to check service and characteristic validity$characteristic")
      return false
    }

    //Send buffer
    gatt!!.getService(service).getCharacteristic(characteristic).value = payload
    //Log.d(TAG, "write "+ Arrays.toString(gatt.getService(service).getCharacteristic(characteristic).getValue()));
    if (!gatt!!.writeCharacteristic(gatt!!.getService(service).getCharacteristic(characteristic))) { //the mbtgattcontroller onCharacteristicWrite callback is invoked, reporting the result of the operation.
      LogUtils.e(TAG, "Error: failed to write characteristic $characteristic")
      return false
    }
    return true
  }

  /** Checks whether the service and characteristic about to be used to communicate with the remote device.
   * @param service the service to check
   * @param characteristic the characteristic to check
   * @return false if something not valid, true otherwise
   */
  fun checkServiceAndCharacteristicValidity(service: UUID, characteristic: UUID): Boolean {
    return gatt != null && gatt!!.getService(service) != null && gatt!!.getService(service).getCharacteristic(characteristic) != null
  }

  /** Checks if the charateristic has notifications already enabled or not.
   * @param service the Service UUID that holds the characteristic
   * @param characteristic the characteristic UUID.
   * @return true is already enabled notifications, false otherwise.
   */
  fun isNotificationEnabledOnCharacteristic(service: UUID, characteristic: UUID): Boolean {
    if (!checkServiceAndCharacteristicValidity(service, characteristic)) return false
    return if (gatt!!.getService(service).getCharacteristic(characteristic).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID) == null) false else Arrays.equals(gatt!!.getService(service).getCharacteristic(characteristic).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID).value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
  }

  /** Initiates a read battery operation on this correct Protocol  */
  override fun readBattery(): Boolean {
    LogUtils.i(TAG, "read battery")
    return startReadOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_BATTERY_LEVEL)
  }



  /** Initiates a read firmware version operation on this correct Protocol  */
  override fun readFwVersion(): Boolean {
    LogUtils.i(TAG, "read firmware version")
    return startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION)
  }

  /** Initiates a read hardware version operation on this correct Protocol */
  override fun readHwVersion(): Boolean {
    LogUtils.i(TAG, "read hardware version")
    return startReadOperation(MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION)
  }

  /** Initiates a read serial number operation on this correct Protocol  */
  override fun readSerialNumber(): Boolean {
    LogUtils.i(TAG, "read serial number requested")
    return startReadOperation(MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER)
  }

  /** Initiates a read model number operation on this correct BtProtocol  */
  override fun readModelNumber(): Boolean {
    LogUtils.i(TAG, "read product name")
    return startReadOperation(MelomindCharacteristics.CHARAC_INFO_MODEL_NUMBER)
  }

  /** Callback called by the [gatt controller][MbtGattController] when the notification state has changed.
   * @param isSuccess if the modification state is correctly changed
   * @param characteristic the characteristic which had its notification state changed
   * @param wasEnableRequest if the request was to enable (true) or disable (false) request.
   */
  fun onNotificationStateChanged(isSuccess: Boolean, characteristic: BluetoothGattCharacteristic, wasEnableRequest: Boolean) {
    if (MelomindCharacteristics.CHARAC_MEASUREMENT_EEG == characteristic.uuid) {
      if (wasEnableRequest && isSuccess) {
        notifyStreamStateChanged(StreamState.STARTED)
      } else if (!wasEnableRequest && isSuccess) {
        notifyStreamStateChanged(StreamState.STOPPED)
      } else {
        notifyStreamStateChanged(StreamState.FAILED)
      }
    } else if (MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX == characteristic.uuid) {
      //TODO see what's important here
    } else if (MelomindCharacteristics.CHARAC_HEADSET_STATUS == characteristic.uuid) {
      //TODO see what's important here
    }
  }

  /** Close gatt if the current state is connected & ready or upgrading
   * @param gatt
   */
  fun onStateDisconnected(gatt: BluetoothGatt) {
    if (gatt != null && currentState.ordinal >= BluetoothState.CONNECTED_AND_READY.ordinal) gatt.close()
    notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED)
  }

  fun onStateDisconnecting() {
    notifyConnectionStateChanged(BluetoothState.DISCONNECTING)
  }

  fun onStateConnecting() {
    if (currentState == BluetoothState.DEVICE_FOUND) this.updateConnectionState(false) //current state is set to DATA_BT_CONNECTING
  }

  fun onStateConnected() {
    if (currentState == BluetoothState.DATA_BT_CONNECTING || currentState == BluetoothState.SCAN_STARTED) updateConnectionState(true) //current state is set to DATA_BT_CONNECTION_SUCCESS and future is completed
    else if (currentState == BluetoothState.IDLE || currentState == BluetoothState.UPGRADING) this.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY)
  }

  /** Callback triggered by the [MbtGattController] callback when the connection state has changed.
   * @param newState the new [state][BluetoothState]
   */
  override fun notifyConnectionStateChanged(newState: BluetoothState) {
    super.notifyConnectionStateChanged(newState)
    if (newState == BluetoothState.DATA_BT_DISCONNECTED) {
      if (isStreaming) notifyStreamStateChanged(StreamState.DISCONNECTED)
      BroadcastUtils.unregisterReceiver(context, receiver)
    }
  }

  /** Callback triggered by the [MbtGattController] callback
   * when an event -not related to a mailbox request sent by the SDK- occurs
   * @param mailboxEvent the event that occurs
   * @param eventData the data associated to the mailbox event detected
   */
  fun notifyEventReceived(mailboxEvent: DeviceCommandEvent?, eventData: ByteArray?) {
    manager.notifyEvent(BluetoothResponseEvent(mailboxEvent, eventData))
  }

  fun notifyConnectionResponseReceived(mailboxEvent: DeviceCommandEvent, mailboxResponse: Byte) {
    if (!mbtGattController.isConnectionMailboxEvent(mailboxEvent)) {
      LogUtils.e(TAG, "Error : received response is not related to Bluetooth connection")
      return
    }
    LogUtils.i(TAG, "Received response for " + (if (mailboxEvent == MBX_CONNECT_IN_A2DP) "connection" else "disconnection") + " : " + mailboxResponse)
    if (mailboxEvent == MBX_CONNECT_IN_A2DP) {
      val jackConnectedResponseCode = MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED)
      val successResponseCode = MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_SUCCESS)
      if (jackConnectedResponseCode?.let { BitUtils.areByteEquals(it, mailboxResponse) } == true)
        updateConnectionState(BluetoothState.JACK_CABLE_CONNECTED)
      else if (successResponseCode?.let { BitUtils.areByteEquals(it, mailboxResponse) } == true)
        updateConnectionState(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS)
    } else updateConnectionState(BluetoothState.AUDIO_BT_DISCONNECTED)
  }

  fun updateConnectionState(isCompleted: Boolean) {
    manager.connecter.updateConnectionState(isCompleted) //do nothing if the current state is CONNECTED_AND_READY
  }

  fun updateConnectionState(newState: BluetoothState?) {
    manager.connecter.updateConnectionState(newState) //do nothing if the current state is CONNECTED_AND_READY
  }

  /** This method handle a single command in order to
   * reconfigure some headset or bluetooth streaming parameters
   * or get values stored by the headset
   * or ask the headset to perform an action.
   * The command's parameters are bundled in a [instance][command.CommandInterface.MbtCommand]
   * that can provide a nullable response callback.
   * All method inside are blocking.
   * @param command is the [command.CommandInterface.MbtCommand] object that defines the type of command to send
   * and the associated command parameters.
   * One of this parameter is an optional callback that returns the response
   * sent by the headset to the SDK once the command is received.
   */
  override fun sendCommand(command: MbtCommand<BaseError>) {
    var response: Any? = null
    if (!isConnectedDeviceReadyForCommand) { //error returned if no headset is connected
      LogUtils.e(TAG, "Command not sent : $command")
      command.onError(BluetoothError.ERROR_NOT_CONNECTED, null)
    } else { //any command is not sent if no device is connected
      if (command.isValid) { //any invalid command is not sent : validity criteria are defined in each Bluetooth implemented class , the onError callback is triggered in the constructor of the command object
        LogUtils.d(TAG, "Valid command : $command")
        val requestSent = sendRequestData(command)
        if (!requestSent) {
          LogUtils.e(TAG, "Command sending failed")
          command.onError(BluetoothError.ERROR_REQUEST_OPERATION, null)
        } else {
          command.onRequestSent()
          if (command.isResponseExpected) {
            response = startWaitingOperation(11000)
            command.onResponseReceived(response)
          }
        }
      } else LogUtils.w(TAG, "Command not sent : $command")
    }
    manager.reader.notifyResponseReceived(response, command) //return null response to the client if request has not been sent
  }

  fun sendRequestData(command: MbtCommand<*>): Boolean {
    if (command is Mtu)
      return changeMTU(command.serialize())

    if (command is DeviceCommand<*, *>)
      return writeCharacteristic((command.serialize() as ByteArray),
          MelomindCharacteristics.SERVICE_MEASUREMENT,
          if (command is TransferPacket) MelomindCharacteristics.CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER
          else MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX,
          command !is TransferPacket)
    return false
  }

  /** Initiates a change MTU request in order to have bigger (or smaller) bluetooth notifications.
   * The default size is also the minimum size : 23
   * The maximum size is set to 121.
   * This method is synchronous and blocks the calling thread until operation is complete.
   * See [BluetoothGatt.requestMtu] for more info.
   * @param newMTU the new MTU value.
   * @return false if request dod not start as planned, true otherwise.
   */
  fun changeMTU(newMTU: Int): Boolean {
    LogUtils.i(TAG, "change mtu $newMTU")
    return if (gatt == null) false else gatt!!.requestMtu(newMTU)
  }

  fun writeCharacteristic(buffer: ByteArray, service: UUID, characteristic: UUID, enableNotification: Boolean): Boolean {
    //Log.d(TAG, "write characteristic "+characteristic+ " for service "+service);
    if (buffer.isEmpty()) return false
    if (!isNotificationEnabledOnCharacteristic(service, characteristic) && enableNotification) {
      enableOrDisableNotificationsOnCharacteristic(true, gatt!!.getService(service).getCharacteristic(characteristic))
    }
    if (!startWriteOperation(service, characteristic, buffer)) {
      LogUtils.e(TAG, "Failed to send the command the the headset")
      return false
    }
    return true
  }

  /** Once a device is connected in Bluetooth Low Energy / SPP for data streaming, we consider that the Bluetooth connection process is not fully completed.
   * The services offered by a remote device as well as their characteristics and descriptors are discovered to ensure that Data Streaming can be performed.
   * It means that the Bluetooth Manager retrieve all the services, which can be seen as categories of data that the headset is transmitting
   * This is an asynchronous operation.
   * Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered.
   * If the discovery was successful, the remote services can be retrieved using the getServices function  */
  fun discoverServices() {
    LogUtils.i(TAG, "start discover services")
    updateConnectionState(false) //current state is set to DISCOVERING_SERVICES
    if (!gatt!!.discoverServices()) {
      notifyConnectionStateChanged(BluetoothState.DISCOVERING_FAILURE)
      LogUtils.i(TAG, " discover services failed")
    }
  }

  /** Starts a read operation of the Battery charge level to trigger an automatic bonding.
   * If the headset is already bonded, it will return the value of the battery level.
   * If the headset is not already bonded, it will bond and return an authentication failed status code (0x89 GATT_AUTH_FAIL)
   * in the [MbtGattController.onCharacteristicRead]onCharacteristicRead callback  */
  fun bond() {
    LogUtils.i(TAG, "start bonding")
    val isBondRetry = currentState == BluetoothState.BONDING
    if (isBondRetry && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      LogUtils.d(TAG, "Retry not necessary : Android will retry the read operation itself after bonding has completed") //However, on Android 6 & 7 you will have to retry the operation yourself
      return
    }
    if (currentState == BluetoothState.READING_SUCCESS) updateConnectionState(false) //current state is set to BONDING
    manager.reader.startReadOperation(DeviceInfo.BATTERY) //trigger bonding indirectly
  }

  override fun notifyDeviceInfoReceived(deviceInfo: DeviceInfo, deviceValue: String) { // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
    super.notifyDeviceInfoReceived(deviceInfo, deviceValue)
    if (currentState.isReadingDeviceInfoState()) updateConnectionState(true) //current state is set to READING_FIRMWARE_VERSION_SUCCESS or READING_HARDWARE_VERSION_SUCCESS or READING_SERIAL_NUMBER_SUCCESS or READING_SUCCESS if reading device info and future is completed
  }

  public override fun notifyBatteryReceived(value: Int) {
    if (currentState == BluetoothState.BONDING) updateConnectionState(true) //current state is set to BONDED
    if (value != -1) super.notifyBatteryReceived(value)
  }

}