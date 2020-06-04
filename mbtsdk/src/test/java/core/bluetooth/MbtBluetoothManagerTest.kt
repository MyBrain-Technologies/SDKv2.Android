package core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import command.BluetoothCommand
import command.BluetoothCommands
import command.DeviceCommandEvent
import core.bluetooth.MbtBluetoothManager.RequestProcessor.RequestThread
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.requests.BluetoothRequests
import core.bluetooth.requests.DisconnectRequestEvent
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent
import core.bluetooth.spp.MbtBluetoothSPP
import engine.clientevents.BaseError
import features.MbtDeviceType
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class MbtBluetoothManagerTest {
  private val RESPONSE_MTU = 47
  private val RESPONSE_SERIAL_NUMBER = byteArrayOf(DeviceCommandEvent.MBX_SET_SERIAL_NUMBER.identifierCode, 1, 2, 3, 4, 5)
  private val RESPONSE_MODEL_NUMBER = byteArrayOf(DeviceCommandEvent.MBX_SET_EXTERNAL_NAME.identifierCode, 1, 2, 3, 4, 5)
  private val RESPONSE_EEG_CONFIG = byteArrayOf(DeviceCommandEvent.MBX_GET_EEG_CONFIG.identifierCode, 1, 2, 3, 4, 5)
  private lateinit var manager: MbtBluetoothManager

  @Before
  fun setUp() {
    val context = Mockito.mock(Context::class.java)
    manager = MbtBluetoothManager(context)
  }

  /**
   * Check that the UninitializedPropertyAccessException is raised if we try accessing the bluetooth context while changeBluetoothParameters has not been called
   */
  @Test (expected =  UninitializedPropertyAccessException::class)
  fun changeBluetoothParameters_notInitialized() {
    assert(manager.context.connectAudio)
  }
  /**
   * Check that changeBluetoothParameters has well set up the parameters for a Melomind Config
   */
  @Test fun changeBluetoothParameters_MelomindInitialization() {
    val context = Mockito.mock(Context::class.java)
    manager.changeBluetoothParameters(
        StartOrContinueConnectionRequestEvent(true,
            BluetoothContext(context,
                MbtDeviceType.MELOMIND,
                true,
                "deviceName",
                "deviceQrCode",
                47
            )))
    assert(manager.context.deviceTypeRequested == MbtDeviceType.MELOMIND)
    assert(manager.context.connectAudio)
    assert(manager.context.deviceNameRequested == "deviceName")
    assert(manager.context.deviceQrCodeRequested == "deviceQrCode")
    assert(manager.context.mtu == 47)
    assert(MbtDataBluetooth.isInitialized())
    assert(MbtDataBluetooth.instance is MbtBluetoothLE)
    assert(MbtAudioBluetooth.instance is MbtBluetoothA2DP)
  }
  /**
   * Check that changeBluetoothParameters has well set up the parameters for a Vpro Config
   */
  @Test
  fun changeBluetoothParameters_VproInitialization() {
    val context = Mockito.mock(Context::class.java)
    val applicationContext = Mockito.mock(Context::class.java)
    val bluetoothManager = Mockito.mock(BluetoothManager::class.java)
    val bluetoothAdapter = Mockito.mock(BluetoothAdapter::class.java)
    Mockito.`when`(context.applicationContext).thenReturn(applicationContext)
    Mockito.`when`(applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    Mockito.`when`(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    manager.changeBluetoothParameters(
        StartOrContinueConnectionRequestEvent(true,
            BluetoothContext(context,
                MbtDeviceType.VPRO,
                true,
                "vpro",
                null,
                47
            )))
    assert(manager.context.deviceTypeRequested == MbtDeviceType.VPRO)
    assert(!manager.context.connectAudio)
    assert(manager.context.deviceNameRequested == "vpro")
    assert(manager.context.deviceQrCodeRequested == null)
    assert(manager.context.mtu == 47)
    assert(MbtDataBluetooth.isInitialized())
    assert(MbtDataBluetooth.instance is MbtBluetoothSPP)
    assert(MbtAudioBluetooth.instance == null)
  }
  /**
   * Check that changeBluetoothParameters has well set up the parameters for a Melomind Config then for a Vpro Config
   * (if changeBluetoothParameters is called twice : one after the other for both headsets)
   */
  @Test
  fun changeBluetoothParameters_MelomindThenVproInitialization() {
    val context = Mockito.mock(Context::class.java)
    manager.changeBluetoothParameters(
        StartOrContinueConnectionRequestEvent(true,
            BluetoothContext(context,
                MbtDeviceType.MELOMIND,
                true,
                "melo_12345678",
                "MM102345678",
                47
            )))
    assert(manager.context.deviceTypeRequested == MbtDeviceType.MELOMIND)
    assert(manager.context.connectAudio)
    assert(manager.context.deviceNameRequested == "melo_12345678")
    assert(manager.context.deviceQrCodeRequested == "MM102345678")
    assert(manager.context.mtu == 47)
    assert(MbtDataBluetooth.isInitialized())
    assert(MbtDataBluetooth.instance is MbtBluetoothLE)
    assert(MbtAudioBluetooth.instance != null)
    val applicationContext = Mockito.mock(Context::class.java)
    val bluetoothManager = Mockito.mock(BluetoothManager::class.java)
    val bluetoothAdapter = Mockito.mock(BluetoothAdapter::class.java)
    Mockito.`when`(context.applicationContext).thenReturn(applicationContext)
    Mockito.`when`(applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    Mockito.`when`(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    manager.changeBluetoothParameters(
        StartOrContinueConnectionRequestEvent(true,
            BluetoothContext(context,
                MbtDeviceType.VPRO,
                true,
                "vpro",
                null,
                47
            )))
   assert(manager.context.deviceTypeRequested == MbtDeviceType.VPRO)
    assert(!manager.context.connectAudio)
    assert(manager.context.deviceNameRequested == "vpro")
    assert(manager.context.deviceQrCodeRequested == null)
    assert(manager.context.mtu == 47)
    assert(MbtDataBluetooth.isInitialized())
    assert(MbtDataBluetooth.instance is MbtBluetoothSPP)
    assert(MbtAudioBluetooth.instance == null)
  }
  /**
   * Check that changeBluetoothParameters has not changed the bluetooth operators if it was already initialized
   */
  @Test fun changeBluetoothParameters_alreadyInitialized() {
    val context = Mockito.mock(Context::class.java)
    val applicationContext = Mockito.mock(Context::class.java)
    val bluetoothManager = Mockito.mock(BluetoothManager::class.java)
    val bluetoothAdapter = Mockito.mock(BluetoothAdapter::class.java)
    Mockito.`when`(context.applicationContext).thenReturn(applicationContext)
    Mockito.`when`(applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    Mockito.`when`(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    manager.context = BluetoothContext(context,
        MbtDeviceType.MELOMIND,
        true,
        "melo_12345678",
        "MM102345678",
        47
    )
    MbtDataBluetooth.instance = MbtBluetoothLE(manager)
    MbtAudioBluetooth.instance = MbtBluetoothA2DP(manager)
    val dataBluetoothBefore = MbtDataBluetooth.instance
    val audioBluetoothBefore = MbtAudioBluetooth.instance
    Mockito.`when`(context.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    manager.changeBluetoothParameters(
        StartOrContinueConnectionRequestEvent(false,
            BluetoothContext(context,
                MbtDeviceType.VPRO,
                true,
                "vpro",
                null,
                47
            )))
    val dataBluetoothAfter = MbtDataBluetooth.instance
    val audioBluetoothAfter = MbtAudioBluetooth.instance
    assert(dataBluetoothBefore == dataBluetoothAfter)
    assert(audioBluetoothBefore == audioBluetoothAfter)

  }

  /**
   * Check that changeBluetoothParameters has well set up the parameters for a Melomind Config
   */
  @Test fun initBluetoothOperators_MelomindInitialization() {
    val context = Mockito.mock(Context::class.java)
    manager.context = BluetoothContext(context,
        MbtDeviceType.MELOMIND,
        true,
        "melo_12345678",
        "MM102345678",
        47
    )
    manager.initBluetoothOperators()
    assert(MbtDataBluetooth.isInitialized())
    assert(MbtDataBluetooth.instance is MbtBluetoothLE)
    assert(MbtDataBluetooth.instance !is MbtBluetoothSPP)
    assert(MbtAudioBluetooth.instance is MbtBluetoothA2DP)
  }
  /**
   * Check that changeBluetoothParameters has well set up the parameters for a Vpro Config
   */
  @Test
  fun initBluetoothOperators_VproInitialization() {
    val context = Mockito.mock(Context::class.java)
    val applicationContext = Mockito.mock(Context::class.java)
    val bluetoothManager = Mockito.mock(BluetoothManager::class.java)
    val bluetoothAdapter = Mockito.mock(BluetoothAdapter::class.java)
    Mockito.`when`(context.applicationContext).thenReturn(applicationContext)
    Mockito.`when`(applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    Mockito.`when`(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    manager.context = BluetoothContext(context,
        MbtDeviceType.VPRO,
        true,
        "vpro",
        null,
        47
    )
    manager.initBluetoothOperators()

    assert(MbtDataBluetooth.isInitialized())
    assert(MbtDataBluetooth.instance is MbtBluetoothSPP)
    assert(MbtDataBluetooth.instance !is MbtBluetoothLE)
    assert(MbtAudioBluetooth.instance == null)
  }

  fun initMelomind(){
    val context = Mockito.mock(Context::class.java)
    manager.context = BluetoothContext(context,
        MbtDeviceType.MELOMIND,
        true,
        "melo_12345678",
        "MM102345678",
        47
    )
    manager.initBluetoothOperators()
  }
  /**
   * Check that isOperationWaiting return true if an asynchronous operation is in progress
   */
  @Test
  fun isOperationWaiting() {
    initMelomind()
    assert(!manager.isOperationWaiting())
    manager.tryOperation({assert(manager.isOperationWaiting())}, 1000)
    assert(!manager.isOperationWaiting())
    manager.requestProcessor.asyncOperation.tryOperation({assert(manager.isOperationWaiting())},1000)
    assert(!manager.isOperationWaiting())
  }
  /**
   * Check that isSwitchOperationWaiting return true if a switch asynchronous operation is in progress
   */
  @Test
  fun isSwitchOperationWaiting() {
    initMelomind()
    assert(!manager.isSwitchOperationWaiting())
    manager.requestProcessor.asyncSwitchOperation.tryOperation({assert(manager.isSwitchOperationWaiting())},1000)
    assert(!manager.isSwitchOperationWaiting())
  }
  /**
   * Check that the rights methods are called if MTU response is received
   */
  @Test
  fun onBluetoothResponseReceived_MTU() {
    val connecterMock = Mockito.mock(MbtBluetoothConnecter::class.java)
   manager.connecter = connecterMock
    manager.onBluetoothResponseReceived(null, BluetoothCommands.Mtu(47))
    Mockito.verify(connecterMock).updateConnectionState(true)
    Mockito.verify(connecterMock).switchToNextConnectionStep()
  }
  class BluetoothCommandTest() : BluetoothCommand<Unit, BaseError>() {
    override fun getData() {}
    override fun serialize(): Any? {return null}
    override val isValid: Boolean
      get() = true
    override val invalidityError: String?
      get() = null
  }

  /**
   * Check that the no method is called if differents than MTU responses are received
   */
  @Test
  fun onBluetoothResponseReceived_Other() {
    val connecterMock = Mockito.mock(MbtBluetoothConnecter::class.java)
   manager.connecter = connecterMock
    manager.onBluetoothResponseReceived(null, BluetoothCommandTest())
    Mockito.verifyNoMoreInteractions(connecterMock)
  }
  /**
   * Check that any incoming disconnection request is not added
   * to the handler queue to perform directly the disconnection
   * if connection is not aborted
   */
  @Test
  fun onNewBluetoothRequest_DisconnectRequestEvent_Aborted() {
//        MbtBluetoothManager.RequestProcessor.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestProcessor.RequestThread.class);
//        bluetoothManager.getRequestProcessor().setRequestThread(requestThread);
//        Handler requestHandler = Mockito.spy(Handler.class);
//        MbtBluetoothLE dataBluetooth = Mockito.mock(MbtBluetoothLE.class);
//        bluetoothManager.getRequestProcessor().setRequestHandler(requestHandler);
//        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
//        Mockito.when(request.isInterrupted()).thenReturn(true);
//        MbtDataBluetooth.instance = dataBluetooth;
//        Mockito.when(dataBluetooth.isConnected()).thenReturn(true);
//
//        bluetoothManager.onNewBluetoothRequest(request);
//
//        Mockito.verifyZeroInteractions(requestHandler);
  }

  /**
   * Check that any incoming disconnection request is added
   * if connection is not aborted
   */
  @Test
  fun onNewBluetoothRequest_DisconnectRequestEvent_NotAborted() {
    val requestThread = Mockito.mock(RequestThread::class.java)
    manager.requestProcessor.requestThread = requestThread
    val requestHandler = Mockito.spy(Handler::class.java)
    manager.requestProcessor.requestHandler = requestHandler
    val request = Mockito.mock(DisconnectRequestEvent::class.java)
    Mockito.`when`(request.isInterrupted).thenReturn(false)
    manager.onNewBluetoothRequest(request)
    Mockito.verify(requestHandler).post(Mockito.any(Runnable::class.java))
  }

  /** Check that :
   * - any incoming request (non disconnection request) is added to the handler queue
   * by verifying that the request handler has posted the runnable.
   * - any queued request is executed by the handler
   * by checking that the request Thread has parsed the request
   */
  @Test
  fun onNewBluetoothRequest_BluetoothRequests() {
    val requestThread = Mockito.mock(RequestThread::class.java)
    manager.requestProcessor.requestThread = requestThread
    val requestHandler = Mockito.spy(Handler::class.java)
    manager.requestProcessor.requestHandler = requestHandler
    val request = Mockito.mock(BluetoothRequests::class.java)
    Mockito.doAnswer(Answer<Void?> { invocation: InvocationOnMock? ->
      requestThread.parseRequest(request)
      null
    }).`when`(requestHandler).post(Mockito.any(Runnable::class.java))
    //        MessageQueue queueBeforeEvent = requestHandler.getLooper().getQueue();
    manager.onNewBluetoothRequest(request)
    Mockito.verify(requestHandler).post(Mockito.any(Runnable::class.java))

//        MessageQueue queueAfterEvent = requestHandler.getLooper().getQueue();
//        assertNotSame(queueBeforeEvent, queueAfterEvent);
  }

  /**
   * Check that a response callback is triggered and returns the right value
   * if any mailbox command is sent.
   */
  @Test
  fun onNewBluetoothRequest_DeviceCommandRequestEvent() {

//        byte[] expectedResponse = new byte[]{0,1,2,3,4,5,6,7,8,9};
//        MbtBluetoothManager.RequestProcessor.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestProcessor.RequestThread.class);
//        bluetoothManager.getRequestProcessor().setRequestThread(requestThread);
//        Handler requestHandler = Mockito.spy(Handler.class);
//        bluetoothManager.getRequestProcessor().setRequestHandler(requestHandler);
//
//        DeviceCommand command = new DeviceCommands.ConnectAudio(new CommandInterface.CommandCallback<byte[]>() {
//            @Override
//            public void onError(CommandInterface.MbtCommand request, BaseError error, String additionalInfo) {
//            }
//
//            @Override
//            public void onRequestSent(CommandInterface.MbtCommand request) {
//            }
//
//            @Override
//            public void onResponseReceived(CommandInterface.MbtCommand request, byte[] callbackResponse) {
//                assertEquals(expectedResponse,callbackResponse);
//            }
//        });
//
//        Mockito.doAnswer((Answer<Void>) invocation -> {
//            //Mockito.verify(requestThread).parseRequest(Mockito.any(BluetoothRequests.class));
//            bluetoothManager.reader.notifyResponseReceived(expectedResponse, command);
//            return null;
//        }).when(requestHandler).post(Mockito.any(Runnable.class));
//
//        bluetoothManager.onNewBluetoothRequest(new CommandRequestEvent(command));
//
//        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));
  }

  /**
   * Check that the read method
   * is called if a read request is parsed
   *
   */
  @Test
  fun parseRequest_StartOrContinueConnectionRequestEvent() {
//    val bluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
//    MbtDataBluetooth.instance = bluetoothLE
//    val requestEvent = Mockito.mock(StartOrContinueConnectionRequestEvent::class.java)
//    Mockito.`when`(requestEvent.isClientUserRequest).thenReturn(true)
//    Mockito.`when`(bluetoothLE.currentState).thenReturn(BluetoothState.READY_FOR_BLUETOOTH_OPERATION)

    //bluetoothManager.parseRequest(requestEvent);

    //Mockito.verify(bluetoothLE).startLowEnergyScan(true);
  }

  /**
   * Check that the read method
   * is called if a read request is parsed
   *
   */
  @Test
  fun parseRequest_ReadRequestEvent_Battery() {
//    val bluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
//    MbtDataBluetooth.instance = bluetoothLE
//    val requestEvent = Mockito.mock(ReadRequestEvent::class.java)
//    Mockito.`when`(requestEvent.deviceInfo).thenReturn(DeviceInfo.BATTERY)
//    bluetoothManager.parseRequest(requestEvent)
//    Mockito.verify(bluetoothLE).readBattery()
  }

  /**
   * Check that the read method
   * is called if a read request is parsed
   *
   */
  @Test
  fun parseRequest_ReadRequestEvent_HardwareVersion() {
//    val bluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
//    MbtDataBluetooth.instance = bluetoothLE
//    val requestEvent = Mockito.mock(ReadRequestEvent::class.java)
//    Mockito.`when`(requestEvent.deviceInfo).thenReturn(DeviceInfo.HW_VERSION)
//    bluetoothManager.parseRequest(requestEvent)
//    Mockito.verify(bluetoothLE).readHwVersion()
  }

  /**
   * Check that the read method
   * is called if a read request is parsed
   *
   */
  @Test
  fun parseRequest_ReadRequestEvent_FirmwareVersion() {
//    val bluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
//    MbtDataBluetooth.instance = bluetoothLE
//    val requestEvent = Mockito.mock(ReadRequestEvent::class.java)
//    Mockito.`when`(requestEvent.deviceInfo).thenReturn(DeviceInfo.FW_VERSION)
//    bluetoothManager.parseRequest(requestEvent)
//    Mockito.verify(bluetoothLE).readFwVersion()
  }

  /**
   * Check that the read method
   * is called if a read request is parsed
   *
   */
  @Test
  fun parseRequest_ReadRequestEvent_ModelNumber() {
//    val bluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
//    MbtDataBluetooth.instance = bluetoothLE
//    val requestEvent = Mockito.mock(ReadRequestEvent::class.java)
//    Mockito.`when`(requestEvent.deviceInfo).thenReturn(DeviceInfo.MODEL_NUMBER)
//    bluetoothManager.parseRequest(requestEvent)
//    Mockito.verify(bluetoothLE).readModelNumber()
  }

  /**
   * Check that the read method
   * is called if a read request is parsed
   *
   */
  @Test
  fun parseRequest_ReadRequestEvent_SerialNumber() {
//    val bluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
//    MbtDataBluetooth.instance = bluetoothLE
//
//    val requestEvent = Mockito.mock(ReadRequestEvent::class.java)
//    Mockito.`when`(requestEvent.deviceInfo).thenReturn(DeviceInfo.SERIAL_NUMBER)
//    bluetoothManager.parseRequest(requestEvent)
//    Mockito.verify(bluetoothLE).readSerialNumber()
  }

  /**
   * Check that the send device command method
   * is called if a command request is parsed
   *
   */
  @Test
  fun parseRequest_CommandRequestEvent() {
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        CommandInterface.MbtCommand command = Mockito.mock(CommandInterface.MbtCommand.class);
//        CommandRequestEvent commandRequestEvent = new CommandRequestEvent(command);
//
//        bluetoothManager.parseRequest(commandRequestEvent);
//
//        Mockito.verify(bluetoothLE).sendCommand(command);
  }

  /**
   * Check that the start stream  method
   * is called if a stream request is parsed
   *
   */
  @Test
  fun parseRequest_StartStreamRequestEvent() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        StreamRequestEvent request = Mockito.mock(StreamRequestEvent.class);
//        Mockito.when(request.isStartStream()).thenReturn(true);
//        Mockito.when(bluetoothLE.isConnected()).thenReturn(true);
//        Mockito.when(bluetoothLE.isStreaming()).thenReturn(false);
//
//        bluetoothManager.parseRequest(request);
//
//        Mockito.verify(bluetoothLE).startStream();
  }

  /**
   * Check that the start stream  method
   * is called if a stream request is parsed
   *
   */
  @Test
  fun parseRequest_StopStreamRequestEvent() {
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        StreamRequestEvent request = Mockito.mock(StreamRequestEvent.class);
//        Mockito.when(request.isStartStream()).thenReturn(false);
//        Mockito.when(bluetoothLE.isStreaming()).thenReturn(true);
//
//        bluetoothManager.parseRequest(request);
//
//        Mockito.verify(bluetoothLE).stopStream();
  }

  /**
   * Check that the cancel connection method
   * is called if a disconnect request is parsed
   *
   */
  @Test
  fun parseRequest_CancelConnectionRequestEvent() {
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
//        Mockito.when(request.isInterrupted()).thenReturn(true);
//        Mockito.when(bluetoothLE.isConnected()).thenReturn(true);
//        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CONNECTED_AND_READY);
//
//        bluetoothManager.parseRequest(request);
//
//        Mockito.verify(bluetoothLE).disconnect();
  }

  /**
   * Check that the disconnect  method
   * is called if a disconnect request is parsed
   */
  @Test
  fun parseRequest_DisconnectRequestEvent() {
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
//        Mockito.when(request.isInterrupted()).thenReturn(false);
//        Mockito.when(bluetoothLE.isConnected()).thenReturn(true);
//        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CONNECTED_AND_READY);
//
//        bluetoothManager.parseRequest(request);
//
//        Mockito.verify(bluetoothLE).disconnect();
  }

  /**
   * Check that the headset RESPONSE to a device command request
   * is well posted to the RESPONSE subscribers
   *
   */
  @Test
  fun notifyResponseReceived_DeviceCommand_NullResponse() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        CommandInterface.MbtCommand command = Mockito.mock(DeviceCommand.class);
//
//        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
//        bluetoothManager.reader.notifyResponseReceived(null, command);
//        MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
//        assertEquals(deviceAfterUpdate, deviceBeforeUpdate);
  }

  /**
   * Check that the serial number has been well updated on the device unit when
   * a headset RESPONSE has been received after a update serial number request
   */
  @Test
  fun notifyResponseReceived_DeviceCommand_SerialNumber() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
//        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
//            MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//            MbtDataBluetooth.instance = bluetoothLE;
//            CommandInterface.MbtCommand command = Mockito.mock(DeviceCommands.UpdateSerialNumber.class);
//            bluetoothManager.reader.notifyResponseReceived(RESPONSE_SERIAL_NUMBER, command);
//
//            MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
//                assertEquals(deviceAfterUpdate.getSerialNumber(), new String(RESPONSE_SERIAL_NUMBER) ); //we check that the serial number has been updated on the Device unit
//                assertNotEquals(deviceBeforeUpdate.getSerialNumber(), deviceAfterUpdate.getSerialNumber());//we compare the device serial number after and before the update
//                assertEquals(deviceBeforeUpdate.getExternalName(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
//                assertNotEquals(deviceAfterUpdate.getSerialNumber(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
  }

  /**
   * Check that the model number (external name) has been well updated on the device unit when
   * a headset RESPONSE has been received after a update external name request
   */
  @Test
  fun notifyResponseReceived_DeviceCommand_ExternalName() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
//        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
//            MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//            MbtDataBluetooth.instance = bluetoothLE;
//            CommandInterface.MbtCommand command = Mockito.mock(DeviceCommands.UpdateExternalName.class);
//            bluetoothManager.reader.notifyResponseReceived(RESPONSE_MODEL_NUMBER, command);
//
//            MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
//                assertNotEquals(deviceBeforeUpdate.getExternalName(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
//                assertEquals(deviceAfterUpdate.getExternalName(), new String(RESPONSE_MODEL_NUMBER) ); //we check that the serial number has been updated on the Device unit
//                assertEquals(deviceBeforeUpdate.getSerialNumber(), deviceBeforeUpdate.getSerialNumber());//we compare the device serial number after and before the update
//                assertNotEquals(deviceAfterUpdate.getSerialNumber(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
//
  }

  /**
   * Check that the current connection state is not updated to [BluetoothState.BT_PARAMETERS_CHANGED]
   * when a bluetooth command response that is not a MTU command has been received
   */
  @Test
  fun notifyResponseReceived_BluetoothCommand() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CHANGING_BT_PARAMETERS);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CHANGING_BT_PARAMETERS);
//        CommandInterface.MbtCommand command = Mockito.mock(BluetoothCommand.class);
//
//        bluetoothManager.reader.notifyResponseReceived(new Object(), command);
//        //assertEquals(bluetoothManager.getCurrentState(), BluetoothState.BT_PARAMETERS_CHANGED ); //impossible to know as getCurrentState is private
//        Mockito.verifyZeroInteractions(bluetoothLE);
  }

  /**
   * Check that the current connection state is updated to [BluetoothState.BT_PARAMETERS_CHANGED]
   * when a new MTU has been received
   */
  @Test
  fun notifyResponseReceived_BluetoothCommand_MTU() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CHANGING_BT_PARAMETERS);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CHANGING_BT_PARAMETERS);
//        CommandInterface.MbtCommand command = Mockito.mock(BluetoothCommands.Mtu.class);
//
//        bluetoothManager.reader.notifyResponseReceived(RESPONSE_MTU, command);
    //assertEquals(bluetoothManager.getCurrentState(), BluetoothState.BT_PARAMETERS_CHANGED ); //impossible to know as getCurrentState is private
    //Mockito.verify(bluetoothLE).notifyConnectionStateChanged(BluetoothState.BT_PARAMETERS_CHANGED);
  }

  /**
   * Check that the EEG internal config has been well updated on the device unit when
   * a headset RESPONSE has been received after a get EEG config request
   */
  @Test
  fun notifyResponseReceived_DeviceCommand_EegConfig() {
//        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
//        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
//        deviceBeforeUpdate.setInternalConfig(new MbtDevice.InternalConfig(2));
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        MbtDataBluetooth.instance = bluetoothLE;
//        CommandInterface.MbtCommand command = Mockito.mock(DeviceCommand.class);
//        bluetoothManager.reader.notifyResponseReceived(RESPONSE_EEG_CONFIG, command);
//
//        MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
//        assertEquals(deviceAfterUpdate.getInternalConfig(), new MbtDevice.InternalConfig(2)); //we check that the internal config has been updated on the Device unit
//        assertNotEquals(deviceBeforeUpdate.getInternalConfig(), deviceBeforeUpdate.getInternalConfig());//we compare the internal config before and after the request command
//
  }
}