package core;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;


import command.CommandInterface;
import core.bluetooth.requests.CommandRequestEvent;
import core.device.DeviceEvents;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import engine.SimpleRequestCallback;
import eventbus.MbtEventBus;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@RunWith( PowerMockRunner.class )
public class MbtManagerTest {

    Context context ;
    MbtManager manager ;


    @Before
    public void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        manager = new MbtManager(context);
        MbtEventBus.BUS.unregister(manager);
        EventBus.clearCaches();
    }


    //    @Test
//    public void constructor_AllUnitsEnabled() {
//Context context = Mockito.mock(Context.class);
//    MbtManager manager = new MbtManager(context);
//        if(BuildConfig.BUILD_TYPE.equals("unitTests")){
//            assertTrue(BuildConfig.BLUETOOTH_ENABLED);
//            assertEquals(1, manager.getRegisteredModuleManagers().size());
//            ArrayList<BaseModuleManager> modulesRegistered = new ArrayList<>(manager.getRegisteredModuleManagers());
//            assertTrue(modulesRegistered.get(0) instanceof MbtBluetoothManager);
//        }else if(BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("release")){
//            assertTrue(BuildConfig.BLUETOOTH_ENABLED);
//            assertTrue(BuildConfig.DEVICE_ENABLED);
//            assertTrue(BuildConfig.EEG_ENABLED);
//            assertEquals(3, manager.getRegisteredModuleManagers().size());
//            ArrayList<BaseModuleManager> modulesRegistered = new ArrayList<>(manager.getRegisteredModuleManagers());
//            assertTrue(modulesRegistered.get(0) instanceof MbtBluetoothManager || modulesRegistered.get(1) instanceof MbtBluetoothManager || modulesRegistered.get(2) instanceof MbtBluetoothManager);
//            assertTrue(modulesRegistered.get(0) instanceof MbtDeviceManager || modulesRegistered.get(1) instanceof MbtDeviceManager || modulesRegistered.get(2) instanceof MbtDeviceManager);
//            assertTrue(modulesRegistered.get(0) instanceof MbtEEGManager || modulesRegistered.get(1) instanceof MbtEEGManager || modulesRegistered.get(2) instanceof MbtEEGManager);
//        }
//    }

    @After
    public void tearDown() {
        EventBus.clearCaches();
    }

    /**
     *  Check that a subscriber is well registered
     *  to receive the headset instance
     *  and if a callback is provided by the client
     *  Also check that no subscriber was registered before the request,
     *  and the subscriber is unregistered once the response callback is returned.
     */
    @Test
    public void requestCurrentConnectedDevice_withValidCallback(){
        Context context = Mockito.mock(Context.class);
        MbtManager manager = new MbtManager(context);
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        MbtDevice connectedDevice  = new MelomindDevice(bluetoothDevice);
        ArgumentCaptor<DeviceEvents.GetDeviceEvent> captor = ArgumentCaptor.forClass(DeviceEvents.GetDeviceEvent.class); //capture any get device request

        SimpleRequestCallback<MbtDevice> callback = device -> {
            assertEquals(device, connectedDevice);
            //the subscriber is supposed to be unregistered once the callback is triggered
            assertFalse(MbtEventBus.BUS.hasSubscriberForEvent(DeviceEvents.PostDeviceEvent.class));
        };
        //no subscriber is supposed to be registered before the command call
        assertFalse(MbtEventBus.BUS.hasSubscriberForEvent(DeviceEvents.PostDeviceEvent.class));
        PowerMockito.mockStatic(MbtEventBus.class);

        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        MbtEventBus.postEvent(connectedDevice);
                        return null;})
                    .when(MbtEventBus.class, "postEvent", captor.capture(), Mockito.any(MbtEventBus.Callback.class));

        } catch (Exception e) {
            e.printStackTrace();
        }
        manager.requestCurrentConnectedDevice(callback);
    }

    /**
     *  Check that no event is posted if the input callbcak is invalid
     */
    @Test
    public void requestCurrentConnectedDevice_withInvalidCallback(){
        Context context = Mockito.mock(Context.class);
        MbtManager manager = new MbtManager(context);

        //no subscriber is supposed to be registered before the command call
        assertFalse(MbtEventBus.BUS.hasSubscriberForEvent(DeviceEvents.PostDeviceEvent.class));
        PowerMockito.spy(MbtEventBus.class);
        PowerMockito.verifyZeroInteractions(MbtEventBus.class);

        manager.requestCurrentConnectedDevice(null);
    }
}