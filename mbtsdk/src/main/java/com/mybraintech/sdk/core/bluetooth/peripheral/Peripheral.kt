package com.mybraintech.sdk.core.bluetooth.peripheral

import android.bluetooth.BluetoothDevice

// BluetoothDevice
class Peripheral(
  private val peripheral: BluetoothDevice,
  val isPreIndus5: Boolean
) {

  //----------------------------------------------------------------------------
  // Properties
  //----------------------------------------------------------------------------


  /******************** State ********************/

  val isBleConnected: Boolean
    get() {
      return false
    }

  val isA2dpConnected: Boolean
    get() {
      return false
    }

  /******************** Notification ********************/

  var isListeningEeg: Boolean = false
    set(value) {
      println("isListeningEeg: $value")
      field = value
    }

  var isListeningIms: Boolean = false
    set(value) {
      println("isListeningEeg: $value")
      field = value
    }

  var isListeningToHeadsetStatus: Boolean = false
    set(value) {
      println("isListeningEeg: $value")
      field = value
    }

  /******************** Gateway ********************/

  /******************** Callbacks ********************/

  //----------------------------------------------------------------------------
  // Initialization
  //----------------------------------------------------------------------------

  init {

  }

  //----------------------------------------------------------------------------
  // Update
  //----------------------------------------------------------------------------

  //----------------------------------------------------------------------------
  // Commands
  //----------------------------------------------------------------------------

  fun requestBatteryLevel() {

  }
}