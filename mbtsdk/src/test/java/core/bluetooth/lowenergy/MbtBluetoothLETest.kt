package core.bluetooth.lowenergy

import android.bluetooth.*
import android.content.Context
import command.BluetoothCommands.Mtu
import command.CommandInterface.MbtCommand
import command.DeviceCommandEvent
import command.DeviceCommands
import command.DeviceCommands.Reboot
import config.MbtConfig
import core.MbtManager
import core.bluetooth.*
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_INFO_FIRMWARE_VERSION
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_EEG
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_MAILBOX
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.NOTIFICATION_DESCRIPTOR_UUID
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.SERVICE_MEASUREMENT
import core.bluetooth.spp.MbtBluetoothSPP
import engine.clientevents.BaseError
import engine.clientevents.BluetoothError
import features.MbtDeviceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import utils.MbtAsyncWaitOperation
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class MbtBluetoothLETest {

  /**
   * Class under test
   */
  private lateinit var bluetoothLE: MbtBluetoothLE
  private lateinit var manager: MbtBluetoothManager
  private lateinit var gatt: BluetoothGatt
  private var gattService: BluetoothGattService? = null
  private var characteristic: BluetoothGattCharacteristic? = null
  private var descriptor: BluetoothGattDescriptor? = null
  private lateinit var command: MbtCommand<BaseError>
  private lateinit var asyncOperation: MbtAsyncWaitOperation<Any>
  private val BYTE_REQUEST = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
  private val BYTE_RESPONSE = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
  private val SERVICE = SERVICE_MEASUREMENT
  private val CHARACTERISTIC_MAILBOX = CHARAC_MEASUREMENT_MAILBOX
  private val CHARACTERISTIC_EEG = CHARAC_MEASUREMENT_EEG
  private val DESCRIPTOR = NOTIFICATION_DESCRIPTOR_UUID
  private val UNKNOWN = UUID.fromString("0-0-0-0-0")
  private val DEVICE_NAME = "melo_1010100100"

  @Before
  fun setUp() {
    initMelomind()
    bluetoothLE = MbtBluetoothLE(manager)
    MbtDataBluetooth.instance = bluetoothLE
    gatt = Mockito.mock(BluetoothGatt::class.java)
   bluetoothLE.gatt = gatt
    gattService = Mockito.mock(BluetoothGattService::class.java)
    characteristic = Mockito.mock(BluetoothGattCharacteristic::class.java)
    descriptor = Mockito.mock(BluetoothGattDescriptor::class.java)
    command = Mockito.mock(MbtCommand::class.java) as MbtCommand<BaseError>
    asyncOperation = Mockito.mock(MbtAsyncWaitOperation::class.java) as MbtAsyncWaitOperation<Any>
    bluetoothLE.setLock(asyncOperation)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
  }
  fun initMelomind(){
    val context = Mockito.mock(Context::class.java)
    manager = MbtBluetoothManager(context)
    val applicationContext = Mockito.mock(Context::class.java)
    val bluetoothManager = Mockito.mock(BluetoothManager::class.java)
    val bluetoothAdapter = Mockito.mock(BluetoothAdapter::class.java)
    manager.context = BluetoothContext(context,
        MbtDeviceType.MELOMIND,
        true,
        "melo_12345678",
        "MM102345678",
        47
    )
    Mockito.`when`(context.applicationContext).thenReturn(applicationContext)
    Mockito.`when`(context.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    Mockito.`when`(applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    Mockito.`when`(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    manager.initBluetoothOperators()
  }
  
  //
  //    @Test
  //    public void startStream_MockGatt() {
  //        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
  //        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
  //        when(gatt.getServices()).thenReturn(Arrays.asList(gattService));
  //        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
  //        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
  //        when(gattService.getCharacteristics()).thenReturn(Arrays.asList(characteristic));
  //        bluetoothLE.setGatt(gatt;
  //        assertTrue(bluetoothLE.startStream());
  //
  //    }
  /**
   * Assert that streaming cannot be started if headset is idle
   */
  @Test
  fun startStream_Idle() {
    bluetoothLE.notifyStreamStateChanged(StreamState.IDLE)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming state become IDLE if headset is disconnected
   */
  @Test
  fun startStream_StreamStoppedAfterDisconnection() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED)
    Assert.assertFalse(bluetoothLE.isStreaming)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset is disconnected
   */
  @Test
  fun startStream_Disconnected() {
    bluetoothLE.notifyStreamStateChanged(StreamState.DISCONNECTED)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming return true if the stream state is started
   */
  @Test
  fun startStream_Started() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    Assert.assertTrue(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if streaming has just been stopped
   */
  @Test
  fun startStream_Stopped() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if gatt is null
   */
  @Test
  fun startStream_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset is connected BUT not ready
   */
  @Test
  fun startStream_HeadsetNotReady() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection is in progress
   */
  @Test
  fun startStream_Connecting() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTING)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset disconnection is in progress
   */
  @Test
  fun startStream_Disconnecting() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCONNECTING)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection that was in progress has failed
   */
  @Test
  fun startStream_ConnectionFailed() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection that was in progress has failed because Bluetooth is off on the user's mobile device
   */
  @Test
  fun startStream_NotConnectedBluetoothDisabled() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.BLUETOOTH_DISABLED)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection that was in progress has failed because Bluetooth is not supported by the user's mobile device
   */
  @Test
  fun startStream_NotConnectedBluetoothNotSupported() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.NO_BLUETOOTH)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection that was in progress has failed because Location is off on the user's mobile device
   */
  @Test
  fun startStream_NotConnectedLocationDisabled() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.LOCATION_DISABLED)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection that was in progress has failed because Bluetooth is off on the user's mobile device
   */
  @Test
  fun startStream_NotConnectedLocationPermissionNotGranted() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.LOCATION_PERMISSION_NOT_GRANTED)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection was interrupted by the user
   */
  @Test
  fun startStream_ConnectionInterrupted() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTION_INTERRUPTED)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if connection process is still in progress, at the services discovering step
   */
  @Test
  fun startStream_DiscoveringServices() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_SERVICES)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection has failed while it was discovering services
   */
  @Test
  fun startStream_DiscoveringFailed() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_FAILURE)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if connection process is still in progress, at the device info reading step
   */
  @Test
  fun startStream_ReadingDeviceInfo() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.READING_FIRMWARE_VERSION)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection has failed while it was reading device info
   */
  @Test
  fun startStream_ReadingFailed() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.READING_FAILURE)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if connection process is still in progress, at the bonding step
   */
  @Test
  fun startStream_Bonding() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.BONDING)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if headset connection has failed while it was requesting bonding
   */
  @Test
  fun startStream_BondingFailed() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.BONDING_FAILURE)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if firmware version upgrading is in progress
   */
  @Test
  fun startStream_Upgrading() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.UPGRADING)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that streaming cannot be started if firmware version upgrade has failed
   */
  @Test
  fun startStream_UpgradingFailed() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.UPGRADING_FAILURE)
    Assert.assertFalse(bluetoothLE.startStream())
  }

  /**
   * Assert that status monitoring can not be activated if gatt is null
   */
  @Test
  fun activateDeviceStatusMonitoring_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.activateDeviceStatusMonitoring())
  }

  /**
   * Assert that status monitoring can not be activated if headset is not connected
   */
  @Test
  fun activateDeviceStatusMonitoring_Disconnected() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED)
    Assert.assertFalse(bluetoothLE.activateDeviceStatusMonitoring())
  }

  /**
   * Check that nothing happens if user requests stopping streaming whereas any streaming was in progress and the current state was IDLE
   */
  @Test
  fun stopStream_NotStreamingIdle() {
    bluetoothLE.notifyStreamStateChanged(StreamState.IDLE)
    Assert.assertTrue(bluetoothLE.stopStream())
  }

  /**
   * Check that nothing happens if user requests stopping streaming whereas the headset is disconnected
   */
  @Test
  fun stopStream_NotStreamingDisconnected() {
    bluetoothLE.notifyStreamStateChanged(StreamState.DISCONNECTED)
    Assert.assertTrue(bluetoothLE.stopStream())
  }

  /**
   * Check that nothing happens if user requests stopping streaming whereas it was already stopped
   */
  @Test
  fun stopStream_StreamingAlreadyStopped() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED)
    Assert.assertTrue(bluetoothLE.stopStream())
  }

  /**
   * Check that a streaming in progress is not stopped if service is not found
   */
  @Test
  fun stopStream_UnknownService() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(null)
    bluetoothLE.gatt = gatt
    Assert.assertFalse(bluetoothLE.stopStream())
  }

  /**
   * Check that a streaming in progress is not stopped if characteristic is not found
   */
  @Test
  fun stopStream_UnknownCharacteristic() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(null)
    bluetoothLE.gatt = gatt
    Assert.assertFalse(bluetoothLE.stopStream())
  }

  /**
   * Check that a streaming in progress is not stopped if gatt is not found
   */
  @Test
  fun stopStream_NullGatt() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.stopStream())
  }

  /**
   * Check that nothing happens if user requests stopping streaming whereas notification disabling is not working (streaming is in progress and service & characteristic are valid)
   */
  @Test
  fun stopStream_DisablingNotificationFail() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY)
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(null)
    bluetoothLE.gatt = gatt
    Assert.assertFalse(bluetoothLE.stopStream())
  }

  /**
   * Check that streaming in progress is stopped if service & characteristic are valid and notification disabling succeeded
   */
  @Test
  fun stopStream_DisableNotificationWorking() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
    Mockito.`when`(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), false)).thenReturn(true)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
    bluetoothLE.gatt = gatt
    //assertNull(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    //assertTrue(bluetoothLE.stopStream());
  }

  /**
   * Check that streaming is in progress if stream has started
   */
  @Test
  fun notifyStreamStateChanged_Started() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    Assert.assertTrue(bluetoothLE.isStreaming)
  }

  /**
   * Check that streaming is not in progress if stream has stopped
   */
  @Test
  fun notifyStreamStateChanged_Stopped() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Check that streaming is not in progress if stream has failed to start
   */
  @Test
  fun notifyStreamStateChanged_Failed() {
    bluetoothLE.notifyStreamStateChanged(StreamState.FAILED)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Check that streaming is not in progress if stream state is IDLE
   */
  @Test
  fun notifyStreamStateChanged_Idle() {
    bluetoothLE.notifyStreamStateChanged(StreamState.IDLE)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Check that streaming is not in progress if stream state is IDLE
   */
  @Test
  fun notifyStreamStateChanged_Vpro() {
    MbtConfig.setDeviceType(MbtDeviceType.VPRO)
    val bluetoothSPP = MbtBluetoothSPP(manager)
    bluetoothSPP.notifyStreamStateChanged(StreamState.STARTED)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Assert that Bluetooth is not streaming if the streaming failed to start
   */
  @Test
  fun isStreaming_Null(){
      bluetoothLE.notifyStreamStateChanged(StreamState.FAILED)
      Assert.assertFalse(bluetoothLE.isStreaming)
    }

  /**
   * Assert that Bluetooth is streaming if headset is disconnected
   */
  @Test
  fun isStreaming_Disconnected(){
      bluetoothLE.notifyStreamStateChanged(StreamState.DISCONNECTED)
      Assert.assertFalse(bluetoothLE.isStreaming)
    }

  /**
   * Assert that Bluetooth is streaming if stream is started
   */
  @Test
  fun isStreaming_Started(){
      bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
      Assert.assertTrue(bluetoothLE.isStreaming)
    }

  /**
   * Assert that Bluetooth is not streaming if streaming is stopped
   */
  @Test
  fun isStreaming_Stopped(){
      bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED)
      Assert.assertFalse(bluetoothLE.isStreaming)
    }

  /**
   * Assert that bluetooth is not streaming if headset is idle
   */
  @Test
  fun isStreaming_Idle(){
      bluetoothLE.notifyStreamStateChanged(StreamState.IDLE)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  /**
   * Assert that BLE is not streaming if headset is a vpro
   */
  @Test
  fun isStreaming_Vpro(){
      MbtConfig.setDeviceType(MbtDeviceType.VPRO)
      val bluetoothSPP = MbtBluetoothSPP(manager)
      bluetoothSPP.notifyStreamStateChanged(StreamState.STARTED)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  /**
   * Assert that notifications can not be enabled if headset is not connected
   */
  @Test
  fun enableOrDisableNotificationsOnCharacteristic_DisconnectedEnable() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED)
    Assert.assertFalse(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(true,
        gatt.getService(SERVICE_MEASUREMENT).getCharacteristic(CHARAC_MEASUREMENT_EEG)))
  }

  /**
   * Assert that notifications can not be disabled if headset is not connected
   */
  @Test
  fun enableOrDisableNotificationsOnCharacteristic_DisconnectedDisable() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED)
    Assert.assertFalse(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(false,
        gatt.getService(SERVICE_MEASUREMENT).getCharacteristic(CHARAC_MEASUREMENT_EEG)
    ))
  }

  /**
   * Assert that notifications can be enabled if headset is connected and ready
   */
  @Test
  fun enableOrDisableNotificationsOnCharacteristic_ConnectedReadyEnable() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
    Mockito.`when`(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), false)).thenReturn(true)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
    Mockito.`when`(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), true)).thenReturn(true)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true)
    Mockito.`when`(gatt.writeDescriptor(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR))).thenReturn(true)
    Mockito.`when`(asyncOperation.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout())).thenReturn(true)
    bluetoothLE.gatt = gatt
    Assert.assertTrue(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(true, bluetoothLE.gatt?.getService(SERVICE)!!.getCharacteristic(CHARACTERISTIC_EEG)))
  }

  /**
   * Assert that notifications can be disabled if headset is connected and ready
   */
  @Test
  fun enableOrDisableNotificationsOnCharacteristic_ConnectedReadyDisable() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
    Mockito.`when`(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), false)).thenReturn(true)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
    Mockito.`when`(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), true)).thenReturn(true)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true)
    Mockito.`when`(gatt.writeDescriptor(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR))).thenReturn(true)
    bluetoothLE.gatt = gatt
    Mockito.`when`(asyncOperation.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout())).thenReturn(true)
    Assert.assertTrue(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(false, bluetoothLE.gatt?.getService(SERVICE)!!.getCharacteristic(CHARACTERISTIC_EEG)))
  }

  @Test
  fun startLowEnergyScan_FailedNullNameFilterOn() {
    Assert.assertFalse(bluetoothLE.startScan())
    Assert.assertNotEquals(bluetoothLE.currentState, BluetoothState.SCAN_FAILURE)
  }

  @Test
  fun startLowEnergyScan_FailedNotNullNameFilterOn() {
    Assert.assertFalse(bluetoothLE.startScan())
    Assert.assertNotEquals(bluetoothLE.currentState, BluetoothState.SCAN_FAILURE)
  }

  /**
   * Check that the current state is no more SCAN STARTED after stopping the scan
   */
  @Test
  fun stopLowEnergyScan_StateIsNotScanStarted() {
    bluetoothLE.stopScan()
    Assert.assertNotEquals(bluetoothLE.currentState, BluetoothState.SCAN_STARTED)
  }

  /**
   * Check that the current state is IDLE after stopping the scan
   */
  @Test
  fun stopLowEnergyScan_StateIsIdle() {
    bluetoothLE.stopScan()
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  /**
   * Check that the current state is IDLE after stopping the scan
   */
  @Test
  fun stopLowEnergyScan_StartedStoppedFailed() {
    bluetoothLE.startScan()
    bluetoothLE.stopScan()
  }

  @Test
  fun connect_NonNullContext() {
    val device = Mockito.mock(BluetoothDevice::class.java)
    Assert.assertTrue(bluetoothLE.connect(manager.context.context, device))
    Assert.assertNull(bluetoothLE.gatt)
  }

  @Test
  fun connect_NullContext() { //obsolete with kotlin non null input
    val device = Mockito.mock(BluetoothDevice::class.java)
//    Assert.assertFalse(bluetoothLE.connect(null, device))
//    Assert.assertNotNull(bluetoothLE.gatt)
  }

  @Test
  fun connect_NullDevice() { //obsolete with kotlin non null input
//    Assert.assertFalse(bluetoothLE.connect(manager.context.context, null))
//    Assert.assertNotNull(bluetoothLE.gatt)
  }

  /**
   * check that disconnection fail if there is no connected headset
   */
  @Test
  fun disconnect_NoConnectedHeadset() {
    Assert.assertFalse(bluetoothLE.disconnect())
  }

  /**
   * check that disconnection fail if gatt is null
   */
  @Test
  fun disconnect_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.disconnect())
  }

  /**
   * check that gatt is reset to null after disconnection
   */
  @Test
  fun disconnect_GattNotNull() {
    bluetoothLE.gatt = gatt
    Assert.assertFalse(bluetoothLE.disconnect())
    Assert.assertNull(bluetoothLE.gatt)
  }

  @Test
  fun isConnected_ConnectedAndReady(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY, true)
      Assert.assertTrue(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Connected(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Disconnected(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ScanFailed(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.SCAN_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_AudioConnected(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ConnectionFailure(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ConnectionInterrupted(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTION_INTERRUPTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_InternalFailure(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.INTERNAL_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_BluetoothDisabled(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.BLUETOOTH_DISABLED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ReadingDeviceInfo(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.READING_FIRMWARE_VERSION, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ReadingFailure(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.READING_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ScanTimeout(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.SCAN_TIMEOUT, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Bonding(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.BONDING, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_AnotherDeviceConnected(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.ANOTHER_DEVICE_CONNECTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_BondingFailure(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.BONDING_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Connecting(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTING, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_DeviceFound(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DEVICE_FOUND, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Disconnecting(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCONNECTING, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_DiscoveringFailed(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Discovering(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.DISCOVERING_SERVICES, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Idle(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.IDLE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_LocationIsRequired(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.LOCATION_DISABLED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_LocationPermission(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.LOCATION_PERMISSION_NOT_GRANTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_BluetoothNotSupported(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.NO_BLUETOOTH, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ScanFailedAlreadyStarted(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.SCAN_FAILED_ALREADY_STARTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ScanInterrupted(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.SCAN_INTERRUPTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_ScanStarted(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.SCAN_STARTED, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_UpgradingFailed(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.UPGRADING_FAILURE, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun isConnected_Upgrading(){
      bluetoothLE.notifyConnectionStateChanged(BluetoothState.UPGRADING, true)
      Assert.assertFalse(bluetoothLE.isConnected)
    }

  @Test
  fun startReadOperation_ConnectedNotReady() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
    Assert.assertFalse(bluetoothLE.startReadOperation(CHARAC_INFO_FIRMWARE_VERSION))
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  @Test
  fun startReadOperation_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.startReadOperation(CHARAC_INFO_FIRMWARE_VERSION))
  }

  @Test
  fun startReadOperation_GattCharacteristicNull() {
    Assert.assertFalse(bluetoothLE.startReadOperation(UNKNOWN))
  }

  /**
   * Check that startWriteOperation return false if gatt is invalid
   */
  @Test
  fun startWriteOperation_GattInvalid() {
    bluetoothLE.gatt = null
    val code = byteArrayOf(DeviceCommandEvent.MBX_GET_EEG_CONFIG.identifierCode)
    Assert.assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code))
  }

  /**
   * Check that startWriteOperation return false if gatt service is invalid
   */
  @Test
  fun startWriteOperation_ServiceInvalid() {
    val code = byteArrayOf(DeviceCommandEvent.MBX_GET_EEG_CONFIG.identifierCode)
    bluetoothLE.gatt = gatt
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(null)
    Assert.assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code))
  }

  /**
   * Check that startWriteOperation return false if gatt characteristic is invalid
   */
  @Test
  fun startWriteOperation_CharacteristicInvalid() {
    val code = byteArrayOf(DeviceCommandEvent.MBX_GET_EEG_CONFIG.identifierCode)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    val characteristic: BluetoothGattCharacteristic? = null
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic)
    Assert.assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code))
  }

  /**
   * Check that startWriteOperation return false if gatt write characteristic is invalid
   */
  @Test
  fun startWriteOperation_WriteCharacteristicInvalid() {
    val code = byteArrayOf(DeviceCommandEvent.MBX_GET_EEG_CONFIG.identifierCode)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic)
    Mockito.`when`(gatt.writeCharacteristic(characteristic)).thenReturn(false)
    Assert.assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code))
  }

  /**
   * Check that startWriteOperation return true if gatt write characteristic is valid
   */
  @Test
  fun startWriteOperation_Valid() {
    val code = byteArrayOf(DeviceCommandEvent.MBX_GET_EEG_CONFIG.identifierCode)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic)
    Mockito.`when`(gatt.writeCharacteristic(characteristic)).thenReturn(true)
    Assert.assertTrue(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code))
  }

  /**
   * Checks that checkServiceAndCharacteristicValidity returns true if
   * a valid service and a valid characteristic are used.
   */
  @Test
  fun checkServiceAndCharacteristicValidity_Valid() {
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
    Assert.assertTrue(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG))
  }

  /**
   * Checks that checkServiceAndCharacteristicValidity returns false if
   * a invalid service and a valid characteristic are used.
   */
  @Test
  fun checkServiceAndCharacteristicValidity_InvalidService() {
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(null)
    Assert.assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG))
  }

  /**
   * Checks that checkServiceAndCharacteristicValidity returns false if
   * a valid service and a invalid characteristic are used.
   */
  @Test
  fun checkServiceAndCharacteristicValidity_InvalidCharacteristic() {
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(null)
    Assert.assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG))
  }

  /**
   * Checks that checkServiceAndCharacteristicValidity returns false if
   * a invalid gatt is used.
   */
  @Test
  fun checkServiceAndCharacteristicValidity_InvalidGatt() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG))
  }

  /**
   * Check that false is returned if gatt is invalid
   */
  @Test
  fun isNotificationEnabledOnCharacteristic_InvalidGatt(){
      bluetoothLE.gatt = null
      Assert.assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX))
    }

  /**
   * Check that false is returned if service is invalid
   */
  @Test
  fun isNotificationEnabledOnCharacteristic_InvalidService(){
      Mockito.`when`(gatt.getService(SERVICE)).thenReturn(null)
      Assert.assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX))
    }

  /**
   * Check that false is returned if characteristic is invalid
   */
  @Test
  fun isNotificationEnabledOnCharacteristic_InvalidCharacteristic(){
      Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
      Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
      Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(null)
      Assert.assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX))
    }

  /**
   * Check that false is returned if characteristic is invalid
   */
  @Test
  fun isNotificationEnabledOnCharacteristic_InvalidDescriptor(){
      Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
      Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
      Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
      Mockito.`when`(characteristic!!.getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
      Assert.assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX))
    }

  /**
   * Check that true is returned if the descriptor is valid
   */
  @Test
  fun isNotificationEnabledOnCharacteristic_ValidDescriptor(){
      Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
      Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
      Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic)
      Mockito.`when`(characteristic!!.getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
      Mockito.`when`(descriptor!!.value).thenReturn(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
      Assert.assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX))
    }

  @Test
  fun readBattery_ConnectedNotReady() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
    Assert.assertFalse(bluetoothLE.readBattery())
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  @Test
  fun readBattery_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.readBattery())
  }

  @Test
  fun readBattery_GattCharacteristicNull() {
    Assert.assertFalse(bluetoothLE.readBattery())
  }

  @Test
  fun readFwVersion_ConnectedNotReady() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
    Assert.assertFalse(bluetoothLE.readFwVersion())
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  @Test
  fun readFwVersion_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.readFwVersion())
  }

  @Test
  fun readFwVersion_GattCharacteristicNull() {
    Assert.assertFalse(bluetoothLE.readFwVersion())
  }

  @Test
  fun readHwVersion_ConnectedNotReady() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
    Assert.assertFalse(bluetoothLE.readHwVersion())
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  @Test
  fun readHwVersion_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.readHwVersion())
  }

  @Test
  fun readHwVersion_GattCharacteristicNull() {
    Assert.assertFalse(bluetoothLE.readHwVersion())
  }

  @Test
  fun readSerialNumber_ConnectedNotReady() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
    Assert.assertFalse(bluetoothLE.readSerialNumber())
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  @Test
  fun readSerialNumber_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.readSerialNumber())
  }

  @Test
  fun readSerialNumber_GattCharacteristicNull() {
    Assert.assertFalse(bluetoothLE.readSerialNumber())
  }

  @Test
  fun onNotificationStateChanged_started() {
    val characteristic = BluetoothGattCharacteristic(CHARACTERISTIC_EEG, 0, 0)
    bluetoothLE.onNotificationStateChanged(true, characteristic, true)
    //assertTrue(bluetoothLE.isStreaming()); //todo check why characteristic.getuuid is null
  }

  @Test
  fun onNotificationStateChanged_stopped() {
    val characteristic = BluetoothGattCharacteristic(CHARACTERISTIC_EEG, 0, 0)
    bluetoothLE.onNotificationStateChanged(true, characteristic, false)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  @Test
  fun onNotificationStateChanged_failed() {
    val characteristic = BluetoothGattCharacteristic(CHARACTERISTIC_EEG, 0, 0)
    bluetoothLE.onNotificationStateChanged(false, characteristic, false)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Assert that streaming state become IDLE if headset is disconnected
   */
  @Test
  fun notifyConnectionStateChanged_DisconnectionDuringStreaming() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    Assert.assertTrue(bluetoothLE.isStreaming)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED, true)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Assert that streaming state do not change if streaming is in progress and the new state is not equal to DATA_BT_DISCONNECTED
   */
  @Test
  fun notifyConnectionStateChanged_ConnectionWhileStreaming() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STARTED)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS, true)
    Assert.assertTrue(bluetoothLE.isStreaming)
  }

  /**
   * Assert that streaming state do not change if streaming is not progress and the new state is to DATA_BT_DISCONNECTED
   */
  @Test
  fun notifyConnectionStateChanged_DisconnectionDuringStoppedStream() {
    bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED, true)
    Assert.assertFalse(bluetoothLE.isStreaming)
  }

  /**
   * Check that MTU is not changed if gatt is null
   */
  @Test
  fun changeMTU_GattNull() {
    bluetoothLE.gatt = null
    Assert.assertFalse(bluetoothLE.changeMTU(47))
  }

  /**
   * Check that MTU is not changed if gatt requestMtu return false
   */
  @Test
  fun changeMTU_RequestMtuFailure() {
    Mockito.`when`(gatt.requestMtu(Mockito.anyInt())).thenReturn(false)
    Assert.assertFalse(bluetoothLE.changeMTU(47))
  }

  /**
   * Check that MTU is changed if gatt requestMtu return true
   */
  @Test
  fun changeMTU_RequestMtuSuccess() {
    Mockito.`when`(gatt.requestMtu(Mockito.anyInt())).thenReturn(true)
    Assert.assertTrue(bluetoothLE.changeMTU(47))
  }

  /**
   * Check that gatt services cannot be discovered if gatt is disconnected
   */
  @Test
  fun disconnectHeadsetAlreadyConnected_DisconnectionSuccess() {
//        bluetoothLE.setGatt(gatt; //todo mock gatt.getdevice.getname
//        bluetoothLE.disconnectHeadsetAlreadyConnected(DEVICE_NAME,true);
//        assertFalse(bluetoothLE.gatt.discoverServices());
  }

  /**
   * Check that gatt services cannot be discovered if gatt is disconnected
   */
  @Test
  fun disconnectHeadsetAlreadyConnected_DisconnectionFailed() {
//        bluetoothLE.setGatt(gatt; //todo mock gatt.getdevice.getname
//        bluetoothLE.disconnectHeadsetAlreadyConnected(DEVICE_NAME,false);
//        assertFalse(bluetoothLE.gatt.discoverServices());
  }

  @Test
  fun connectA2DPFromBLE_NullGatt() {
    //todo mock
  }

  @Test
  fun disconnectA2DPFromBLE_NullGatt() {
    bluetoothLE.gatt = null
    //assertFalse(bluetoothLE.disconnectA2DPFromBLE());
  }

  @Test
  fun requestBonding() {
  }

  //
  //    @Test
  //    public void startScanDiscovery() {
  //    }
  //
  //    @Test
  //    public void stopScanDiscovery() {
  //    }
  @Test
  fun notifyConnectionStateChanged_notifyClientConnected() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS)
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.DATA_BT_CONNECTION_SUCCESS)
  }

  @Test
  fun notifyConnectionStateChanged_doNotNotifyClientConnected() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTION_SUCCESS)
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.DATA_BT_CONNECTION_SUCCESS)
  }

  @Test
  fun notifyConnectionStateChanged_notifyClientDisconnected() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED)
    Assert.assertEquals(bluetoothLE.currentState, BluetoothState.IDLE)
  }

  @Test
  fun enableBluetoothOnDevice() {
  }

  /**
   * Check that a command is not sent
   * and that an error is raised
   * if the connection state is invalid
   */
  @Test
  fun sendCommand_invalidConnectionState() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_CONNECTING)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic)
    Mockito.`when`(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true)
    Mockito.`when`(characteristic!!.getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
    Mockito.`when`(gatt.writeCharacteristic(characteristic)).thenReturn(true)
    bluetoothLE.sendCommand(command)
    Mockito.verify(command)?.onError(BluetoothError.ERROR_NOT_CONNECTED, null)
    Mockito.verify(gatt, times(1)).getService(SERVICE) //a valid command interacts with gatt to write the characteristic/request the mtu
    Mockito.verifyZeroInteractions(asyncOperation) //a valid command interacts with an asyncOperation that wait until the response is received (or until timeout)
    //Mockito.verify(manager).reader.notifyResponseReceived(null, command) //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
  }

  /**
   * Check that a command is not sent
   * and that an error is raised if the command is invalid
   */
  @Test
  fun sendCommand_invalidCommand() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED)
    Mockito.`when`(command.isValid).thenReturn(false)
    command.let { bluetoothLE.sendCommand(it) }
    Mockito.verify(gatt,times(1)).getService(SERVICE) //a valid command interact with gatt to write the characteristic/request the mtu
    Mockito.verifyZeroInteractions(asyncOperation) //a valid command interacts with an asyncOperation that wait until the response is received (or until timeout)
