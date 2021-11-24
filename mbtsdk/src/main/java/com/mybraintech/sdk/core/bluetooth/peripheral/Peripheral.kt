package com.mybraintech.sdk.core.bluetooth.peripheral

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.mybraintech.sdk.core.bluetooth.central.IBluetoothUsage
import com.mybraintech.sdk.core.bluetooth.peripheral.peripheralgateway.IPeripheralGateway
import com.mybraintech.sdk.core.bluetooth.peripheral.peripheralgateway.PeripheralGatewayPostIndus5
import com.mybraintech.sdk.core.bluetooth.peripheral.peripheralgateway.PeripheralGatewayPreIndus5
import no.nordicsemi.android.ble.BleManager

// BluetoothDevice
class Peripheral(
  private val bleUsage: IBluetoothUsage
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

  private lateinit var gateway: IPeripheralGateway

  /******************** Callbacks ********************/

  //----------------------------------------------------------------------------
  // Initialization
  //----------------------------------------------------------------------------

  init {
//    if (isPreIndus5) {
//      gateway = PeripheralGatewayPreIndus5()
//    } else {
//      gateway = PeripheralGatewayPostIndus5()
//    }
  }

  //----------------------------------------------------------------------------
  // Update
  //----------------------------------------------------------------------------

  //----------------------------------------------------------------------------
  // Commands
  //----------------------------------------------------------------------------

  fun requestBatteryLevel() {
    bleUsage.readBatteryLevelMbt()
  }
}