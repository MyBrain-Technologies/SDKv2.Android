package com.mybraintech.sdk.core.bluetooth.deviceinformation

enum class IndusVersion {

  //----------------------------------------------------------------------------
  // Cases
  //----------------------------------------------------------------------------

  Indus2,
  Indus3,
  Indus5;

  //----------------------------------------------------------------------------
  // Properties
  //----------------------------------------------------------------------------

  val binaryPrefix: String
  get() {
    return when(this) {
      Indus2 -> "mm-ota-"
      Indus3 -> "mm-ota-i3-"
      // TODO: To check if indus5 binary prefix is good
      Indus5 -> "mm-ota-i5-"
    }
  }

  // TODO
  val binaryNameRegex: String
  get() {
    return ""
//    return "$binaryPrefix$Constants.binaryVersionRegex"
  }

  val hardwareVersion: String
    get() {
      return when(this) {
        Indus2 -> "1.0.0"
        Indus3 -> "1.1.0"
        Indus5 -> "2.0.0"
      }
    }

  //----------------------------------------------------------------------------
  // Init
  //----------------------------------------------------------------------------

  fun toRaw() = enumToRaw[this]

  companion object {
    private val rawToEnum = mapOf(
      "indus2" to Indus2,
      "indus3" to Indus3,
      "indus5" to Indus5,
    )
    val enumToRaw = rawToEnum.entries.associate{(k,v)-> v to k}
    fun ofRaw(raw: String): IndusVersion? {
      return rawToEnum[raw]
    }
  }

}