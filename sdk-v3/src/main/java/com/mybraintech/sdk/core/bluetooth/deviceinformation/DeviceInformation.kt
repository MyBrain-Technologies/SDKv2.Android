package com.mybraintech.sdk.core.bluetooth.deviceinformation

data class DeviceInformation(
  val productName: String,
  val deviceId: String,
  val hardwareVersion: HardwareVersion,
  val firmwareVersion: String,
  val indusVersion: IndusVersion,
  val acquisitionInformation: DeviceAcquisitionInformation =
    DeviceAcquisitionInformation(indusVersion)
) {

  //----------------------------------------------------------------------------
  // Update
  //----------------------------------------------------------------------------
  
  fun isVersionUpToDate(oadFirmwareVersion: String): Boolean {
    return false
    // TODO: Create a classe `FormatedVersion` using KotlinVersion
//    return formattedFirmwareVersion == oadFirmwareVersion
  }
}
