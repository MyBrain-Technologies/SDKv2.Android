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
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.requests.DeviceCommandRequestEvent;
import core.device.DeviceEvents;
import engine.SimpleRequestCallback;
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
        //MbtBluetoothManager bluetoothManager = Mockito.mock(MbtBluetoothManager.class);
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

    @Test
    public void reconnectIfAudioConnected() {
    }

    @Test
    public void resetBackgroundReconnectionRetryCounter() {
    }

    @Test
    public void registerReceiverIntents() {
    }

    @Test
    public void scanAndConnect() {
        //bluetoothManager.scanAndConnect("melo_01010101",true);
    }

    @Test
    public void isBluetoothEnabled() {
    }

    @Test
    public void isLocationEnabledAndGranted() {
    }

    @Test
    public void isAlreadyConnected() {
    }

    @Test
    public void scan() {
    }

    @Test
    public void bondAndConnectAudioIfRequested() {
    }

    @Test
    public void connect() {
    }

    @Test
    public void scanDevices() {
    }

    @Test
    public void scanSingle() {
    }

    @Test
    public void stopCurrentScan() {
    }

    @Test
    public void configureHeadset() {
    }

    @Test
    public void startStream() {
    }

    @Test
    public void stopStream() {
    }

    @Test
    public void readBattery() {
    }

    @Test
    public void readFwVersion() {
    }

    @Test
    public void readHwVersion() {
    }

    @Test
    public void readSerialNumber() {
    }

    @Test
    public void readModelNumber() {
    }

    @Test
    public void disconnect() {
    }

    @Test
    public void cancelPendingConnection() {
    }

    @Test
    public void handleDataAcquired() {
    }

    @Test
    public void deinit() {
    }

    @Test
    public void onNewBluetoothRequest() {
    }

    @Test
    public void getConnectionError() {
    }

    @Test
    public void notifyDeviceInfoReceived() {
    }

    @Test
    public void notifyStreamStateChanged() {
    }

    @Test
    public void notifyNewHeadsetStatus() {
    }

    @Test
    public void isConnected() {
    }

    @Test
    public void notifyStateChanged() {
    }

    @Test
    public void connectBLEFromA2DP() {
    }

    @Test
    public void requestCurrentConnectedDevice() {
    }

    @Test
    public void connectA2DPFromBLE() {
    }

    @Test
    public void disconnectA2DPFromBLE() {
    }

    @Test
    public void isAlreadyConnectedToRequestedDevice() {
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