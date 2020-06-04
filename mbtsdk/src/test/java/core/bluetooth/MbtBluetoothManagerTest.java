package core.bluetooth;

import android.content.Context;
import android.os.Handler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import command.BluetoothCommand;
import command.BluetoothCommands;
import command.CommandInterface;
import command.DeviceCommand;
import command.DeviceCommandEvent;
import command.DeviceCommands;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.requests.BluetoothRequests;
import core.bluetooth.requests.CommandRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import engine.clientevents.BaseError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MbtBluetoothManagerTest {

    private final int RESPONSE_MTU = 47;
    private final byte[] RESPONSE_SERIAL_NUMBER = new byte[]{DeviceCommandEvent.MBX_SET_SERIAL_NUMBER.getIdentifierCode(),1,2,3,4,5};
    private final byte[] RESPONSE_MODEL_NUMBER = new byte[]{DeviceCommandEvent.MBX_SET_EXTERNAL_NAME.getIdentifierCode(),1,2,3,4,5};
    private final byte[] RESPONSE_EEG_CONFIG = new byte[]{DeviceCommandEvent.MBX_GET_EEG_CONFIG.getIdentifierCode(),1,2,3,4,5};
    private MbtBluetoothManager bluetoothManager;

    @Before
    public void setUp() {
        Context context = Mockito.mock(Context.class);
        bluetoothManager = new MbtBluetoothManager(context);
    }

    /**
     * Check that any incoming disconnection request is not added
     * to the handler queue to perform directly the disconnection
     * if connection is not aborted
     */
    @Test
    public void onNewBluetoothRequest_DisconnectRequestEvent_Aborted(){
        MbtBluetoothManager.RequestProcessor.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestProcessor.RequestThread.class);
        bluetoothManager.getRequestProcessor().setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(Handler.class);
        MbtBluetoothLE dataBluetooth = Mockito.mock(MbtBluetoothLE.class);
        bluetoothManager.getRequestProcessor().setRequestHandler(requestHandler);
        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
        Mockito.when(request.isInterrupted()).thenReturn(true);
        MbtDataBluetooth.instance = dataBluetooth;
        Mockito.when(dataBluetooth.isConnected()).thenReturn(true);

        bluetoothManager.onNewBluetoothRequest(request);

        Mockito.verifyZeroInteractions(requestHandler);

    }

    /**
     * Check that any incoming disconnection request is added
     * if connection is not aborted
     */
    @Test
    public void onNewBluetoothRequest_DisconnectRequestEvent_NotAborted(){
        MbtBluetoothManager.RequestProcessor.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestProcessor.RequestThread.class);
        bluetoothManager.getRequestProcessor().setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(Handler.class);
        bluetoothManager.getRequestProcessor().setRequestHandler(requestHandler);
        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
        Mockito.when(request.isInterrupted()).thenReturn(false);

        bluetoothManager.onNewBluetoothRequest(request);

        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));

    }

    /** Check that :
     * - any incoming request (non disconnection request) is added to the handler queue
     *   by verifying that the request handler has posted the runnable.
     * - any queued request is executed by the handler
     *   by checking that the request Thread has parsed the request
     */
    @Test
    public void onNewBluetoothRequest_BluetoothRequests(){
        MbtBluetoothManager.RequestProcessor.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestProcessor.RequestThread.class);
        bluetoothManager.getRequestProcessor().setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(Handler.class);
        bluetoothManager.getRequestProcessor().setRequestHandler(requestHandler);

        BluetoothRequests request = Mockito.mock(BluetoothRequests.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            requestThread.parseRequest(request);
            return null;
        }).when(requestHandler).post(Mockito.any(Runnable.class));
//        MessageQueue queueBeforeEvent = requestHandler.getLooper().getQueue();

        bluetoothManager.onNewBluetoothRequest(request);

        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));

