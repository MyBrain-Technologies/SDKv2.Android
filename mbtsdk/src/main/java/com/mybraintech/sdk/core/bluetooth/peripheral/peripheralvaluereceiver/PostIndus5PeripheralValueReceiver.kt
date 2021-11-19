package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PostIndus5Characteristic
import com.mybraintech.sdk.core.bluetooth.peripheral.mailbox.MailboxCommand
import com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver.batterylevel.PostIndus5BatteryLevelDecoder
import java.lang.Exception

class PostIndus5PeripheralValueReceiver: IPeripheralValueReceiver {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  /******************** Decoder ********************/

  private val batteryLevelDecoder = PostIndus5BatteryLevelDecoder()

  /******************** Error ********************/

  // TODO: Anh Tuan is it a good way to create custom error for the callbacks?
  class ReadError(message: String): Exception(message)
  class MbtCharacteristicConversionError(message: String): Exception(message)
  class InvalidDataError(message: String): Exception(message)
  class MbtMailboxConversionError(message: String): Exception(message)

  /******************** Listener ********************/

  override var listener: IPeripheralValueReceiverListener? = null

  //----------------------------------------------------------------------------
  // MARK: - Notification
  //----------------------------------------------------------------------------

  override fun handleValueWriteFor(characteristic: BluetoothGattCharacteristic,
                                   status: Int) {
    TODO("Not yet implemented")
  }

  //----------------------------------------------------------------------------
  // MARK: - Write
  //----------------------------------------------------------------------------

  override fun handleNotificationStateUpdateFor(
    characteristic: BluetoothGattCharacteristic,
    status: Int
  ) {
    // Not used so far
  }


  //----------------------------------------------------------------------------
  // MARK: - Read
  //----------------------------------------------------------------------------

  override fun handleValueUpdateFor(
    characteristic: BluetoothGattCharacteristic,
    status: Int
  ) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      val error = ReadError("Invalid read status $status")
      listener?.onError(error)
    }

    val characteristicUUID = characteristic.uuid
    val mbtCharacteristic =
      PostIndus5Characteristic.ofRaw(characteristicUUID) ?: run {
        val error =
          MbtCharacteristicConversionError("Invalid characteristic uuid")
        listener?.onError(error)
        return
      }

    val characteristicData = characteristic.value

    when(mbtCharacteristic) {
      PostIndus5Characteristic.Tx -> handleTxUpdate(characteristicData)
      PostIndus5Characteristic.Rx -> handleRxUpdate(characteristicData)
      PostIndus5Characteristic.Unknown -> {
        println("Unknown Characteristic")
        return
      }
    }
  }

  private fun handleTxUpdate(data: ByteArray) {
    TODO("Not implemented")
  }

  private fun handleRxUpdate(data: ByteArray) {
    if (data.size < 2) {
      val error = InvalidDataError("Invalid data size")
      listener?.onError(error)
    }
    val opCode = data[0]
    val parameterBytes = data.drop(1).toByteArray()

    val mailboxCommand =
      MailboxCommand.ofRaw(opCode) ?: run {
        val error = MbtMailboxConversionError("Invalid mailbox opcode")
        listener?.onError(error)
        return
      }

    when(mailboxCommand) {
      MailboxCommand.SetProductName -> TODO()
      MailboxCommand.StartOTATFX -> TODO()
      MailboxCommand.OtaModeEvent -> TODO()
      MailboxCommand.OtaIndexResetEvent -> TODO()
      MailboxCommand.OtaStatusEvent -> TODO()
      MailboxCommand.SystemGetStatus -> TODO()
      MailboxCommand.SystemRebootEvent -> TODO()
      MailboxCommand.SetSerialNumber -> TODO()
      MailboxCommand.SetA2dpName -> TODO()
      MailboxCommand.SetNotchFilter -> TODO()
      MailboxCommand.SetBandpassFilter -> TODO()
      MailboxCommand.SetAmplifierSignalGain -> TODO()
      MailboxCommand.GetEEGConfig -> TODO()
      MailboxCommand.ToggleP300 -> TODO()
      MailboxCommand.EnableDCOffset -> TODO()
      MailboxCommand.A2dpConnection -> TODO()
      MailboxCommand.BatteryLevel -> handleBatteryUpdate(parameterBytes)
      MailboxCommand.SerialNumber -> TODO()
      MailboxCommand.DeviceId -> TODO()
      MailboxCommand.StartEeg -> TODO()
      MailboxCommand.StopEeg -> TODO()
      MailboxCommand.Reset -> TODO()
      MailboxCommand.FirmewareVersion -> TODO()
      MailboxCommand.HardwareVersion -> TODO()
      MailboxCommand.MtuSize -> TODO()
      MailboxCommand.GetFilterConfigurationType -> TODO()
      MailboxCommand.SetFilterConfigurationType -> TODO()
      MailboxCommand.SetAudioconfig -> TODO()
      MailboxCommand.StartImsAcquisition -> TODO()
      MailboxCommand.StopImsAcquisition -> TODO()
      MailboxCommand.StartPpgAcquisition -> TODO()
      MailboxCommand.StopPpgAcquisition -> TODO()
      MailboxCommand.StartTemperatureAcquisition -> TODO()
      MailboxCommand.StopTemperatureAcquisition -> TODO()
      MailboxCommand.SetImsConfiguration -> TODO()
      MailboxCommand.GetSensorStatus -> TODO()
      MailboxCommand.SetPpgConfiguration -> TODO()
      MailboxCommand.ImsDataFrameEvent -> TODO()
      MailboxCommand.PpgDataFrameEvent -> TODO()
      MailboxCommand.EegDataFrameEvent -> TODO()
      MailboxCommand.UnknownEvent -> TODO()
    }

  }


  //----------------------------------------------------------------------------
  // Mailbox handlers
  //----------------------------------------------------------------------------

  /******************** Battery ********************/

  private fun handleBatteryUpdate(bytes: ByteArray) {
    if (bytes.isEmpty()) {
      // TODO: Handle error
      return
    }

    val batteryLevel =
      batteryLevelDecoder.decodeBatteryValue(bytes[0]) ?: return


    listener?.didUpdateBatteryLevel(batteryLevel)
  }

//  MbtMailboxConversionError



}