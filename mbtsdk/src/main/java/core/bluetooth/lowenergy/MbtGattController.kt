package core.bluetooth.lowenergy

import android.bluetooth.*
import android.util.Log
import command.DeviceCommandEvent
import command.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS
import command.DeviceCommandEvent.Companion.CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID
import command.DeviceCommandEvent.MBX_CONNECT_IN_A2DP
import core.Indus5FastMode
import core.bluetooth.BluetoothState
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_HEADSET_STATUS
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_INFO_FIRMWARE_VERSION
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_INFO_HARDWARE_VERSION
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_INFO_MODEL_NUMBER
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_INFO_SERIAL_NUMBER
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_BATTERY_LEVEL
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_EEG
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_MAILBOX
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.INDUS_5_RX_CHARACTERISTIC
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.INDUS_5_TRANSPARENT_SERVICE
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.INDUS_5_TX_CHARACTERISTIC
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.SERVICE_DEVICE_INFOS
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.SERVICE_MEASUREMENT
import core.device.model.DeviceInfo
import core.device.model.MelomindDevice
import timber.log.Timber
import utils.BitUtils
import utils.CommandUtils
import utils.LogUtils
import utils.MbtAsyncWaitOperation
import java.util.*

/**
 * A custom Gatt controller that extends [BluetoothGattCallback] class.
 * All gatt operations from [MbtBluetoothLE] LE controller are completed here.
 *
 * @see BluetoothGattCallback
 */
internal class MbtGattController(private val mbtBluetoothLE: MbtBluetoothLE) : BluetoothGattCallback() {

  //----------------------------------------------------------------------------
  // indus5 variables
  //----------------------------------------------------------------------------

  /**
   * use for indus 5
   */
  private var transparentService: BluetoothGattService? = null
  var indus5RxCharacteristic: BluetoothGattCharacteristic? = null
  var indus5TxCharacteristic: BluetoothGattCharacteristic? = null

  //----------------------------------------------------------------------------
  // indus 2/3 variables
  //----------------------------------------------------------------------------
  /**
   * use for indus 2/3
   */
  private var mainService: BluetoothGattService? = null
  private var deviceInfoService: BluetoothGattService? = null
  private var measurement: BluetoothGattCharacteristic? = null
  private var headsetStatus: BluetoothGattCharacteristic? = null
  private var mailBox: BluetoothGattCharacteristic? = null
  private var oadPacketsCharac: BluetoothGattCharacteristic? = null
  private var battery: BluetoothGattCharacteristic? = null
  private var fwVersion: BluetoothGattCharacteristic? = null
  private var hwVersion: BluetoothGattCharacteristic? = null
  private var serialNumber: BluetoothGattCharacteristic? = null
  private var modelNumber: BluetoothGattCharacteristic? = null

  //----------------------------------------------------------------------------
  // functions
  //----------------------------------------------------------------------------