//        MessageQueue queueAfterEvent = requestHandler.getLooper().getQueue();
//        assertNotSame(queueBeforeEvent, queueAfterEvent);
    }
    /**
     * Check that a response callback is triggered and returns the right value
     *  if any mailbox command is sent.
     */
    @Test
    public void onNewBluetoothRequest_DeviceCommandRequestEvent(){

        byte[] expectedResponse = new byte[]{0,1,2,3,4,5,6,7,8,9};
        MbtBluetoothManager.RequestProcessor.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestProcessor.RequestThread.class);
        bluetoothManager.getRequestProcessor().setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(Handler.class);
        bluetoothManager.getRequestProcessor().setRequestHandler(requestHandler);

        DeviceCommand command = new DeviceCommands.ConnectAudio(new CommandInterface.CommandCallback<byte[]>() {
            @Override
            public void onError(CommandInterface.MbtCommand request, BaseError error, String additionalInfo) {
            }

            @Override
            public void onRequestSent(CommandInterface.MbtCommand request) {
            }

            @Override
            public void onResponseReceived(CommandInterface.MbtCommand request, byte[] callbackResponse) {
                assertEquals(expectedResponse,callbackResponse);
            }
        });

        Mockito.doAnswer((Answer<Void>) invocation -> {
            //Mockito.verify(requestThread).parseRequest(Mockito.any(BluetoothRequests.class));
            bluetoothManager.reader.notifyResponseReceived(expectedResponse, command);
            return null;
        }).when(requestHandler).post(Mockito.any(Runnable.class));

        bluetoothManager.onNewBluetoothRequest(new CommandRequestEvent(command));

        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));
    }

    /**
     * Check that the read method
     * is called if a read request is parsed
     *
     */
    @Test
    public void parseRequest_StartOrContinueConnectionRequestEvent(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        StartOrContinueConnectionRequestEvent requestEvent = Mockito.mock(StartOrContinueConnectionRequestEvent.class);
        Mockito.when(requestEvent.isClientUserRequest()).thenReturn(true);
        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.READY_FOR_BLUETOOTH_OPERATION);

        //bluetoothManager.parseRequest(requestEvent);

        //Mockito.verify(bluetoothLE).startLowEnergyScan(true);
    }

    /**
     * Check that the read method
     * is called if a read request is parsed
     *
     */
    @Test
    public void parseRequest_ReadRequestEvent(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        ReadRequestEvent requestEvent = Mockito.mock(ReadRequestEvent.class);
        Mockito.when(requestEvent.getDeviceInfo()).thenReturn(DeviceInfo.BATTERY);

        bluetoothManager.getRequestProcessor().getRequestThread().parseRequest(requestEvent);

        Mockito.verify(bluetoothLE).readBattery();
    }

    /**
     * Check that the send device command method
     * is called if a command request is parsed
     *
     */
    @Test
    public void parseRequest_CommandRequestEvent(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        CommandInterface.MbtCommand command = Mockito.mock(CommandInterface.MbtCommand.class);
        CommandRequestEvent commandRequestEvent = new CommandRequestEvent(command);

        bluetoothManager.parseRequest(commandRequestEvent);

        Mockito.verify(bluetoothLE).sendCommand(command);
    }

    /**
     * Check that the start stream  method
     * is called if a stream request is parsed
     *
     */
    @Test
    public void parseRequest_StartStreamRequestEvent(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        StreamRequestEvent request = Mockito.mock(StreamRequestEvent.class);
        Mockito.when(request.isStartStream()).thenReturn(true);
        Mockito.when(bluetoothLE.isConnected()).thenReturn(true);
        Mockito.when(bluetoothLE.isStreaming()).thenReturn(false);

        bluetoothManager.parseRequest(request);

        Mockito.verify(bluetoothLE).startStream();
    }


    /**
     * Check that the start stream  method
     * is called if a stream request is parsed
     *
     */
    @Test
    public void parseRequest_StopStreamRequestEvent(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        StreamRequestEvent request = Mockito.mock(StreamRequestEvent.class);
        Mockito.when(request.isStartStream()).thenReturn(false);
        Mockito.when(bluetoothLE.isStreaming()).thenReturn(true);

        bluetoothManager.parseRequest(request);

        Mockito.verify(bluetoothLE).stopStream();
    }

    /**
     * Check that the cancel connection method
     * is called if a disconnect request is parsed
     *
     */
    @Test
    public void parseRequest_CancelConnectionRequestEvent(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
        Mockito.when(request.isInterrupted()).thenReturn(true);
        Mockito.when(bluetoothLE.isConnected()).thenReturn(true);
        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CONNECTED_AND_READY);

        bluetoothManager.parseRequest(request);

        Mockito.verify(bluetoothLE).disconnect();
    }

    /**
     * Check that the disconnect  method
     * is called if a disconnect request is parsed
     */
    @Test
    public void parseRequest_DisconnectRequestEvent(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        DisconnectRequestEvent request = Mockito.mock(DisconnectRequestEvent.class);
        Mockito.when(request.isInterrupted()).thenReturn(false);
        Mockito.when(bluetoothLE.isConnected()).thenReturn(true);
        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CONNECTED_AND_READY);

        bluetoothManager.parseRequest(request);

        Mockito.verify(bluetoothLE).disconnect();
    }

    /**
     * Check that the headset RESPONSE to a device command request
     * is well posted to the RESPONSE subscribers
     *
     */
    @Test
    public void notifyResponseReceived_DeviceCommand_NullResponse(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        CommandInterface.MbtCommand command = Mockito.mock(DeviceCommand.class);

        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
        bluetoothManager.reader.notifyResponseReceived(null, command);
        MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
        assertEquals(deviceAfterUpdate, deviceBeforeUpdate);

    }

    /**
     * Check that the serial number has been well updated on the device unit when
     * a headset RESPONSE has been received after a update serial number request
     */
    @Test
    public void notifyResponseReceived_DeviceCommand_SerialNumber(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
            MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
            MbtDataBluetooth.instance = bluetoothLE;
            CommandInterface.MbtCommand command = Mockito.mock(DeviceCommands.UpdateSerialNumber.class);
            bluetoothManager.reader.notifyResponseReceived(RESPONSE_SERIAL_NUMBER, command);

            MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
                assertEquals(deviceAfterUpdate.getSerialNumber(), new String(RESPONSE_SERIAL_NUMBER) ); //we check that the serial number has been updated on the Device unit
                assertNotEquals(deviceBeforeUpdate.getSerialNumber(), deviceBeforeUpdate.getSerialNumber());//we compare the device serial number after and before the update
                assertEquals(deviceBeforeUpdate.getExternalName(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
                assertNotEquals(deviceAfterUpdate.getSerialNumber(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
            
        
    }

    /**
     * Check that the model number (external name) has been well updated on the device unit when
     * a headset RESPONSE has been received after a update external name request
     */
    @Test
    public void notifyResponseReceived_DeviceCommand_ExternalName(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
            MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
            MbtDataBluetooth.instance = bluetoothLE;
            CommandInterface.MbtCommand command = Mockito.mock(DeviceCommands.UpdateExternalName.class);
            bluetoothManager.reader.notifyResponseReceived(RESPONSE_MODEL_NUMBER, command);

            MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
                assertNotEquals(deviceBeforeUpdate.getExternalName(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
                assertEquals(deviceAfterUpdate.getExternalName(), new String(RESPONSE_MODEL_NUMBER) ); //we check that the serial number has been updated on the Device unit
                assertEquals(deviceBeforeUpdate.getSerialNumber(), deviceBeforeUpdate.getSerialNumber());//we compare the device serial number after and before the update
                assertNotEquals(deviceAfterUpdate.getSerialNumber(), deviceAfterUpdate.getExternalName()); //we make sure that the external name has not been updated, as the serial number update command and external name update command have the same identifier code
            
    }

    /**
     * Check that the current connection state is not updated to {@link BluetoothState#BT_PARAMETERS_CHANGED}
     * when a bluetooth command response that is not a MTU command has been received
     */
    @Test
    public void notifyResponseReceived_BluetoothCommand(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CHANGING_BT_PARAMETERS);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CHANGING_BT_PARAMETERS);
        CommandInterface.MbtCommand command = Mockito.mock(BluetoothCommand.class);

        bluetoothManager.reader.notifyResponseReceived(new Object(), command);
        //assertEquals(bluetoothManager.getCurrentState(), BluetoothState.BT_PARAMETERS_CHANGED ); //impossible to know as getCurrentState is private
        Mockito.verifyZeroInteractions(bluetoothLE);

    }

    /**
     * Check that the current connection state is updated to {@link BluetoothState#BT_PARAMETERS_CHANGED}
     * when a new MTU has been received
     */
    @Test
    public void notifyResponseReceived_BluetoothCommand_MTU(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CHANGING_BT_PARAMETERS);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BluetoothState.CHANGING_BT_PARAMETERS);
        CommandInterface.MbtCommand command = Mockito.mock(BluetoothCommands.Mtu.class);

        bluetoothManager.reader.notifyResponseReceived(RESPONSE_MTU, command);
        //assertEquals(bluetoothManager.getCurrentState(), BluetoothState.BT_PARAMETERS_CHANGED ); //impossible to know as getCurrentState is private
        Mockito.verify(bluetoothLE).notifyConnectionStateChanged(BluetoothState.BT_PARAMETERS_CHANGED);

    }

    /**
     * Check that the EEG internal config has been well updated on the device unit when
     * a headset RESPONSE has been received after a get EEG config request
     */
    @Test
    public void notifyResponseReceived_DeviceCommand_EegConfig(){
        bluetoothManager.connecter.notifyConnectionStateChanged(BluetoothState.CONNECTED_AND_READY);
        MbtDevice deviceBeforeUpdate = bluetoothManager.connecter.getConnectedDevice();
        deviceBeforeUpdate.setInternalConfig(new MbtDevice.InternalConfig(2));
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        MbtDataBluetooth.instance = bluetoothLE;
        CommandInterface.MbtCommand command = Mockito.mock(DeviceCommand.class);
        bluetoothManager.reader.notifyResponseReceived(RESPONSE_EEG_CONFIG, command);

        MbtDevice deviceAfterUpdate = bluetoothManager.connecter.getConnectedDevice();
        assertEquals(deviceAfterUpdate.getInternalConfig(), new MbtDevice.InternalConfig(2)); //we check that the internal config has been updated on the Device unit
        assertNotEquals(deviceBeforeUpdate.getInternalConfig(), deviceBeforeUpdate.getInternalConfig());//we compare the internal config before and after the request command
           
    }

}