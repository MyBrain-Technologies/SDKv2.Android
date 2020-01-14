package core;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import command.CommandInterface;
import config.StreamConfig;
import core.bluetooth.StreamState;
import core.bluetooth.requests.CommandRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.DeviceEvents;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.eeg.storage.MbtEEGPacket;
import engine.SimpleRequestCallback;
import eventbus.MbtEventBus;
import engine.clientevents.BaseError;
import engine.clientevents.EegListener;
import features.MbtDeviceType;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@RunWith( PowerMockRunner.class )
@PrepareForTest(MbtEventBus.class)
public class MbtManagerTest {

    Context context;
    MbtManager manager;


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
//
//    /**
//     * Check that a subscriber is well registered
//     * to receive the headset response
//     * if any mailbox command is sent
//     * and if no callback is provided by the client
//     */
//    @Test
//    public void sendCommand(){
//        CommandInterface.MbtCommand command = Mockito.mock(CommandInterface.MbtCommand.class);
//        PowerMockito.spy(MbtEventBus.class);
//        MbtEventBus.postEvent(new CommandRequestEvent(command));
//
//        manager.sendCommand(command);
//
//    }
//
//
//
//    @After
//    public void tearDown() {
//        EventBus.clearCaches();
//    }
//
//    /**
//     *  Check that a subscriber is well registered
//     *  to receive the headset instance
//     *  and if a callback is provided by the client
//     *  Also check that no subscriber was registered before the request,
//     *  and the subscriber is unregistered once the response callback is returned.
//     */
//    @Test
//    public void requestCurrentConnectedDevice_withValidCallback(){
//        Context context = Mockito.mock(Context.class);
//        MbtManager manager = new MbtManager(context);
//        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
//        MbtDevice connectedDevice  = new MelomindDevice(bluetoothDevice);
//        ArgumentCaptor<DeviceEvents.GetDeviceEvent> captor = ArgumentCaptor.forClass(DeviceEvents.GetDeviceEvent.class); //capture any get device request
//
//        SimpleRequestCallback<MbtDevice> callback = device -> {
//            assertEquals(device, connectedDevice);
//            //the subscriber is supposed to be unregistered once the callback is triggered
//            assertFalse(MbtEventBus.BUS.hasSubscriberForEvent(DeviceEvents.PostDeviceEvent.class));
//        };
//        //no subscriber is supposed to be registered before the command call
//        assertFalse(MbtEventBus.BUS.hasSubscriberForEvent(DeviceEvents.PostDeviceEvent.class));
//        PowerMockito.mockStatic(MbtEventBus.class);
//
//        try {
//            PowerMockito
//                .doAnswer((Answer<Void>) invocation -> {
//                    MbtEventBus.postEvent(connectedDevice);
//                        return null;})
//                .when(MbtEventBus.class, "postEvent", captor.capture(), Mockito.any(MbtEventBus.Callback.class));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        manager.requestCurrentConnectedDevice(callback);
//    }
//
//    /**
//     *  Check that no event is posted if the input callbcak is invalid
//     */
//    @Test
//    public void requestCurrentConnectedDevice_withInvalidCallback(){
//        Context context = Mockito.mock(Context.class);
//        MbtManager manager = new MbtManager(context);
//
//        //no subscriber is supposed to be registered before the command call
//        assertFalse(MbtEventBus.BUS.hasSubscriberForEvent(DeviceEvents.PostDeviceEvent.class));
//        PowerMockito.spy(MbtEventBus.class);
//        PowerMockito.verifyZeroInteractions(MbtEventBus.class);
//
//        manager.requestCurrentConnectedDevice(null);
//    }


    @Test

    public void sendCommand() {
        CommandInterface.MbtCommand command = Mockito.mock(CommandInterface.MbtCommand.class);
        PowerMockito.spy(MbtEventBus.class);
        MbtEventBus.postEvent(new CommandRequestEvent(command));
    }

    public void stopStream_noRecord() {
        manager.startStream(new StreamConfig.Builder(new EegListener<BaseError>() {
            @Override
            public void onNewPackets(@NonNull MbtEEGPacket eegPackets) {

            }

            @Override
            public void onNewStreamState(@NonNull StreamState streamState) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {

            }
        }).createForDevice(MbtDeviceType.MELOMIND));
        assertTrue(new StreamRequestEvent(false, false,
                false, false, null).stopStream());

    }

    @Test
    public void stopStream_record() {
//        manager.startStream(new StreamConfig.Builder(new EegListener<BaseError>() {
//            @Override
//            public void onNewPackets(@NonNull MbtEEGPacket eegPackets) {
//
//            }
//
//            @Override
//            public void onError(BaseError error, String additionalInfo) {
//
//            }
//        }).recordData(new RecordConfig.Builder(context).create()).create());
        assertTrue(new StreamRequestEvent(false, false,
                false, false, null).stopStream());

    }

    @Test
    public void stopRecord_streamStarted() {
//        manager.startStream(new StreamConfig.Builder(new EegListener<BaseError>() {
//            @Override
//            public void onNewPackets(@NonNull MbtEEGPacket eegPackets) {
//
//            }
//
//            @Override
//            public void onError(BaseError error, String additionalInfo) {
//
//            }
//        }).create());
//        manager.startRecord(context);
        assertFalse(new StreamRequestEvent(false, true,
                false, false, null).stopStream());

    }

    @Test
    public void startStreamRecord_stopStreamRecord() {
//        manager.startStream(new StreamConfig.Builder(new EegListener<BaseError>() {
//            @Override
//            public void onNewPackets(@NonNull MbtEEGPacket eegPackets) {
//
//            }
//
//            @Override
//            public void onError(BaseError error, String additionalInfo) {
//
//            }
//        }).create());
//        manager.startRecord(context);
//        manager.stopRecord(null);
        assertTrue(new StreamRequestEvent(false, false,
                false, false, null).stopStream());

    }

    @Test
    public void startStreamRecord_stopStream() {
//        manager.startStream(new StreamConfig.Builder(new EegListener<BaseError>() {
//            @Override
//            public void onNewPackets(@NonNull MbtEEGPacket eegPackets) {
//
//            }
//
//            @Override
//            public void onError(BaseError error, String additionalInfo) {
//
//            }
//        }).create());
//        manager.startRecord(context);
        assertTrue(new StreamRequestEvent(false, false,
                false, false, null).stopStream());

    }

    @Test
    public void requestCurrentConnectedDevice_withValidCallback() {
        Context context = Mockito.mock(Context.class);
        MbtManager manager = new MbtManager(context);
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        MbtDevice connectedDevice = new MelomindDevice(bluetoothDevice);
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
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture(), Mockito.any(MbtEventBus.Callback.class));

        } catch (Exception e) {
            e.printStackTrace();
        }
        manager.requestCurrentConnectedDevice(callback);
    }

    public void stopRecord_noStream() {
        //manager.startRecord(context);
        assertTrue(new StreamRequestEvent(false, false,
                false, false, null).stopStream());

    }

    @Test
    public void startRecord_stopStream() {
        //manager.startRecord(context);
        assertTrue(new StreamRequestEvent(false, false,
                false, false, null).stopStream());

    }
}