  override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
    super.onPhyUpdate(gatt, txPhy, rxPhy, status)
  }

  override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
    super.onPhyRead(gatt, txPhy, rxPhy, status)
  }

  /**
   * Callback indicating when GATT client has connected/disconnected to/from the headset
   */
  override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
    super.onConnectionStateChange(gatt, status, newState)
    var msg = "Current state was " + mbtBluetoothLE.currentState + " but Connection state change : " + if (newState == 2) "connected " else " code is $newState"
    when (newState) {
      BluetoothGatt.STATE_CONNECTING -> mbtBluetoothLE.onStateConnecting()
      BluetoothGatt.STATE_CONNECTED -> {
        LogUtils.e("ConnSteps", "6c : Bluetooth connected")
        mbtBluetoothLE.onStateConnected()
      }
      BluetoothGatt.STATE_DISCONNECTING -> mbtBluetoothLE.onStateDisconnecting()
      BluetoothGatt.STATE_DISCONNECTED -> {
        LogUtils.e(TAG, "Gatt returned disconnected state")
        mbtBluetoothLE.onStateDisconnected(gatt)
      }
      else -> {
        mbtBluetoothLE.notifyConnectionStateChanged(BluetoothState.INTERNAL_FAILURE)
        gatt.close()
        msg += "Unknown value $newState"
      }
    }
    LogUtils.d("", msg)
  }

  /**
   * Callback invoked when the list of remote services, characteristics and descriptors
   * for the remote device have been updated, ie new services have been discovered.
   *
   * @param gatt
   * @param status
   */
  override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    super.onServicesDiscovered(gatt, status)

    // Checking if services were indeed discovered or not : getServices should be not null and contains values at this point
    if (gatt.services == null || gatt.services.isEmpty() || status != BluetoothGatt.GATT_SUCCESS) {
      if (mbtBluetoothLE.currentState == BluetoothState.DISCOVERING_SERVICES) mbtBluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_FAILURE)
      gatt.disconnect()
      return
    }

    // Logging all available services
    var isMelomindClassic = false
    for (service in gatt.services) {
      LogUtils.i(TAG, "Found Service with UUID -> " + service.uuid.toString())
      if (service.uuid.toString() == SERVICE_MEASUREMENT.toString()) {
        isMelomindClassic = true
        LogUtils.i(TAG, "indus 2/3 detected")
        mainService = gatt.getService(SERVICE_MEASUREMENT)
        transparentService = null
      }
    }
    if (!isMelomindClassic) {
      LogUtils.i(TAG, "indus 5 detected")
      transparentService = gatt.getService(INDUS_5_TRANSPARENT_SERVICE)
      if (transparentService == null) {
        LogUtils.e(TAG, "Do not found transparent service on this device ${gatt.device.name}")
        gatt.disconnect()
        mbtBluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_FAILURE)
        return
      }
      mainService = null
      Indus5FastMode.setMelomindIndus5()
    }

    if (isIndus5()) {
      initIndus5(gatt)
    } else {
      initIndus23(gatt)
    }

    val initFail: Boolean
    if (isIndus5()) {
      initFail = (indus5TxCharacteristic == null) || (indus5RxCharacteristic == null)
    } else {
      initFail = (mainService == null || measurement == null || battery == null || deviceInfoService == null || fwVersion == null || hwVersion == null || serialNumber == null || oadPacketsCharac == null || mailBox == null || headsetStatus == null)
      if (initFail) {
        LogUtils.e(TAG, "error, not all characteristics have been found")
      }
    }
    if (initFail) {
      gatt.disconnect()
      mbtBluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_FAILURE)
    } else if (mbtBluetoothLE.currentState == BluetoothState.DISCOVERING_SERVICES) {
      LogUtils.e("ConnSteps", "7b : onServicesDiscovered + init ok")
      if (isIndus5()) {
        mbtBluetoothLE.markCurrentStepAsCompletedAndUpdateConnectionState(BluetoothState.INDUS5_DISCOVERING_SUCCESS)
      } else {
        mbtBluetoothLE.updateConnectionState(true) //current state is set to DISCOVERING_SUCCESS and future is completed
      }
    }
  }

  private fun initIndus23(gatt: BluetoothGatt) {
    deviceInfoService = gatt.getService(SERVICE_DEVICE_INFOS)
    if (mainService != null) {
      // Retrieving all relevant characteristics
      measurement = mainService?.getCharacteristic(CHARAC_MEASUREMENT_EEG)
      battery = mainService?.getCharacteristic(CHARAC_MEASUREMENT_BATTERY_LEVEL)
      mailBox = mainService?.getCharacteristic(CHARAC_MEASUREMENT_MAILBOX)
      oadPacketsCharac = mainService?.getCharacteristic(CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER)
      headsetStatus = mainService?.getCharacteristic(CHARAC_HEADSET_STATUS)
      //write no response requested in core.device.oad fw specification
      oadPacketsCharac?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    }
    if (deviceInfoService != null) {
      // Retrieving all relevant characteristics
      fwVersion = deviceInfoService?.getCharacteristic(CHARAC_INFO_FIRMWARE_VERSION)
      hwVersion = deviceInfoService?.getCharacteristic(CHARAC_INFO_HARDWARE_VERSION)
      serialNumber = deviceInfoService?.getCharacteristic(CHARAC_INFO_SERIAL_NUMBER)
      modelNumber = deviceInfoService?.getCharacteristic(CHARAC_INFO_MODEL_NUMBER)
    }
  }

  private fun initIndus5(gatt: BluetoothGatt) {
    indus5TxCharacteristic = transparentService?.getCharacteristic(INDUS_5_TX_CHARACTERISTIC)
    indus5RxCharacteristic = transparentService?.getCharacteristic(INDUS_5_RX_CHARACTERISTIC)
    val charNotified = gatt.setCharacteristicNotification(indus5RxCharacteristic, true)
    Timber.i("setCharacteristicNotification indus5 rx requested = $charNotified")
    val descriptor = indus5RxCharacteristic?.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)
    FlagHelper.setRxDescriptorFlag(false)
    FlagHelper.asyncWaitOperation = MbtAsyncWaitOperation<Boolean>()
    descriptor?.let {
      it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
      val descriptionWritten = gatt.writeDescriptor(it)
      Timber.i("writeDescriptor rx requested = $descriptionWritten")
    }
    FlagHelper.asyncWaitOperation.waitOperationResult(100)
    if (FlagHelper.isRxDescriptorOk()) {
      Timber.i("subscribed RX successfully")
    } else {
      //init fail
      Timber.e("subscribed RX fail")
      indus5RxCharacteristic = null
      indus5TxCharacteristic = null
    }
  }

  /**
   * Callback triggered when gatt.readCharacteristic is called and no failure occured
   *
   * @param gatt           GATT client invoked [BluetoothGatt.readCharacteristic]
   * @param characteristic Characteristic that was read from the associated remote device.
   * @param status         [BluetoothGatt.GATT_SUCCESS] if the read operation was completed successfully.
   */
  override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
    super.onCharacteristicRead(gatt, characteristic, status)
    Log.d(TAG, "on Characteristic Read value: " + if (characteristic.value == null) characteristic.value else Arrays.toString(characteristic.value))
    if (characteristic.uuid.compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0) mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, String(characteristic.value))
    if (characteristic.uuid.compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0) mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, String(characteristic.value))
    if (characteristic.uuid.compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0) mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, String(characteristic.value))
    if (characteristic.uuid.compareTo(CHARAC_INFO_MODEL_NUMBER) == 0) mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.MODEL_NUMBER, String(characteristic.value))
    if (characteristic.uuid.compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
      if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION || characteristic.value == null) {
        LogUtils.w(TAG, "Insufficient authentication to read the battery level")
        mbtBluetoothLE.bond()
      } else {
        var level: Short = -1
        if (characteristic.value != null) {
          if (characteristic.value.size < 4) {
            val sb = StringBuffer()
            for (value in characteristic.value) {
              sb.append(value.toInt())
              sb.append(';')
            }
            LogUtils.e(TAG, """
   Error: received a [onCharacteristicRead] callback for battery level request but the payload of the characteristic is invalid !
   Value(s) received -> $sb
   """.trimIndent())
            return
          }
          level = MelomindDevice.getBatteryPercentageFromByteValue(characteristic.value[0])
          if (level.toInt() == -1) {
            LogUtils.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                    "but the returned value could not be decoded ! " +
                    "Byte value received -> " + characteristic.value[3])
          }
          LogUtils.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
                  "Value -> " + level)
        }
        mbtBluetoothLE.notifyBatteryReceived(level.toInt())
      }
    }
  }

  override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
    super.onCharacteristicWrite(gatt, characteristic, status)
    //Log.d(TAG, "on Characteristic Write value: "+(characteristic.getValue() == null ? characteristic.getValue() : Arrays.toString(characteristic.getValue())) );
    if (isIndus5()) {

    } else {
      mbtBluetoothLE.notifyEventReceived(DeviceCommandEvent.OTA_STATUS_TRANSFER, byteArrayOf(BitUtils.booleanToBit(status == BluetoothGatt.GATT_SUCCESS)))
    }
  }

  override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
    super.onCharacteristicChanged(gatt, characteristic)
    //Log.d(TAG, "on Characteristic Changed value: "+(characteristic.getValue() == null ? characteristic.getValue() : Arrays.toString(characteristic.getValue())) );
    if (isIndus5()) {
//      LogUtils.i("n113", "onCharacteristicChanged : data = ${Arrays.toString(characteristic.value)}")
      //TODO: log and debug n113
      val response = characteristic.value.parseRawIndus5Response()
      when (response) {
        is Indus5Response.MtuChangedResponse -> {
          LogUtils.i("n113", "indus5 mtu changed : byte 2 = ${response.sampleSize}")
          mbtBluetoothLE.onMtuIndus5Changed()
//          mbtBluetoothLE.stopWaitingOperation(false)
        }
        is Indus5Response.EegFrameResponse -> {
          LogUtils.i("n113", "indus5 eeg frame received: data = ${Arrays.toString(characteristic.value)}")
          mbtBluetoothLE.notifyNewDataAcquired(response.data)
        }
        else -> {
          //it should be Indus5Response.UnknownResponse here
          LogUtils.e(TAG, "unknown indus5 frame : data = ${Arrays.toString(characteristic.value)}")
        }
      }
//      characteristic.value
//      mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, String(characteristic.value))
    } else {
      if (characteristic.uuid.compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG) == 0) {
        mbtBluetoothLE.notifyNewDataAcquired(characteristic.value)
      } else if (characteristic.uuid.compareTo(MelomindCharacteristics.CHARAC_HEADSET_STATUS) == 0) {
        mbtBluetoothLE.notifyNewHeadsetStatus(characteristic.value)
      } else if (characteristic.uuid.compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX) == 0) {
        onMailboxEventReceived(characteristic)
        //mbtBluetoothLE.stopWaitingOperation(true, false);
      }
    }
  }

  override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
    super.onDescriptorRead(gatt, descriptor, status)
  }

  override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
    super.onDescriptorWrite(gatt, descriptor, status)
    // Check for EEG Notification status
    if (isIndus5()) {
        if (descriptor.uuid == indus5RxCharacteristic?.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)?.uuid) {
          Timber.i("onDescriptorWrite : status = $status")
          if (status == 0) {
            FlagHelper.setRxDescriptorFlag(true)
            FlagHelper.asyncWaitOperation.stopWaitingOperation(true)
          }
        }
    } else {
      LogUtils.i(TAG, "Received a [onDescriptorWrite] callback with status " + if (status == BluetoothGatt.GATT_SUCCESS) "SUCCESS" else "FAILURE")
      mbtBluetoothLE.stopWaitingOperation(status == BluetoothGatt.GATT_SUCCESS)
      mbtBluetoothLE.onNotificationStateChanged(status == BluetoothGatt.GATT_SUCCESS, descriptor.characteristic, descriptor.value == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    }
  }

  override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
    super.onReliableWriteCompleted(gatt, status)
  }

  override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
    super.onReadRemoteRssi(gatt, rssi, status)
  }

  override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
    super.onMtuChanged(gatt, mtu, status)
    Timber.e("n113 : onMtuChanged : mtu = $mtu ; status = $status")
    LogUtils.e("ConnSteps", "8b : mtu changed")
    mbtBluetoothLE.stopWaitingOperation(mtu)
  }

  /**
   * Notifies that the connected headset returned a response after a characteristic writing operation
   * @param characteristic
   */
  private fun onMailboxEventReceived(characteristic: BluetoothGattCharacteristic) {
    //Log.d(TAG, "Mailbox event received " + Arrays.toString(characteristic.getValue()));
    val response = CommandUtils.deserialize(characteristic.value)
    val mailboxEvent = characteristic.value[0]
    val event = DeviceCommandEvent.getEventFromIdentifierCode(mailboxEvent)
    if (event == null) {
      LogUtils.e(TAG, "Event $mailboxEvent not found ")
      return
    }
    when (event) {
      DeviceCommandEvent.MBX_CONNECT_IN_A2DP, DeviceCommandEvent.MBX_DISCONNECT_IN_A2DP, DeviceCommandEvent.MBX_SET_SERIAL_NUMBER, DeviceCommandEvent.MBX_SET_PRODUCT_NAME, DeviceCommandEvent.MBX_SYS_GET_STATUS, DeviceCommandEvent.MBX_SYS_REBOOT_EVT, DeviceCommandEvent.MBX_SET_NOTCH_FILT, DeviceCommandEvent.MBX_SET_AMP_GAIN, DeviceCommandEvent.MBX_GET_EEG_CONFIG, DeviceCommandEvent.MBX_P300_ENABLE, DeviceCommandEvent.MBX_DC_OFFSET_ENABLE, DeviceCommandEvent.MBX_OTA_MODE_EVT -> notifyResponseReceived(event, response)
      DeviceCommandEvent.MBX_OTA_STATUS_EVT, DeviceCommandEvent.MBX_OTA_IDX_RESET_EVT -> mbtBluetoothLE.notifyEventReceived(event, response)
      DeviceCommandEvent.MBX_SET_ADS_CONFIG, DeviceCommandEvent.MBX_SET_AUDIO_CONFIG, DeviceCommandEvent.MBX_LEAD_OFF_EVT, DeviceCommandEvent.MBX_BAD_EVT -> {
      }
      else -> {
      }
    }
  }

  private fun notifyResponseReceived(event: DeviceCommandEvent, response: ByteArray) {
    if (isMailboxEventFinished(event, response)) {
      if (isConnectionMailboxEvent(event)) mbtBluetoothLE.notifyConnectionResponseReceived(event, response[0]) //connection and disconnection response are composed of only one byte
      mbtBluetoothLE.stopWaitingOperation(response)
    }
  }

  /**
   * Return true if the mailboxEvent if the Bluetooth connection or disconnection event is finished (no more reponse will be received)
   * @param mailboxEvent mailbox command identifier
   */
  fun isConnectionMailboxEvent(mailboxEvent: DeviceCommandEvent): Boolean {
    return mailboxEvent == DeviceCommandEvent.MBX_DISCONNECT_IN_A2DP || mailboxEvent == DeviceCommandEvent.MBX_CONNECT_IN_A2DP
  }

  /**
   * Return true if the mailboxEvent if the Bluetooth connection or disconnection event is finished (no more reponse will be received)
   * The SDK waits until timeout if a mailbox response is received for a command that is not finished
   * @param mailboxEvent mailbox command identifier
   */
  private fun isMailboxEventFinished(mailboxEvent: DeviceCommandEvent, response: ByteArray): Boolean {
    val inProgressResponseCode = MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS)
    val linkkeyInvalidResponseCode = MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID)
    return (mailboxEvent != MBX_CONNECT_IN_A2DP //the connect a2dp command is the only command where the headset returns several responses
        || (inProgressResponseCode?.let { BitUtils.areByteEquals(it, response[0]) }  == false//wait another response until timeout if the connection is not in progress
        && linkkeyInvalidResponseCode?.let { BitUtils.areByteEquals(it, response[0]) } == false)) //wait another response until timeout if the linkkey invalid response is returned
  }
  private val TAG = this::class.java.simpleName

  //----------------------------------------------------------------------------
  // indus 5 new functions
  //----------------------------------------------------------------------------

  fun isIndus5(): Boolean {
    return (transparentService != null)
  }



}