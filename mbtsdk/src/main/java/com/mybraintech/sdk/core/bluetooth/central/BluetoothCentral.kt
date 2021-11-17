package com.mybraintech.sdk.core.bluetooth.central

class BluetoothCentral {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  var isScanning: Boolean = false

  // TODO: Find right type for this set
  private var discoveredPheripherals: MutableSet<Any> = HashSet()

//  private val peripheralValidator = PeripheralValidator()


  /******************** Callbacks ********************/

//  var onDiscoverPeripheral: ((CBPeripheral) -> Void)? = null
//  var onConnectToPeripheral: ((PeripheralResult) -> Void)? = null
  var onError: ((Error) -> Void)? = null
//  var onDisconnect: ((CBPeripheral, Error?) -> Void)? = null

  //----------------------------------------------------------------------------
  // MARK: - Initialization
  //----------------------------------------------------------------------------

  init {

  }

  //----------------------------------------------------------------------------
  // MARK: - Scanning
  //----------------------------------------------------------------------------

  fun scan() {

    discoveredPheripherals.clear()

  }

  fun stopScanning() {

  }

  // TODO: Add parameters
  fun handleNewDiscoveredPeripheral() {
    // Use peripheralValidator
  }

  //----------------------------------------------------------------------------
  // MARK: - Connection
  //----------------------------------------------------------------------------

  fun connect() {

  }

  fun disconnect() {

  }

  private fun handleConnectionFailure() {
    // ...

    //onError.invoke()
  }
}