//    Mockito.verify(manager).reader.notifyResponseReceived(null, command) //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
  }

  /**
   * Check that a command is not sent
   * and that an error is raised
   * if the sending operation encountered a problem (mock sendRequestData)
   */
  @Test
  fun sendCommand_failureSendRequestData() {
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED)
    command = Mockito.mock(MbtCommand::class.java) as MbtCommand<BaseError>
    Mockito.`when`(command.isValid).thenReturn(true)
    Mockito.`when`(command.serialize()).thenReturn(ByteArray(0))
    command.let { bluetoothLE.sendCommand(it) }
    Mockito.verify(command)?.onError(BluetoothError.ERROR_REQUEST_OPERATION, null)
    Mockito.verifyZeroInteractions(asyncOperation) //a valid command interacts with an asyncOperation that wait until the response is received (or until timeout)
    //Mockito.verify(manager).reader.notifyResponseReceived(null, command) //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
  }

  /**
   * Check that a command is sent and a response is returned to the client
   * if a device is connected , the command is valid and the send request data method succeeded
   *
   */
  @Test
  @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
  fun sendCommand_SuccessWithResponse() {
    val mtu = 47
    val timeout = 11000
    command = Mockito.mock(Mtu::class.java)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED)
    //if this setter is not called, the async configuration mock is not called
    Mockito.`when`(command.isValid).thenReturn(true)
    Mockito.`when`<Any?>(command.serialize()).thenReturn(mtu)
    Mockito.`when`(gatt.requestMtu(47)).thenReturn(true)
    Mockito.`when`(command.isResponseExpected).thenReturn(true)
    Mockito.`when`(asyncOperation.waitOperationResult(timeout)).thenReturn(mtu)
    bluetoothLE.sendCommand(command)
    Mockito.verify(command).serialize()
    Mockito.verify(gatt)?.requestMtu(47)
    Mockito.verify(command).isResponseExpected
    Mockito.verify(command).onRequestSent()
    Mockito.verify(asyncOperation).waitOperationResult(timeout)
    Mockito.verify(command).onResponseReceived(mtu)
  //  Mockito.verify(manager).reader.notifyResponseReceived(mtu, command) //non null response is supposed to be returned to notify the bluetooth manager that the command has succeeded to be sent
  }

  /**
   * Check that a command is sent and no response is returned to the client
   * if a device is connected , the command is valid and the send request data method succeeded
   */
  @Test
  fun sendCommand_SuccessNoResponse() {
    val timeout = 11000
    command = Mockito.mock(Reboot::class.java)
    bluetoothLE.notifyConnectionStateChanged(BluetoothState.CONNECTED)
    Mockito.`when`(command.isValid).thenReturn(true)
    Mockito.`when`<Any>(command.serialize()).thenReturn(BYTE_REQUEST)
    Mockito.`when`(gatt.getService(SERVICE)).thenReturn(gattService)
    Mockito.`when`(gattService!!.uuid).thenReturn(SERVICE)
    Mockito.`when`(gattService!!.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic)
    Mockito.`when`(characteristic!!.uuid).thenReturn(CHARACTERISTIC_MAILBOX)
    Mockito.`when`(characteristic!!.getDescriptor(DESCRIPTOR)).thenReturn(descriptor)
    Mockito.`when`(descriptor!!.value).thenReturn(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    Mockito.`when`(gatt.setCharacteristicNotification(characteristic, true)).thenReturn(true)
    Mockito.`when`(descriptor!!.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true)
    Mockito.`when`(gatt.writeDescriptor(descriptor)).thenReturn(true)
    Mockito.`when`(gatt.writeCharacteristic(characteristic)).thenReturn(true)
    Mockito.`when`(command.isResponseExpected).thenReturn(false)
    bluetoothLE.sendCommand(command)
    Mockito.verify(command).serialize()
    Mockito.verify(command).onRequestSent()
    Mockito.verify(command).isResponseExpected
    Mockito.verifyZeroInteractions(asyncOperation) // no response is expected so the async operation does not wait for any response
   // Mockito.verify(manager).reader.notifyResponseReceived(null, command) //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
  }
}