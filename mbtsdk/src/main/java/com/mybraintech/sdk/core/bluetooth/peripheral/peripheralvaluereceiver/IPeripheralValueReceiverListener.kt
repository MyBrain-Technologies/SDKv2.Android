package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralvaluereceiver

interface IPeripheralValueReceiverListener {
  fun didUpdateBatteryLevel(batteryLevel: Float)
  fun didUpdateBrainData(brainData: ByteArray)
//  fun didUpdate(imsData: Data)
//  fun didUpdate(saturationStatus: Int)
//  fun didUpdate(productName: String)
//  fun didUpdate(serialNumber: String)
//  fun didUpdate(firmwareVersion: String)
//  fun didUpdate(hardwareVersion: String)
//  fun didUpdate(sampleBufferSizeFromMtu: Int)
//  fun didA2DPConnectionRequestSucceed()
//  fun didRequestPairing()
//  fun didPair()
//  fun didFail(with error: Error)
}