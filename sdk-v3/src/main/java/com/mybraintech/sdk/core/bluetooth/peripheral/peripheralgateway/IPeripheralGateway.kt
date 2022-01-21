package com.mybraintech.sdk.core.bluetooth.peripheral.peripheralgateway

import android.bluetooth.BluetoothGattCharacteristic
import com.mybraintech.sdk.core.bluetooth.deviceinformation.DeviceInformation
import com.mybraintech.sdk.core.bluetooth.peripheral.IPeripheralListener
import com.mybraintech.sdk.core.bluetooth.peripheral.Peripheral
import java.lang.Error

interface IPeripheralGateway {

  //----------------------------------------------------------------------------
  // Properties
  //----------------------------------------------------------------------------

  /******************** State ********************/

  var isReady: Boolean

  /******************** Information ********************/

  // Read only
  val information: DeviceInformation?

//  val deviceInformationBuilder: DeviceInformationBuilder

  /******************** Listener ********************/

  val peripheralListiner: IPeripheralListener?


  /******************** A2DP ********************/

  var isA2dpConnected: Boolean

  var ad2pName: String?

//  #warning("Use interface to hide it.")
//  var peripheralCommunicator: PeripheralCommunicable? { get }
//
//  var allIndusServiceCBUUIDs: [CBUUID] { get }

  //----------------------------------------------------------------------------
  // Initialization
  //----------------------------------------------------------------------------

  // TODO: How to ad custom constructor?
//  constructor(peripheral: Peripheral)

  //----------------------------------------------------------------------------
  // Discovering
  //----------------------------------------------------------------------------

  fun discover(characteristic: BluetoothGattCharacteristic)

  //----------------------------------------------------------------------------
  // Commands
  //----------------------------------------------------------------------------

  fun requestBatteryLevel()

  //----------------------------------------------------------------------------
  // Gateway
  //----------------------------------------------------------------------------

  fun handleValueUpdate(characteristic: BluetoothGattCharacteristic,
                        error: Error?)

  fun handleNotificationStateUpdate(characteristic: BluetoothGattCharacteristic,
                                    error: Error?)

  fun handleValueWrite(characteristic: BluetoothGattCharacteristic,
                       error: Error?)
}