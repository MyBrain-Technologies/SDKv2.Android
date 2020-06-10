package core.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import command.CommandInterface.MbtCommand
import engine.clientevents.BaseError
import engine.clientevents.BluetoothError

interface BluetoothInterfaces {
  interface IDataBluetooth : IScan, IBattery, ICommand
  interface IAudioBluetooth { //TODO add method for audio unit
    //IStream{
    //startStream ( play(track, volume, duration));
    //pauseStream( pause());
    //stopStream( stop());
    // }
  }

  /**
   * Interface used to connect to or disconnect from a bluetooth peripheral device
   */
  interface IConnect {
    /**
     * ConnectRequestEvent to the peripheral device
     * @return true upon success, false otherwise
     */
    fun connect(context: Context, device: BluetoothDevice): Boolean

    /**
     * Disconnect from the peripheral device
     * @return true upon success, false otherwise
     */
    fun disconnect(): Boolean

    /**
     * Method used to notify that the connection state has changed
     * @param newState The new bluetooth connection state. Refer to @[BluetoothState]
     * for the complete list of states.
     */
    fun notifyConnectionStateChanged(newState: BluetoothState)

    /**
     * @return whether or not the device is correctly connected, ie if current state is [BluetoothState.CONNECTED_AND_READY]
     */
    val isConnected: Boolean
  }

  /**
   * Interface used to connect to or disconnect from a bluetooth peripheral device
   */
  interface IScan {
    /**
     * Start a classic discovery scan in order to find a bluetooth device
     * @param
     */
    fun startScan(): Boolean

    /**
     * Disconnect from the peripheral device
     */
    fun stopScan()
  }

  /**
   * Created by Etienne on 08/02/2018.
   *
   * Interface used to start or stop streaming operations from a bluetooth peripheral device.
   * Streaming means here acquisition in real time of data from the peripheral, using supported bluetooth mechanisms.
   */
  interface IStream {
    /**
     * Start a stream operation
     * @return
     */
    fun startStream(): Boolean

    /**
     * Stop a stream operation
     * @return
     */
    fun stopStream(): Boolean
    interface DataStreamListener {
      fun onNewData()
    }

    /**
     * Called whenever the stream state has changed.
     * @param streamState the new StreamState
     */
    fun notifyStreamStateChanged(streamState: StreamState)

    /**
     *
     * @return true is streaming is in progress, false otherwise.
     */
    val isStreaming: Boolean
  }

  interface ICommand {
    fun sendCommand(command: MbtCommand<BaseError>)
  }

  interface IBattery {
    fun readBattery(): Boolean
  }

  interface IDeviceInfoMonitor {
    fun readFwVersion(): Boolean
    fun readHwVersion(): Boolean
    fun readSerialNumber(): Boolean
    fun readModelNumber(): Boolean
  }
}