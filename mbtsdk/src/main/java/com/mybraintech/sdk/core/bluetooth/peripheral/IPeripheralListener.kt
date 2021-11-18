package com.mybraintech.sdk.core.bluetooth.peripheral

import com.mybraintech.sdk.core.bluetooth.deviceinformation.DeviceInformation

interface IPeripheralListener {
  fun onBatteryValueUpdate(batteryLevel: Float)
  fun onBrainValueUpdate(brainData: ByteArray)
  fun onImsValueUpdate(imsData: ByteArray)
  fun onValueUpdate(saturationStatus: Int)
  fun onUpdate(sampleBufferSizeFromMtu: Int)
  fun onRequestA2DPConnection()
  fun onA2DPConnect()
  fun onA2DPDisconnect(error: Error?)
  fun onConnect()
  fun onConnect(deviceInformation: DeviceInformation)
  fun onFail(error: Error)

}