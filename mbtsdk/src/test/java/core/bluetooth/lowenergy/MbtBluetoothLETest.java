package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


import command.BluetoothCommands;
import command.CommandInterface;
import command.DeviceCommandEvents;
import command.DeviceCommands;
import config.MbtConfig;
import core.bluetooth.BtState;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.StreamState;
import core.bluetooth.spp.MbtBluetoothSPP;
import engine.clientevents.BluetoothError;
import features.MbtDeviceType;
import utils.MbtAsyncWaitOperation;

import static java.util.UUID.fromString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MbtBluetoothLETest {

    private Context context;

    /**
     * Class under test
     */
    private MbtBluetoothLE bluetoothLE;

    private MbtBluetoothManager bluetoothManager;

    private BluetoothGatt gatt;

    private BluetoothGattService gattService;

    private BluetoothGattCharacteristic characteristic;

    private BluetoothGattDescriptor descriptor;

    private CommandInterface.MbtCommand command;

    private MbtAsyncWaitOperation asyncOperation;

    private final byte[] BYTE_REQUEST = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private final byte[] BYTE_RESPONSE = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final UUID SERVICE = MelomindCharacteristics.SERVICE_MEASUREMENT;
    private final UUID CHARACTERISTIC_MAILBOX = MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX;
    private final UUID CHARACTERISTIC_EEG = MelomindCharacteristics.CHARAC_MEASUREMENT_EEG;
    private final UUID DESCRIPTOR = MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID;
    private final UUID UNKNOWN = fromString("0-0-0-0-0");

    private final String DEVICE_NAME = "melo_1010100100";

    @Before
    public void setUp() {

        context = Mockito.mock(Context.class);
        bluetoothManager = Mockito.mock(MbtBluetoothManager.class);
        bluetoothLE = new MbtBluetoothLE(context, bluetoothManager);

        gatt = Mockito.mock(BluetoothGatt.class);
        bluetoothLE.gatt = gatt;
        gattService = Mockito.mock(BluetoothGattService.class);
        characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        descriptor = Mockito.mock(BluetoothGattDescriptor.class);

        command = Mockito.mock(CommandInterface.MbtCommand.class);
        asyncOperation = Mockito.mock(MbtAsyncWaitOperation.class);

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
//        bluetoothLE.gatt = gatt;
//        assertTrue(bluetoothLE.startStream());
//
//    }

    /**
     * Assert that streaming cannot be started if headset is idle
     */
    @Test
    public void startStream_Idle() {
        bluetoothLE.notifyStreamStateChanged(StreamState.IDLE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming state become IDLE if headset is disconnected
     */
    @Test
    public void startStream_StreamStoppedAfterDisconnection() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
        assertFalse(bluetoothLE.isStreaming());
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset is disconnected
     */
    @Test
    public void startStream_Disconnected() {
        bluetoothLE.notifyStreamStateChanged(StreamState.DISCONNECTED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming return true if the stream state is started
     */
    @Test
    public void startStream_Started() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        assertTrue(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if streaming has just been stopped
     */
    @Test
    public void startStream_Stopped() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if gatt is null
     */
    @Test
    public void startStream_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset is connected BUT not ready
     */
    @Test
    public void startStream_HeadsetNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection is in progress
     */
    @Test
    public void startStream_Connecting() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTING);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset disconnection is in progress
     */
    @Test
    public void startStream_Disconnecting() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTING);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection that was in progress has failed
     */
    @Test
    public void startStream_ConnectionFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection that was in progress has failed because Bluetooth is off on the user's mobile device
     */
    @Test
    public void startStream_NotConnectedBluetoothDisabled() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BLUETOOTH_DISABLED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection that was in progress has failed because Bluetooth is not supported by the user's mobile device
     */
    @Test
    public void startStream_NotConnectedBluetoothNotSupported() {
        bluetoothLE.notifyConnectionStateChanged(BtState.NO_BLUETOOTH);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection that was in progress has failed because Location is off on the user's mobile device
     */
    @Test
    public void startStream_NotConnectedLocationDisabled() {
        bluetoothLE.notifyConnectionStateChanged(BtState.LOCATION_DISABLED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection that was in progress has failed because Bluetooth is off on the user's mobile device
     */
    @Test
    public void startStream_NotConnectedLocationPermissionNotGranted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.LOCATION_PERMISSION_NOT_GRANTED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection was interrupted by the user
     */
    @Test
    public void startStream_ConnectionInterrupted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if connection process is still in progress, at the services discovering step
     */
    @Test
    public void startStream_DiscoveringServices() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_SERVICES);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection has failed while it was discovering services
     */
    @Test
    public void startStream_DiscoveringFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if connection process is still in progress, at the device info reading step
     */
    @Test
    public void startStream_ReadingDeviceInfo() {
        bluetoothLE.notifyConnectionStateChanged(BtState.READING_FIRMWARE_VERSION);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection has failed while it was reading device info
     */
    @Test
    public void startStream_ReadingFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.READING_FAILURE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if connection process is still in progress, at the bonding step
     */
    @Test
    public void startStream_Bonding() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BONDING);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset connection has failed while it was requesting bonding
     */
    @Test
    public void startStream_BondingFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BONDING_FAILURE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if firmware version upgrading is in progress
     */
    @Test
    public void startStream_Upgrading() {
        bluetoothLE.notifyConnectionStateChanged(BtState.UPGRADING);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if firmware version upgrade has failed
     */
    @Test
    public void startStream_UpgradingFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.UPGRADING_FAILURE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that status monitoring can not be activated if gatt is null
     */
    @Test
    public void activateDeviceStatusMonitoring_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.activateDeviceStatusMonitoring());
    }

    /**
     * Assert that status monitoring can not be activated if headset is not connected
     */
    @Test
    public void activateDeviceStatusMonitoring_Disconnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
        assertFalse(bluetoothLE.activateDeviceStatusMonitoring());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas any streaming was in progress and the current state was IDLE
     */
    @Test
    public void stopStream_NotStreamingIdle() {
        bluetoothLE.notifyStreamStateChanged(StreamState.IDLE);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas the headset is disconnected
     */
    @Test
    public void stopStream_NotStreamingDisconnected() {
        bluetoothLE.notifyStreamStateChanged(StreamState.DISCONNECTED);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas it was already stopped
     */
    @Test
    public void stopStream_StreamingAlreadyStopped() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that a streaming in progress is not stopped if service is not found
     */
    @Test
    public void stopStream_UnknownService() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        when(gatt.getService(SERVICE)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that a streaming in progress is not stopped if characteristic is not found
     */
    @Test
    public void stopStream_UnknownCharacteristic() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that a streaming in progress is not stopped if gatt is not found
     */
    @Test
    public void stopStream_NullGatt() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas notification disabling is not working (streaming is in progress and service & characteristic are valid)
     */
    @Test
    public void stopStream_DisablingNotificationFail() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that streaming in progress is stopped if service & characteristic are valid and notification disabling succeeded
     */
    @Test
    public void stopStream_DisableNotificationWorking() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        when(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), false)).thenReturn(true);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(descriptor);


        bluetoothLE.gatt = gatt;
        //assertNull(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        //assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that streaming is in progress if stream has started
     */
    @Test
    public void notifyStreamStateChanged_Started() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        assertTrue(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream has stopped
     */
    @Test
    public void notifyStreamStateChanged_Stopped() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream has failed to start
     */
    @Test
    public void notifyStreamStateChanged_Failed() {
        bluetoothLE.notifyStreamStateChanged(StreamState.FAILED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream state is IDLE
     */
    @Test
    public void notifyStreamStateChanged_Idle() {
        bluetoothLE.notifyStreamStateChanged(StreamState.IDLE);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream state is IDLE
     */
    @Test
    public void notifyStreamStateChanged_Vpro() {
        MbtConfig.setDeviceType(MbtDeviceType.VPRO);
        MbtBluetoothSPP bluetoothSPP = new MbtBluetoothSPP(context, bluetoothManager);
        bluetoothSPP.notifyStreamStateChanged(StreamState.STARTED);
        assertFalse(bluetoothLE.isStreaming());
    }


    /**
     * Assert that Bluetooth is not streaming if the streaming failed to start
     */
    @Test
    public void isStreaming_Null() {
        bluetoothLE.notifyStreamStateChanged(StreamState.FAILED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that Bluetooth is streaming if headset is disconnected
     */
    @Test
    public void isStreaming_Disconnected() {
        bluetoothLE.notifyStreamStateChanged(StreamState.DISCONNECTED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that Bluetooth is streaming if stream is started
     */
    @Test
    public void isStreaming_Started() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        assertTrue(bluetoothLE.isStreaming());
    }

    /**
     * Assert that Bluetooth is not streaming if streaming is stopped
     */
    @Test
    public void isStreaming_Stopped() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that bluetooth is not streaming if headset is idle
     */
    @Test
    public void isStreaming_Idle() {
        bluetoothLE.notifyStreamStateChanged(StreamState.IDLE);
        assertFalse(bluetoothLE.isConnected());
    }

    /**
     * Assert that BLE is not streaming if headset is a vpro
     */
    @Test
    public void isStreaming_Vpro() {
        MbtConfig.setDeviceType(MbtDeviceType.VPRO);
        MbtBluetoothSPP bluetoothSPP = new MbtBluetoothSPP(context, bluetoothManager);
        bluetoothSPP.notifyStreamStateChanged(StreamState.STARTED);
        assertFalse(bluetoothLE.isConnected());
    }

    /**
     * Assert that notifications can not be enabled if headset is not connected
     */
    @Test
    public void enableOrDisableNotificationsOnCharacteristic_DisconnectedEnable() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
        assertFalse(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(true, null));

    }

    /**
     * Assert that notifications can not be disabled if headset is not connected
     */
    @Test
    public void enableOrDisableNotificationsOnCharacteristic_DisconnectedDisable() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
        assertFalse(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(false, null));

    }

    /**
     * Assert that notifications can be enabled if headset is connected and ready
     */
    @Test
    public void enableOrDisableNotificationsOnCharacteristic_ConnectedReadyEnable() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        when(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), false)).thenReturn(true);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(descriptor);

        when(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), true)).thenReturn(true);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true);
        when(gatt.writeDescriptor(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR))).thenReturn(true);

        bluetoothLE.gatt = gatt;

        assertTrue(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(true, bluetoothLE.gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)));

    }

    /**
     * Assert that notifications can be disabled if headset is connected and ready
     */
    @Test
    public void enableOrDisableNotificationsOnCharacteristic_ConnectedReadyDisable() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        when(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), false)).thenReturn(true);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR)).thenReturn(descriptor);

        when(gatt.setCharacteristicNotification(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG), true)).thenReturn(true);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true);
        when(gatt.writeDescriptor(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG).getDescriptor(DESCRIPTOR))).thenReturn(true);

        bluetoothLE.gatt = gatt;

        assertTrue(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(false, bluetoothLE.gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)));

    }


    @Test
    public void startLowEnergyScan_FailedNullNameFilterOn() {
        assertFalse(bluetoothLE.startScan());
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILURE);
    }

    @Test
    public void startLowEnergyScan_FailedNotNullNameFilterOn() {
        assertFalse(bluetoothLE.startScan());
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILURE);
    }

    /**
     * Check that the current state is no more SCAN STARTED after stopping the scan
     */
    @Test
    public void stopLowEnergyScan_StateIsNotScanStarted() {
        bluetoothLE.stopScan();
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.SCAN_STARTED);
    }

    /**
     * Check that the current state is IDLE after stopping the scan
     */
    @Test
    public void stopLowEnergyScan_StateIsIdle() {
        bluetoothLE.stopScan();
        assertEquals(bluetoothLE.getCurrentState(), BtState.IDLE);
    }

    /**
     * Check that the current state is IDLE after stopping the scan
     */
    @Test
    public void stopLowEnergyScan_StartedStoppedFailed() {
        bluetoothLE.startScan();
        bluetoothLE.stopScan();
    }


    @Test
    public void connect_NonNullContext() {
        BluetoothDevice device = Mockito.mock(BluetoothDevice.class);
        assertTrue(bluetoothLE.connect(context, device));
        assertNull(bluetoothLE.gatt);
    }

    @Test
    public void connect_NullContext() {
        BluetoothDevice device = Mockito.mock(BluetoothDevice.class);
        assertFalse(bluetoothLE.connect(null, device));
        assertNotNull(bluetoothLE.gatt);
    }

    @Test
    public void connect_NullDevice() {
        assertFalse(bluetoothLE.connect(context, null));
        assertNotNull(bluetoothLE.gatt);
    }


    /**
     * check that disconnection fail if there is no connected headset
     */
    @Test
    public void disconnect_NoConnectedHeadset() {
        assertFalse(bluetoothLE.disconnect());
    }

    /**
     * check that disconnection fail if gatt is null
     */
    @Test
    public void disconnect_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.disconnect());
    }

    /**
     * check that gatt is reset to null after disconnection
     */
    @Test
    public void disconnect_GattNotNull() {
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.disconnect());
        assertNull(bluetoothLE.gatt);
    }

    @Test
    public void isConnected_ConnectedAndReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY, true);
        assertTrue(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Connected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Disconnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ScanFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_AudioConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.AUDIO_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ConnectionFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTION_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ConnectionInterrupted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_InternalFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_BluetoothDisabled() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BLUETOOTH_DISABLED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ReadingDeviceInfo() {
        bluetoothLE.notifyConnectionStateChanged(BtState.READING_FIRMWARE_VERSION, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ReadingFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.READING_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ScanTimeout() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_TIMEOUT, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Bonding() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BONDING, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_AnotherDeviceConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.ANOTHER_DEVICE_CONNECTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_BondingFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BONDING_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Connecting() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTING, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_DeviceFound() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DEVICE_FOUND, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Disconnecting() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTING, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_DiscoveringFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Discovering() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_SERVICES, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Idle() {
        bluetoothLE.notifyConnectionStateChanged(BtState.IDLE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_LocationIsRequired() {
        bluetoothLE.notifyConnectionStateChanged(BtState.LOCATION_DISABLED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_LocationPermission() {
        bluetoothLE.notifyConnectionStateChanged(BtState.LOCATION_PERMISSION_NOT_GRANTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_BluetoothNotSupported() {
        bluetoothLE.notifyConnectionStateChanged(BtState.NO_BLUETOOTH, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ScanFailedAlreadyStarted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_FAILED_ALREADY_STARTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ScanInterrupted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_INTERRUPTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_ScanStarted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_STARTED, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_UpgradingFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.UPGRADING_FAILURE, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void isConnected_Upgrading() {
        bluetoothLE.notifyConnectionStateChanged(BtState.UPGRADING, true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void startReadOperation_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION));
        assertEquals(bluetoothLE.getCurrentState(), BtState.READING_FAILURE);
    }

    @Test
    public void startReadOperation_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION));
    }

    @Test
    public void startReadOperation_GattCharacteristicNull() {
        assertFalse(bluetoothLE.startReadOperation(UNKNOWN));
    }

    /**
     * Check that startWriteOperation return false if gatt is invalid
     */
    @Test
    public void startWriteOperation_GattInvalid() {
        bluetoothLE.gatt = null;
        byte[] code = {DeviceCommandEvents.MBX_GET_EEG_CONFIG};
        assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code));
    }

    /**
     * Check that startWriteOperation return false if gatt service is invalid
     */
    @Test
    public void startWriteOperation_ServiceInvalid() {
        byte[] code = {DeviceCommandEvents.MBX_GET_EEG_CONFIG};
        bluetoothLE.gatt = gatt;
        when(gatt.getService(SERVICE)).thenReturn(null);

        assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code));
    }

    /**
     * Check that startWriteOperation return false if gatt characteristic is invalid
     */
    @Test
    public void startWriteOperation_CharacteristicInvalid() {
        byte[] code = {DeviceCommandEvents.MBX_GET_EEG_CONFIG};
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        BluetoothGattCharacteristic characteristic = null;
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code));
    }

    /**
     * Check that startWriteOperation return false if gatt write characteristic is invalid
     */
    @Test
    public void startWriteOperation_WriteCharacteristicInvalid() {
        byte[] code = {DeviceCommandEvents.MBX_GET_EEG_CONFIG};
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(false);

        assertFalse(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code));
    }

    /**
     * Check that startWriteOperation return true if gatt write characteristic is valid
     */
    @Test
    public void startWriteOperation_Valid() {
        byte[] code = {DeviceCommandEvents.MBX_GET_EEG_CONFIG};
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.startWriteOperation(SERVICE, CHARACTERISTIC_MAILBOX, code));
    }

    /**
     * Checks that checkServiceAndCharacteristicValidity returns true if
     * a valid service and a valid characteristic are used.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_Valid() {
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gatt.getService(SERVICE).getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        assertTrue(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG));
    }

    /**
     * Checks that checkServiceAndCharacteristicValidity returns false if
     * a invalid service and a valid characteristic are used.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_InvalidService() {
        when(gatt.getService(SERVICE)).thenReturn(null);
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG));
    }

    /**
     * Checks that checkServiceAndCharacteristicValidity returns false if
     * a valid service and a invalid characteristic are used.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_InvalidCharacteristic() {
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(null);
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG));
    }

    /**
     * Checks that checkServiceAndCharacteristicValidity returns false if
     * a invalid gatt is used.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_InvalidGatt() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG));
    }


    /**
     * Check that false is returned if gatt is invalid
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_InvalidGatt() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));
    }

    /**
     * Check that false is returned if service is invalid
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_InvalidService() {
        when(gatt.getService(SERVICE)).thenReturn(null);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));
    }

    /**
     * Check that false is returned if characteristic is invalid
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_InvalidCharacteristic() {

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(null);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));

    }

    /**
     * Check that false is returned if characteristic is invalid
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_InvalidDescriptor() {
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        when(characteristic.getDescriptor(DESCRIPTOR)).thenReturn(descriptor);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));

    }

    /**
     * Check that true is returned if the descriptor is valid
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_ValidDescriptor() {
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_EEG)).thenReturn(characteristic);
        when(characteristic.getDescriptor(DESCRIPTOR)).thenReturn(descriptor);
        when(descriptor.getValue()).thenReturn(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));

    }

    @Test
    public void readBattery_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.readBattery());
        assertEquals(bluetoothLE.getCurrentState(), BtState.READING_FAILURE);
    }

    @Test
    public void readBattery_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readBattery());
    }

    @Test
    public void readBattery_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readBattery());
    }

    @Test
    public void readFwVersion_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.readFwVersion());
        assertEquals(bluetoothLE.getCurrentState(), BtState.READING_FAILURE);
    }

    @Test
    public void readFwVersion_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readFwVersion());
    }

    @Test
    public void readFwVersion_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readFwVersion());
    }

    @Test
    public void readHwVersion_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.readHwVersion());
        assertEquals(bluetoothLE.getCurrentState(), BtState.READING_FAILURE);
    }

    @Test
    public void readHwVersion_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readHwVersion());
    }

    @Test
    public void readHwVersion_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readHwVersion());
    }


    @Test
    public void readSerialNumber_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertFalse(bluetoothLE.readSerialNumber());
        assertEquals(bluetoothLE.getCurrentState(), BtState.READING_FAILURE);
    }

    @Test
    public void readSerialNumber_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readSerialNumber());
    }

    @Test
    public void readSerialNumber_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readSerialNumber());
    }

    @Test
    public void onNotificationStateChanged_started() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_EEG, 0, 0);
        bluetoothLE.onNotificationStateChanged(true, characteristic, true);
        //assertTrue(bluetoothLE.isStreaming()); //todo check why characteristic.getuuid is null
    }

    @Test
    public void onNotificationStateChanged_stopped() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_EEG, 0, 0);
        bluetoothLE.onNotificationStateChanged(true, characteristic, false);
        assertFalse(bluetoothLE.isStreaming());
    }

    @Test
    public void onNotificationStateChanged_failed() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_EEG, 0, 0);
        bluetoothLE.onNotificationStateChanged(false, characteristic, false);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that streaming state become IDLE if headset is disconnected
     */
    @Test
    public void notifyConnectionStateChanged_DisconnectionDuringStreaming() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        assertTrue(bluetoothLE.isStreaming());
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that streaming state do not change if streaming is in progress and the new state is not equal to DATA_BT_DISCONNECTED
     */
    @Test
    public void notifyConnectionStateChanged_ConnectionWhileStreaming() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertTrue(bluetoothLE.isStreaming());
    }

    /**
     * Assert that streaming state do not change if streaming is not progress and the new state is to DATA_BT_DISCONNECTED
     */
    @Test
    public void notifyConnectionStateChanged_DisconnectionDuringStoppedStream() {
        bluetoothLE.notifyStreamStateChanged(StreamState.STOPPED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that MTU is not changed if gatt is null
     */
    @Test
    public void changeMTU_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.changeMTU(47));
    }

    /**
     * Check that MTU is not changed if gatt requestMtu return false
     */
    @Test
    public void changeMTU_RequestMtuFailure() {
        Mockito.when(gatt.requestMtu(Mockito.anyInt())).thenReturn(false);
        assertFalse(bluetoothLE.changeMTU(47));
    }

    /**
     * Check that MTU is changed if gatt requestMtu return true
     */
    @Test
    public void changeMTU_RequestMtuSuccess() {
        Mockito.when(gatt.requestMtu(Mockito.anyInt())).thenReturn(true);
        assertTrue(bluetoothLE.changeMTU(47));
    }

    /**
     * Check that gatt services cannot be discovered if gatt is disconnected
     */
    @Test
    public void disconnectHeadsetAlreadyConnected_DisconnectionSuccess() {
//        bluetoothLE.gatt = gatt; //todo mock gatt.getdevice.getname
//        bluetoothLE.disconnectHeadsetAlreadyConnected(DEVICE_NAME,true);
//        assertFalse(bluetoothLE.gatt.discoverServices());
    }

    /**
     * Check that gatt services cannot be discovered if gatt is disconnected
     */
    @Test
    public void disconnectHeadsetAlreadyConnected_DisconnectionFailed() {
//        bluetoothLE.gatt = gatt; //todo mock gatt.getdevice.getname
//        bluetoothLE.disconnectHeadsetAlreadyConnected(DEVICE_NAME,false);
//        assertFalse(bluetoothLE.gatt.discoverServices());
    }

    @Test
    public void connectA2DPFromBLE_NullGatt() {
        //todo mock
    }

    @Test
    public void disconnectA2DPFromBLE_NullGatt() {
        bluetoothLE.gatt = null;
        //assertFalse(bluetoothLE.disconnectA2DPFromBLE());
    }

    @Test
    public void requestBonding() {
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
    public void notifyConnectionStateChanged_notifyClientConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS);

        assertEquals(bluetoothLE.getCurrentState(), BtState.DATA_BT_CONNECTION_SUCCESS);
    }

    @Test
    public void notifyConnectionStateChanged_doNotNotifyClientConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS);

        assertEquals(bluetoothLE.getCurrentState(), BtState.DATA_BT_CONNECTION_SUCCESS);
    }

    @Test
    public void notifyConnectionStateChanged_notifyClientDisconnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);

        assertEquals(bluetoothLE.getCurrentState(), BtState.IDLE);
    }

    @Test
    public void enableBluetoothOnDevice() {
    }


    /**
     * Check that a command is not sent
     * and that an error is raised
     * if the connection state is invalid
     */
    @Test
    public void sendCommand_invalidConnectionState(){
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTING);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(DESCRIPTOR)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        bluetoothLE.sendCommand(command);

        Mockito.verify(command).onError(BluetoothError.ERROR_NOT_CONNECTED, null);
        Mockito.verifyZeroInteractions(gatt); //a valid command interacts with gatt to write the characteristic/request the mtu
        Mockito.verifyZeroInteractions(asyncOperation); //a valid command interacts with an asyncOperation that wait until the response is received (or until timeout)
        Mockito.verify(bluetoothManager).notifyResponseReceived(null, command); //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
    }

    /**
     * Check that a command is not sent
     * and that an error is raised if the command is invalid
     */
    @Test
    public void sendCommand_invalidCommand(){
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED);
        when(command.isValid()).thenReturn(false);

        bluetoothLE.sendCommand(command);

        Mockito.verifyZeroInteractions(gatt); //a valid command interact with gatt to write the characteristic/request the mtu
        Mockito.verifyZeroInteractions(asyncOperation); //a valid command interacts with an asyncOperation that wait until the response is received (or until timeout)
        Mockito.verify(bluetoothManager).notifyResponseReceived(null, command); //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent

    }


    /**
     * Check that a command is not sent
     * and that an error is raised
     * if the sending operation encountered a problem (mock sendRequestData)
     */
    @Test
    public void sendCommand_failureSendRequestData(){
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED);
        when(command.isValid()).thenReturn(true);

        bluetoothLE.sendCommand(command);

        Mockito.verify(command).onError(BluetoothError.ERROR_REQUEST_OPERATION, null);
        Mockito.verifyZeroInteractions(asyncOperation); //a valid command interacts with an asyncOperation that wait until the response is received (or until timeout)
        Mockito.verify(bluetoothManager).notifyResponseReceived(null, command); //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
    }



    /**
     * Check that a command is sent and a response is returned to the client
     * if a device is connected , the command is valid and the send request data method succeeded
     *
     */
    @Test
    public void sendCommand_SuccessWithResponse() throws InterruptedException, ExecutionException, TimeoutException {
        final int mtu = 47;
        final int timeout = 11000;
        command = Mockito.mock(BluetoothCommands.Mtu.class);
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED);
        bluetoothLE.setLock(asyncOperation); //if this setter is not called, the async configuration mock is not called
        when(command.isValid()).thenReturn(true);
        when(command.serialize()).thenReturn(mtu);
        when(gatt.requestMtu(47)).thenReturn(true);
        when(command.isResponseExpected()).thenReturn(true);
        when(asyncOperation.waitOperationResult(timeout)).thenReturn(mtu);
        bluetoothLE.sendCommand(command);

        Mockito.verify(command).serialize();
        Mockito.verify(gatt).requestMtu(47);
        Mockito.verify(command).isResponseExpected();
        Mockito.verify(command).onRequestSent();
        Mockito.verify(asyncOperation).waitOperationResult(timeout);
        Mockito.verify(command).onResponseReceived(mtu);
        Mockito.verify(bluetoothManager).notifyResponseReceived(mtu, command); //non null response is supposed to be returned to notify the bluetooth manager that the command has succeeded to be sent
    }

    /**
     * Check that a command is sent and no response is returned to the client
     * if a device is connected , the command is valid and the send request data method succeeded
     */
    @Test
    public void sendCommand_SuccessNoResponse(){
        final int timeout = 11000;
        command = Mockito.mock(DeviceCommands.Reboot.class);
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED);
        when(command.isValid()).thenReturn(true);
        when(command.serialize()).thenReturn(BYTE_REQUEST);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);
        when(characteristic.getUuid()).thenReturn(CHARACTERISTIC_MAILBOX);
        when(characteristic.getDescriptor(DESCRIPTOR)).thenReturn(descriptor);
        when(descriptor.getValue()).thenReturn(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        when(gatt.setCharacteristicNotification(characteristic, true)).thenReturn(true);
        when(descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true);
        when(gatt.writeDescriptor(descriptor)).thenReturn(true);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);
        when(command.isResponseExpected()).thenReturn(false);

        bluetoothLE.sendCommand(command);

        Mockito.verify(command).serialize();
        Mockito.verify(command).onRequestSent();
        Mockito.verify(command).isResponseExpected();
        Mockito.verifyZeroInteractions(asyncOperation); // no response is expected so the async operation does not wait for any response
        Mockito.verify(bluetoothManager).notifyResponseReceived(null, command); //null response is supposed to be returned to notify the bluetooth manager that the command has failed to be sent
    }

}

