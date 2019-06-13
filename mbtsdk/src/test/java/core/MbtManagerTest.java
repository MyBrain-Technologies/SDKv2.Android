package core;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.stubbing.Answer;
import org.powermock.core.PowerMockUtils;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

import command.DeviceCommand;
import command.DeviceCommands;
import core.bluetooth.MbtBluetoothManager;

import core.bluetooth.requests.DeviceCommandRequestEvent;
import core.device.DeviceEvents;
import core.device.MbtDeviceManager;
import core.eeg.MbtEEGManager;
import engine.SimpleRequestCallback;
import eventbus.EventBusManager;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.api.mockito.PowerMockito;

//@PrepareForTest({EventBusManager.class})
@RunWith( PowerMockRunner.class )
public class MbtManagerTest {

    Context context;
    MbtManager manager;
    MbtBluetoothManager bluetoothManager;

    @Before
    public void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        manager = new MbtManager(context);

    }

    @Test
    public void constructor_AllUnitsEnabled() {
        if(BuildConfig.BUILD_TYPE.equals("unitTests")){
            assertTrue(BuildConfig.BLUETOOTH_ENABLED);
            assertEquals(1, manager.getRegisteredModuleManagers().size());
            ArrayList<BaseModuleManager> modulesRegistered = new ArrayList<>(manager.getRegisteredModuleManagers());
            assertTrue(modulesRegistered.get(0) instanceof MbtBluetoothManager);
        }else if(BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("release")){
            assertTrue(BuildConfig.BLUETOOTH_ENABLED);
            assertTrue(BuildConfig.DEVICE_ENABLED);
            assertTrue(BuildConfig.EEG_ENABLED);
            assertEquals(3, manager.getRegisteredModuleManagers().size());
            ArrayList<BaseModuleManager> modulesRegistered = new ArrayList<>(manager.getRegisteredModuleManagers());
            assertTrue(modulesRegistered.get(0) instanceof MbtBluetoothManager || modulesRegistered.get(1) instanceof MbtBluetoothManager || modulesRegistered.get(2) instanceof MbtBluetoothManager);
            assertTrue(modulesRegistered.get(0) instanceof MbtDeviceManager || modulesRegistered.get(1) instanceof MbtDeviceManager || modulesRegistered.get(2) instanceof MbtDeviceManager);
            assertTrue(modulesRegistered.get(0) instanceof MbtEEGManager || modulesRegistered.get(1) instanceof MbtEEGManager || modulesRegistered.get(2) instanceof MbtEEGManager);
        }
    }

    /**
     * Check that a subscriber is well registered
     * to receive the headset response
     * if any mailbox command is sent
     * and if no callback is provided by the client
     * Also check that no subscriber was registered before the request.
     */
    @Test
    public void sendDeviceCommand_noCallback(){
        byte[] response = new byte[]{0,1,2,3,4,5,6,7,8,9};
        DeviceCommand command = new DeviceCommands.ConnectAudio();
        SimpleRequestCallback<byte[]> simpleRequestCallback = Mockito.mock(SimpleRequestCallback.class);

        //no subscriber is supposed to be registered before the command is called
        assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        manager.sendDeviceCommand(command);

        //a subscriber is supposed to be registered once the command is called
        assertTrue(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        //verify that the "on request complete" callback is not triggered if the client didn't provide any callback
        Mockito.verify(simpleRequestCallback, Mockito.never()).onRequestComplete(response);
    }

    /**
     * Check that a subscriber is well registered
     * to receive the headset response
     * if any mailbox command is sent
     * and if a callback is provided by the client
     * Also check that no subscriber was registered before the request,
     * and the subscriber is unregistered once the response callback is returned.
     */
    @Test
    public void sendDeviceCommand_withCallback(){
        MbtBluetoothManager bluetoothManager = Mockito.mock(MbtBluetoothManager.class);
        byte[] response = new byte[]{0,1,2,3,4,5,6,7,8,9};
        ArgumentCaptor<DeviceCommandRequestEvent> captor = ArgumentCaptor.forClass(DeviceCommandRequestEvent.class); //capture any DeviceCommandRequestEvent

        SimpleRequestCallback<byte[]> simpleRequestCallback = callbackResponse -> {
            assertEquals(response, callbackResponse);
            //the subscriber is supposed to be unregistered once the callback is triggered
            assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        };
        DeviceCommand command = new DeviceCommands.ConnectAudio(simpleRequestCallback);

        //no subscriber is supposed to be registered before the command call
        assertFalse(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
        PowerMockito.spy(EventBusManager.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        //a subscriber is supposed to be registered once the postEvent method is called
                        assertTrue(EventBusManager.BUS.hasSubscriberForEvent(DeviceEvents.RawDeviceResponseEvent.class));
                        DeviceCommandRequestEvent deviceCommandRequestEvent = captor.getValue();
                        //assert that the right event is posted to send the mailbox command
                        assertThat(deviceCommandRequestEvent.getCommand(), is(command));
                        bluetoothManager.notifyDeviceResponseReceived(response, command);
                        return null;
                    })
                    .when(EventBusManager.class, "postEvent", captor.capture(), Mockito.any(EventBusManager.Callback.class));

        } catch (Exception e) {
            e.printStackTrace();
        }
        manager.sendDeviceCommand(command);

    }
}