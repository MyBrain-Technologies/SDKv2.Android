package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralcommunication

interface IPeripheralCommunicable {

  //----------------------------------------------------------------------------
  // MARK: - Connections
  //----------------------------------------------------------------------------

  fun requestPairing()

  fun requestConnectA2DP()

  //----------------------------------------------------------------------------
  // MARK: - Read
  //----------------------------------------------------------------------------

  fun readDeviceState()

  fun readDeviceInformation()

  //----------------------------------------------------------------------------
  // MARK: - Write
  //----------------------------------------------------------------------------

  fun writeSerialNumber(serialNumber: String)

  fun writeA2dpName(a2dpName: String)

  fun write(firmwareVersion: ByteArray, numberOfBlocks: Short)

  fun writeA2DPConnection()

  fun write(oadBuffer: ByteArray)

  fun write(mtuSize: Byte)

  //----------------------------------------------------------------------------
  // MARK: - Notify
  //----------------------------------------------------------------------------

  fun notifyMailBox(value: Boolean)

  fun notifyBrainActivityMeasurement(value: Boolean)

  fun notifyHeadsetStatus(value: Boolean)

  fun notifyAccelerometerMeasurement(value: Boolean)
}