package core.bluetooth;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import android.os.Handler;

import command.DeviceCommand;
import command.DeviceCommands;
import core.MbtManager;
import core.bluetooth.requests.BluetoothRequests;
import core.bluetooth.requests.DeviceCommandRequestEvent;
import core.device.DeviceEvents;
import eventbus.EventBusManager;

import static org.junit.Assert.*;

public class MbtBluetoothManagerTest {

    private Context context;
    MbtBluetoothManager bluetoothManager;
    @Before
    public void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        bluetoothManager = new MbtBluetoothManager(context);
    }

    /**
     * Check that a response callback is triggered
     * if any mailbox command is sent
     * Also check that no subscriber was registered before the request,
     * and the subscriber is unregistered once the response callback is returned.
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
        assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        bluetoothManager.onNewBluetoothRequest(new DeviceCommandRequestEvent(command));
    }

    /**
     * Check that any incoming request is added to the handler queue
     */
    @Test
    public void onNewBluetoothRequest_requestAddedToQueue(){
        Handler requestHandler = Mockito.spy(Handler.class);
        bluetoothManager.setRequestHandler(requestHandler);
        DeviceCommand command = new DeviceCommands.ConnectAudio();
        bluetoothManager.onNewBluetoothRequest(new DeviceCommandRequestEvent(command));
        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));

    }

    /**
     * Check that any queued request is executed by the handler
     */
    @Test
    public void onNewBluetoothRequest_requestQueuedExecuted(){
        MbtBluetoothManager.RequestThread requestThread = Mockito.mock(MbtBluetoothManager.RequestThread.class);
        bluetoothManager.setRequestThread(requestThread);
        Handler requestHandler = Mockito.spy(new Handler(requestThread.getLooper()));
        bluetoothManager.setRequestHandler(requestHandler);

        DeviceCommand command = new DeviceCommands.ConnectAudio();
        bluetoothManager.onNewBluetoothRequest(new DeviceCommandRequestEvent(command));
        Mockito.verify(requestHandler).post(Mockito.any(Runnable.class));

        Mockito.doAnswer(( Answer<Void>) invocation -> {
            Mockito.verify(requestThread).parseRequest(Mockito.any(BluetoothRequests.class));
            return null;
        }).when(requestHandler).post(Mockito.any(Runnable.class));
        //Mockito.doCallRealMethod().when(requestThread).parseRequest(Mockito.any(DeviceCommandRequestEvent.class));

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