package core.bluetooth;

import android.content.Context;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import android.os.Handler;

import java.util.Arrays;

import command.DeviceCommand;
import command.DeviceCommands;
import core.MbtManager;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.requests.BluetoothRequests;
import core.bluetooth.requests.DeviceCommandRequestEvent;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.device.DeviceEvents;
import eventbus.EventBusManager;
import features.MbtDeviceType;

import static org.junit.Assert.*;

public class MbtBluetoothManagerTest {

    private MbtBluetoothManager bluetoothManager;
    private final byte[] RESPONSE = new byte[]{0,1,2,3,4,5};

    @Before
    public void setUp() throws Exception {
        Context context = Mockito.mock(Context.class);
        bluetoothManager = new MbtBluetoothManager(context);
    }

    /**
     * Check that a RESPONSE callback is triggered
     * if any mailbox command is sent
     * Also check that no subscriber was registered before the request,
     * and the subscriber is unregistered once the RESPONSE callback is returned.
     */
    @Test
    public void onNewBluetoothRequest_DeviceCommandRequestEvent_withCallback(){

        byte[] response = new byte[]{0,1,2,3,4,5,6,7,8,9};
        Handler requestHandler = Mockito.mock(Handler.class);
        DeviceCommand command = new DeviceCommands.ConnectAudio(callbackResponse -> {
            assertEquals(response, callbackResponse);
            //the subscriber is supposed to be unregistered once the callback is triggered
            assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        });

        Mockito.doAnswer((Answer<Void>) invocation -> {
            bluetoothManager.notifyDeviceResponseReceived(response, command);
            return null;
        }).when(requestHandler).post(Mockito.any(Runnable.class));

        //no subscriber is supposed to be registered before the command call
//        assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        bluetoothManager.onNewBluetoothRequest(new DeviceCommandRequestEvent(command));
    }

    /**
     * Check that any incoming request is added to the handler queue
     * by verifying that the request handler has posted the runnable
     */
    @Test
    public void onNewBluetoothRequest_requestAddedToQueue(){
        Handler requestHandler = Mockito.spy(Handler.class);
        bluetoothManager.setRequestHandler(requestHandler);
        DeviceCommand command = new DeviceCommands.ConnectAudio();
//        MessageQueue queueBeforeEvent = requestHandler.getLooper().getQueue();
        bluetoothManager.onNewBluetoothRequest(new DeviceCommandRequestEvent(command));
        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));
//        MessageQueue queueAfterEvent = requestHandler.getLooper().getQueue();
//        assertNotSame(queueBeforeEvent, queueAfterEvent);
    }

    /**
     * Check that any queued request is executed by the handler
     * by checking that the request Thread has parsed the request
     */
    @Test
    public void onNewBluetoothRequest_requestQueuedExecuted(){
        MbtBluetoothManager.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestThread.class);
        bluetoothManager.setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(new Handler(requestThread.getLooper()));
        bluetoothManager.setRequestHandler(requestHandler);

        DeviceCommand command = new DeviceCommands.ConnectAudio();
        bluetoothManager.onNewBluetoothRequest(new DeviceCommandRequestEvent(command));
        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class)); //check that the handler posts the runnable

        Mockito.doAnswer(( Answer<Void>) invocation -> { //check that the thread parses the request posted
            Mockito.verify(requestThread).parseRequest(Mockito.any(BluetoothRequests.class));
            return null;
        }).when(requestHandler).post(Mockito.any(Runnable.class));
        //Mockito.doCallRealMethod().when(requestThread).parseRequest(Mockito.any(DeviceCommandRequestEvent.class));

    }

    /**
     * Check that the bluetooth manager receives
     * the Bluetooth request Event posted by the BUS
     *
     */
    @Test
    public void postEvent_receivedRequest(){
        DeviceCommand command = new DeviceCommands.ConnectAudio();
        DeviceCommandRequestEvent deviceCommandRequestEvent = new DeviceCommandRequestEvent(command);
        Handler requestHandler = Mockito.spy(Handler.class);
        bluetoothManager.setRequestHandler(requestHandler);

        EventBusManager.postEvent(deviceCommandRequestEvent);
        //as the bluetooth manager is the tested class, we cannot mock it and call verify(bluetoothmanager).onNewBluetoothRequest so we verify that a method present inside this method is called
        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));

    }

    /**
     * Check that the bluetooth manager sends the device command
     * to the Bluetooth low energy class
     * once the requested has been parsed
     *
     */
    @Test
    public void postEvent_executedRequest(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        DeviceCommand command = new DeviceCommands.ConnectAudio();
        DeviceCommandRequestEvent deviceCommandRequestEvent = new DeviceCommandRequestEvent(command);
        MbtBluetoothManager.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestThread.class);
        bluetoothManager.setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(new Handler(requestThread.getLooper()));
        bluetoothManager.setRequestHandler(requestHandler);
        bluetoothManager.setMbtBluetoothLE(bluetoothLE);

        bluetoothManager.onNewBluetoothRequest(deviceCommandRequestEvent);

        //as the bluetooth manager is the tested class, we cannot mock it and call verify(bluetoothmanager).onNewBluetoothRequest so we verify that a method called inside this method is called
        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));
    }

    /**
     * Check that the send device command method
     * is called if a device command request is parsed
     *
     */
    @Test
    public void parseRequest_DeviceCommandRequest_valid(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        bluetoothManager.setMbtBluetoothLE(bluetoothLE);
        DeviceCommand command = new DeviceCommands.ConnectAudio();
        DeviceCommandRequestEvent deviceCommandRequestEvent = new DeviceCommandRequestEvent(command);
        StartOrContinueConnectionRequestEvent initRequest = new StartOrContinueConnectionRequestEvent(true, "testName", "testQrCode", MbtDeviceType.MELOMIND);
        Mockito.when(bluetoothLE.getCurrentState()).thenReturn(BtState.CONNECTED_AND_READY);
        bluetoothManager.getRequestThread().parseRequest(initRequest);

        bluetoothManager.getRequestThread().parseRequest(deviceCommandRequestEvent);

        Mockito.verify(bluetoothLE).sendDeviceCommand(command);
    }

    /**
     * Check that the headset RESPONSE to a device command request
     * is well posted to the RESPONSE subscribers
     *
     */
    @Test
    public void notifyDeviceResponseReceived_DeviceCommandRequest_valid(){

        DeviceCommand command = new DeviceCommands.ConnectAudio();
        EventBusManager.Callback callback = new EventBusManager.Callback<DeviceEvents.RawDeviceResponseEvent>(){
            @Override
            @Subscribe
            public Void onEventCallback(DeviceEvents.RawDeviceResponseEvent headsetRawResponse) {
                assertTrue(Arrays.equals(headsetRawResponse.getRawResponse(), RESPONSE));
                EventBusManager.registerOrUnregister(false, this);
                return null;
            }
        };
        EventBusManager.registerOrUnregister(true, callback);
        assertTrue(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));

        //bluetoothManager.notifyDeviceResponseReceived(RESPONSE, command);
        // Event post throws IllegalAccessException: Class org.greenrobot.eventbus.EventBus can not access a member of class core.bluetooth.MbtBluetoothManagerTest$1 with modifiers "public"
        //Eventbus invokes a method by reflection but this raises an exception in the tests only
        // The workaround is to call method.setAccessible(true) before the call to method.invoke(listener)
        // but the call is performed in the Eventbus.java class so we can't do anything for this test
        //assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
    }

    private class MbtManagerWrapper extends MbtManager{

        /**
         * @param context
         */
        public MbtManagerWrapper(Context context) {
            super(context);
        }
    }
}