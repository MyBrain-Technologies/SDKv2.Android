package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralcommunication

import android.bluetooth.BluetoothDevice
import com.mybraintech.sdk.core.bluetooth.peripheral.mailbox.MailboxCommand
import java.lang.UnsupportedOperationException

class PostIndus5PeripheralCommunicator(
  private val peripheral: BluetoothDevice
): IPeripheralCommunicable {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  /******************** Peripheral ********************/

//  private lateinit var peripheral: BluetoothDevice

//  private lateinit var characteristicContainer:
//    PostIndus5CharacteristicContainer

  //----------------------------------------------------------------------------
  // MARK: - Initialization
  //----------------------------------------------------------------------------


  //----------------------------------------------------------------------------
  // MARK: - Communication
  //----------------------------------------------------------------------------

  private fun sendMailBoxCommand(bytes: ByteArray) {

//    peripheral.write...

//    let dataToWrite = Data(bytes)
//    peripheral.writeValue(dataToWrite,
//                          for: characteristicContainer.tx,
//                          type: type)
  }

  //----------------------------------------------------------------------------
  // MARK: - Connections
  //----------------------------------------------------------------------------

  override fun requestPairing() {
    TODO("Not yet implemented")
  }

  override fun requestConnectA2DP() {
    TODO("Not yet implemented")
  }

  //----------------------------------------------------------------------------
  // MARK: - Read
  //----------------------------------------------------------------------------

  // Read battery level
  override fun readDeviceState() {
    val bytes = byteArrayOf(MailboxCommand.BatteryLevel.raw) // [0x20]
    sendMailBoxCommand(bytes)
  }

  override fun readDeviceInformation() {
    TODO("Not yet implemented")
  }

  //----------------------------------------------------------------------------
  // MARK: - Write
  //----------------------------------------------------------------------------

  override fun writeSerialNumber(serialNumber: String) {
    TODO("Not yet implemented")
  }

  override fun writeA2dpName(a2dpName: String) {
    TODO("Not yet implemented")
  }

  override fun write(firmwareVersion: ByteArray, numberOfBlocks: Short) {
    TODO("Not yet implemented")
  }

  override fun write(oadBuffer: ByteArray) {
    TODO("Not yet implemented")
  }

  override fun write(mtuSize: Byte) {
    TODO("Not yet implemented")
  }

  override fun writeA2DPConnection() {
    TODO("Not yet implemented")
  }

  //----------------------------------------------------------------------------
  // MARK: - Notify
  //----------------------------------------------------------------------------

  override fun notifyMailBox(value: Boolean) {
    TODO("Not yet implemented")
  }

  override fun notifyBrainActivityMeasurement(value: Boolean) {
    TODO("Not yet implemented")
  }

  override fun notifyHeadsetStatus(value: Boolean) {
    TODO("Not yet implemented")
  }

  override fun notifyAccelerometerMeasurement(value: Boolean) {
    TODO("Not yet implemented")
  }

}