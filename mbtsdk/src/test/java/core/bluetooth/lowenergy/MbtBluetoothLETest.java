package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.util.UUID;

import command.DeviceCommandOld;
import command.DeviceCommands;
import command.DeviceStreamingCommands;
import command.MailboxEvents;
import config.AmpGainConfig;
import config.FilterConfig;
import config.MbtConfig;
import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.spp.MbtBluetoothSPP;
import engine.SimpleRequestCallback;
import features.MbtDeviceType;

import static java.util.UUID.fromString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
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

//    public EventBusManager.Callback busCallback;
//    public SimpleRequestCallback<byte[]> clientCallback;

    private final byte[] BYTE_RESPONSE = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private final UUID SERVICE = MelomindCharacteristics.SERVICE_MEASUREMENT;
    private final UUID CHARACTERISTIC_MAILBOX = MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX;
    private final UUID CHARACTERISTIC_EEG = MelomindCharacteristics.CHARAC_MEASUREMENT_EEG;
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
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.IDLE);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming state become IDLE if headset is disconnected
     */
    @Test
    public void startStream_StreamStoppedAfterDisconnection() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
        assertFalse(bluetoothLE.isStreaming());
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if headset is disconnected
     */
    @Test
    public void startStream_Disconnected() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.DISCONNECTED);
        assertFalse(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming return true if the stream state is started
     */
    @Test
    public void startStream_Started() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertTrue(bluetoothLE.startStream());
    }

    /**
     * Assert that streaming cannot be started if streaming has just been stopped
     */
    @Test
    public void startStream_Stopped() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
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
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.IDLE);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas the headset is disconnected
     */
    @Test
    public void stopStream_NotStreamingDisconnected() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.DISCONNECTED);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas it was already stopped
     */
    @Test
    public void stopStream_StreamingAlreadyStopped() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that a streaming in progress is not stopped if service is not found
     */
    @Test
    public void stopStream_UnknownService() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that a streaming in progress is not stopped if characteristic is not found
     */
    @Test
    public void stopStream_UnknownCharacteristic() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that a streaming in progress is not stopped if gatt is not found
     */
    @Test
    public void stopStream_NullGatt() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that nothing happens if user requests stopping streaming whereas notification disabling is not working (streaming is in progress and service & characteristic are valid)
     */
    @Test
    public void stopStream_DisablingNotificationFail() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(characteristic);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.stopStream());
    }

    /**
     * Check that streaming in progress is stopped if service & characteristic are valid and notification disabling succeeded
     */
    @Test
    public void stopStream_DisableNotificationWorking() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(characteristic);
        BluetoothGattDescriptor descriptor = Mockito.mock(BluetoothGattDescriptor.class);
        when(gatt.setCharacteristicNotification(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG), false)).thenReturn(true);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);


        bluetoothLE.gatt = gatt;
        //assertNull(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        //assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that streaming is in progress if stream has started
     */
    @Test
    public void notifyStreamStateChanged_Started() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertTrue(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream has stopped
     */
    @Test
    public void notifyStreamStateChanged_Stopped() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream has failed to start
     */
    @Test
    public void notifyStreamStateChanged_Failed() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.FAILED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream state is IDLE
     */
    @Test
    public void notifyStreamStateChanged_Idle() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.IDLE);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that streaming is not in progress if stream state is IDLE
     */
    @Test
    public void notifyStreamStateChanged_Vpro() {
        MbtConfig.setDeviceType(MbtDeviceType.VPRO);
        MbtBluetoothSPP bluetoothSPP = new MbtBluetoothSPP(context, bluetoothManager);
        bluetoothSPP.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertFalse(bluetoothLE.isStreaming());
    }


    /**
     * Assert that Bluetooth is not streaming if the streaming failed to start
     */
    @Test
    public void isStreaming_Null() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.FAILED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that Bluetooth is streaming if headset is disconnected
     */
    @Test
    public void isStreaming_Disconnected() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.DISCONNECTED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that Bluetooth is streaming if stream is started
     */
    @Test
    public void isStreaming_Started() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertTrue(bluetoothLE.isStreaming());
    }

    /**
     * Assert that Bluetooth is not streaming if streaming is stopped
     */
    @Test
    public void isStreaming_Stopped() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that bluetooth is not streaming if headset is idle
     */
    @Test
    public void isStreaming_Idle() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.IDLE);
        assertFalse(bluetoothLE.isConnected());
    }

    /**
     * Assert that BLE is not streaming if headset is a vpro
     */
    @Test
    public void isStreaming_Vpro() {
        MbtConfig.setDeviceType(MbtDeviceType.VPRO);
        MbtBluetoothSPP bluetoothSPP = new MbtBluetoothSPP(context, bluetoothManager);
        bluetoothSPP.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
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
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(characteristic);
        BluetoothGattDescriptor descriptor = Mockito.mock(BluetoothGattDescriptor.class);
        when(gatt.setCharacteristicNotification(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG), false)).thenReturn(true);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);

        when(gatt.setCharacteristicNotification(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG), true)).thenReturn(true);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true);
        when(gatt.writeDescriptor(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID))).thenReturn(true);

        bluetoothLE.gatt = gatt;

        assertTrue(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(true, bluetoothLE.gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)));

    }

    /**
     * Assert that notifications can be disabled if headset is connected and ready
     */
    @Test
    public void enableOrDisableNotificationsOnCharacteristic_ConnectedReadyDisable() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(characteristic);
        BluetoothGattDescriptor descriptor = Mockito.mock(BluetoothGattDescriptor.class);
        when(gatt.setCharacteristicNotification(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG), false)).thenReturn(true);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);

        when(gatt.setCharacteristicNotification(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG), true)).thenReturn(true);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)).thenReturn(true);
        when(gatt.writeDescriptor(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID))).thenReturn(true);

        bluetoothLE.gatt = gatt;

        assertTrue(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(false, bluetoothLE.gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)));

    }


    @Test
    public void startLowEnergyScan_FailedNullNameFilterOn() {
        assertFalse(bluetoothLE.startLowEnergyScan(true));
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILURE);
    }

    @Test
    public void startLowEnergyScan_FailedNotNullNameFilterOn() {
        assertFalse(bluetoothLE.startLowEnergyScan(true));
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILURE);
    }

    /**
     * Check that the current state is no more SCAN STARTED after stopping the scan
     */
    @Test
    public void stopLowEnergyScan_StateIsNotScanStarted() {
        bluetoothLE.stopLowEnergyScan();
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.SCAN_STARTED);
    }

    /**
     * Check that the current state is IDLE after stopping the scan
     */
    @Test
    public void stopLowEnergyScan_StateIsIdle() {
        bluetoothLE.stopLowEnergyScan();
        assertEquals(bluetoothLE.getCurrentState(), BtState.IDLE);
    }

    /**
     * Check that the current state is IDLE after stopping the scan
     */
    @Test
    public void stopLowEnergyScan_StartedStoppedFailed() {
        bluetoothLE.startLowEnergyScan(true);
        bluetoothLE.stopLowEnergyScan();
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
        bluetoothLE.gatt = Mockito.mock(BluetoothGatt.class);
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

    @Test
    public void startWriteOperation_GattNull() {
        bluetoothLE.gatt = null;
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};
        assertFalse(bluetoothLE.startWriteOperation(CHARACTERISTIC_MAILBOX, code));
    }

    @Test
    public void startWriteOperation_GattCharacteristicNull() {
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};
        assertFalse(bluetoothLE.startWriteOperation(UNKNOWN, code));
    }

    /**
     * Checks that the Measurement service and the EEG characteristic that are going to be used to communicate with the remote device are valid.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_SuccessMeasurementEEG() {
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(characteristic);
        bluetoothLE.gatt = gatt;
        assertNotNull(bluetoothLE.gatt);
        assertTrue(bluetoothLE.checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
        assertNotEquals(bluetoothLE.getCurrentState(), BtState.READING_FAILURE);

    }

    /**
     * Checks that an unknown service with a EEG characteristic that are going to be used to communicate with the remote device are invalid.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_UnknownService() {
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(null);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
    }

    /**
     * Checks that the Measurement service and an unknown characteristic that are going to be used to communicate with the remote device are invalid.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_UnknownCharacteristic() {
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattService gattService = Mockito.mock(BluetoothGattService.class);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(MelomindCharacteristics.SERVICE_MEASUREMENT);
        when(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
    }

    /**
     * Checks that any service and any characteristic that are going to be used to communicate with the remote device are invalid if their associated gatt is null.
     */
    @Test
    public void checkServiceAndCharacteristicValidity_NullGatt() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE, CHARACTERISTIC_EEG));
    }

    /**
     * Checks that if the Measurement service and the EEG characteristic have been inverted, the given service and characteristic are considered invalid
     */
    @Test
    public void checkServiceAndCharacteristicValidity_InvertedServiceAndCharacteristic() {
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        when(gatt.getService(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG)).thenReturn(null);
        bluetoothLE.gatt = gatt;
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG, MelomindCharacteristics.SERVICE_MEASUREMENT));
    }

    /**
     * Check that false is returned if gatt is null
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_NullGatt() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));
    }

    /**
     * Check that false is returned if gatt services are null
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_NullGattServices() {
        bluetoothLE.gatt = Mockito.mock(BluetoothGatt.class);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(UNKNOWN, CHARACTERISTIC_MAILBOX)); // unknown is "0" should not exist so this should return false
    }

    /**
     * Check that false is returned if gatt service is not null but characteristic does not exist
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_NullGattCharacteristic() {
        bluetoothLE.gatt = Mockito.mock(BluetoothGatt.class);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(SERVICE, CHARACTERISTIC_MAILBOX));

    }

    /**
     * Check that false is returned if gatt service is not null but characteristic does not exist
     */
    @Test
    public void isNotificationEnabledOnCharacteristic_InvertedServiceAndCharacteristic() {
        bluetoothLE.gatt = Mockito.mock(BluetoothGatt.class);
        assertFalse(bluetoothLE.isNotificationEnabledOnCharacteristic(CHARACTERISTIC_MAILBOX, SERVICE));

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
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG, 0, 0);
        bluetoothLE.onNotificationStateChanged(true, characteristic, true);
        //assertTrue(bluetoothLE.isStreaming()); //todo check why characteristic.getuuid is null
    }

    @Test
    public void onNotificationStateChanged_stopped() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG, 0, 0);
        bluetoothLE.onNotificationStateChanged(true, characteristic, false);
        assertFalse(bluetoothLE.isStreaming());
    }

    @Test
    public void onNotificationStateChanged_failed() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG, 0, 0);
        bluetoothLE.onNotificationStateChanged(false, characteristic, false);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that streaming state become IDLE if headset is disconnected
     */
    @Test
    public void notifyConnectionStateChanged_DisconnectionDuringStreaming() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertTrue(bluetoothLE.isStreaming());
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Assert that streaming state do not change if streaming is in progress and the new state is not equal to DATA_BT_DISCONNECTED
     */
    @Test
    public void notifyConnectionStateChanged_ConnectionWhileStreaming() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_CONNECTION_SUCCESS, true);
        assertTrue(bluetoothLE.isStreaming());
    }

    /**
     * Assert that streaming state do not change if streaming is not progress and the new state is to DATA_BT_DISCONNECTED
     */
    @Test
    public void notifyConnectionStateChanged_DisconnectionDuringStoppedStream() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
    }

    /**
     * Check that MTU is not changed if the new value is lower than the minimum required value
     */
    @Test
    public void changeMTU_LowerThanMinimum() {
        assertFalse(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.Mtu(0)));
    }
    /**
     * Check that MTU is not changed if the new value is higher than the maximum required value
     */
    @Test
    public void changeMTU_HigherThanMaximmum() {
        assertFalse(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.Mtu(200)));
    }

    /**
     * Check that MTU is not changed if the headset is not connected
     */
    @Test
    public void changeMTU_NotConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED, true);
        assertFalse(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.Mtu((200))));
    }

    /**
     * Check that MTU is not changed if gatt is null
     */
    @Test
    public void changeMTU_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.Mtu(47)));
    }

    /**
     * Filter configuration 50 Hz if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeFilterConfiguration_50() {
        //   assertFalse(bluetoothLE.changeFilterConfiguration(FilterConfig.NOTCH_FILTER_50HZ)); //todo mock
    }

    /**
     * Filter configuration 50 Hz if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeFilterConfiguration_60() {
        //  assertFalse(bluetoothLE.changeFilterConfiguration(FilterConfig.NOTCH_FILTER_60HZ)); //todo mock
    }

    /**
     * Filter configuration 50 Hz if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeFilterConfiguration_DEFAULT() {
        // assertFalse(bluetoothLE.changeFilterConfiguration(FilterConfig.NOTCH_FILTER_DEFAULT)); //todo mock
    }

    /**
     * Amplifier configuration x4 if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeAmpGainConfiguration_4() {
        // assertFalse(bluetoothLE.changeAmpGainConfiguration(AmpGainConfig.AMP_GAIN_X4_VLOW)); //todo mock
    }

    /**
     * Amplifier configuration x6 if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeAmpGainConfiguration_6() {
        // assertFalse(bluetoothLE.changeAmpGainConfiguration(AmpGainConfig.AMP_GAIN_X6_LOW)); //todo mock
    }

    /**
     * Amplifier configuration x8 if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeAmpGainConfiguration_8() {
        // assertFalse(bluetoothLE.changeAmpGainConfiguration(AmpGainConfig.AMP_GAIN_X8_MEDIUM)); //todo mock
    }

    /**
     * Amplifier configuration x12 if notification is enabled on mailbox characteristic
     */
    @Test
    public void changeAmpGainConfiguration_12() {
        // assertFalse(bluetoothLE.changeAmpGainConfiguration(AmpGainConfig.AMP_GAIN_X12_DEFAULT)); //todo mock
    }

    /**
     * P300 enabled if notification is enabled on mailbox characteristic
     */
    @Test
    public void switchP300Mode_useP300() {
        //assertFalse(bluetoothLE.switchP300Mode(true)); //todo mock
    }

    /**
     * P300 disabled if notification is enabled on mailbox characteristic
     */
    @Test
    public void switchP300Mode_donotuseP300() {
        //assertFalse(bluetoothLE.switchP300Mode(false)); //todo mock
    }

    @Test
    public void requestDeviceConfig() {
        //todo mock
    }

    /**
     * Check that gatt services cannot be discovered if gatt is disconnected
     */
    @Test
    public void disconnectHeadsetAlreadyConnected_DisconnectionSuccess() {
//        bluetoothLE.gatt = Mockito.mock(BluetoothGatt.class); //todo mock gatt.getdevice.getname
//        bluetoothLE.disconnectHeadsetAlreadyConnected(DEVICE_NAME,true);
//        assertFalse(bluetoothLE.gatt.discoverServices());
    }

    /**
     * Check that gatt services cannot be discovered if gatt is disconnected
     */
    @Test
    public void disconnectHeadsetAlreadyConnected_DisconnectionFailed() {
//        bluetoothLE.gatt = Mockito.mock(BluetoothGatt.class); //todo mock gatt.getdevice.getname
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
     * Check that a mailbox command is sent
     * if the required command is valid
     */
    @Test
    public void sendDeviceCommand_valid(){
        String serialNumber = DEVICE_NAME;
        DeviceCommandOld command = new DeviceCommands.UpdateSerialNumber(serialNumber);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        boolean isCommandSent = bluetoothLE.sendDeviceCommand(command);

        assertTrue(isCommandSent);
    }

    /**
     * Check that a mailbox command is not sent
     * if the required command is invalid (null)
     */
    @Test
    public void sendDeviceCommand_invalid(){
        //String serialNumber = null;
        //DeviceCommandOld command = new DeviceCommands.UpdateSerialNumber(serialNumber);
        DeviceCommandOld command = null;
        Mockito.doNothing().when(bluetoothManager).notifyDeviceResponseReceived(null, command);

        boolean isCommandSent = bluetoothLE.sendDeviceCommand(command);

        assertFalse(isCommandSent);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to update the serial number.
     */
    @Test
    public void sendDeviceCommand_sendSerialNumber(){
        String serialNumber = "1010100100";
        ByteBuffer nameToBytes = ByteBuffer.allocate(3 + serialNumber.getBytes().length);
        nameToBytes.put(MailboxEvents.MBX_SET_SERIAL_NUMBER);
        nameToBytes.put((byte) 0x53);
        nameToBytes.put((byte) 0x4D);
        nameToBytes.put(serialNumber.getBytes());

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
//        when(characteristic.setValue(rawRequest)).thenReturn(true);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceCommands.UpdateSerialNumber(serialNumber)));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to update the product name.
     */
    @Test
    public void sendDeviceCommand_sendProductName(){
        String productName = DEVICE_NAME;
        ByteBuffer nameToBytes = ByteBuffer.allocate(1 + productName.getBytes().length); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_PRODUCT_NAME);
        nameToBytes.put(productName.getBytes());

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceCommands.UpdateProductName(productName)));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to reboot the device.
     */
    @Test
    public void sendDeviceCommand_rebootDevice(){
        ByteBuffer nameToBytes = ByteBuffer.allocate(3);
        nameToBytes.put(MailboxEvents.MBX_SYS_REBOOT_EVT);
        nameToBytes.put((byte)0x29);
        nameToBytes.put((byte)0x08);

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceCommands.Reboot()));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to change the notch filter
     */
    @Test
    public void sendDeviceCommand_changeFilterConfiguration(){
        FilterConfig filterConfig = FilterConfig.NOTCH_FILTER_50HZ;
        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_NOTCH_FILT);
        nameToBytes.put((byte)filterConfig.getNumVal());

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.NotchFilter(filterConfig)));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to change the amplifier gain
     */
    @Test
    public void sendDeviceCommand_changeAmpGainConfiguration(){
        AmpGainConfig ampGainConfig = AmpGainConfig.AMP_GAIN_X12_DEFAULT;
        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_AMP_GAIN);
        nameToBytes.put((byte)ampGainConfig.getNumVal());

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.AmplifierGain(ampGainConfig)));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to enable the triggers
     */
    @Test
    public void sendDeviceCommand_enableTriggers(){
        boolean useP300 = true;
        test_enableOrDisableTriggers(useP300);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to disable the triggers
     */
    @Test
    public void sendDeviceCommand_disableTriggers(){
        boolean useP300 = false;
        test_enableOrDisableTriggers(useP300);
    }

    private void test_enableOrDisableTriggers(boolean useP300){
        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_P300_ENABLE);
        nameToBytes.put((byte)(useP300 ? 0x01 : 0x00));

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.Triggers(useP300)));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to enable the triggers
     */
    @Test
    public void sendDeviceCommand_enableDcOffset(){
        boolean enableDcOffset = true;
        test_enableOrDisableDcOffset(enableDcOffset);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to disable the triggers
     */
    @Test
    public void sendDeviceCommand_disableDcOffset(){
        boolean enableDcOffset = false;
        test_enableOrDisableDcOffset(enableDcOffset);
    }

    private void test_enableOrDisableDcOffset(boolean enableDcOffset){
        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_DC_OFFSET_ENABLE);
        nameToBytes.put((byte)(enableDcOffset ? 0x01 : 0x00));

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.DcOffset(enableDcOffset)));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to get the streaming config
     */
    @Test
    public void sendDeviceCommand_requestDeviceStreamingConfig(){
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.EegConfig(new SimpleRequestCallback<byte[]>() {
            @Override
            public void onRequestComplete(byte[] object) {

            }
        })));
        Mockito.verify(characteristic).setValue(code);
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }


    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to get the system status
     */
    @Test
    public void sendDeviceCommand_requestSystemStatus(){
        byte[] code = {MailboxEvents.MBX_SYS_GET_STATUS};

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceCommands.GetSystemStatus(new SimpleRequestCallback<byte[]>() {
            @Override
            public void onRequestComplete(byte[] object) {

            }
        })));
        Mockito.verify(characteristic).setValue(code);
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to update the external name.
     */
    @Test
    public void sendDeviceCommand_mtu(){
        int mtu = 47;
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.requestMtu(mtu)).thenReturn(true);

        assertTrue(bluetoothLE.sendDeviceCommand(new DeviceStreamingCommands.Mtu(mtu)));
        Mockito.verify(gatt).requestMtu(mtu);
    }

      /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to update the external name.
     */
    @Test
    public void sendExternalName_validName(){
        String externalName = "MM10001000";
        ByteBuffer nameToBytes = ByteBuffer.allocate(3 + externalName.getBytes().length); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_SERIAL_NUMBER);
        nameToBytes.put((byte) 0xAB);
        nameToBytes.put((byte) 0x21);
        nameToBytes.put(externalName.getBytes());

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.sendExternalName(externalName));
        Mockito.verify(characteristic).setValue(nameToBytes.array());
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox request is not sent
     * if the given name is valid
     */
    @Test
    public void sendExternalName_invalidName(){
        String externalName = null;

        assertFalse(bluetoothLE.sendExternalName(externalName));
        Mockito.verifyZeroInteractions(gatt);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to connect the audio
     */
    @Test
    public void connectA2DPFromBLE(){
        byte[] buffer = {MailboxEvents.MBX_CONNECT_IN_A2DP, (byte)0x25, (byte)0xA2}; //Send buffer

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.connectA2DPFromBLE());
        Mockito.verify(characteristic).setValue(buffer);
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    /**
     * Check that a mailbox command is sent
     * if the required command is valid
     * to disconnect the audio
     */
    @Test
    public void disconnectA2DPFromBLE(){
        byte[] buffer = {MailboxEvents.MBX_DISCONNECT_IN_A2DP, (byte)0x85, (byte)0x11};

        when(gatt.getService(SERVICE)).thenReturn(gattService);
        when(gattService.getUuid()).thenReturn(SERVICE);
        when(gattService.getCharacteristic(CHARACTERISTIC_MAILBOX)).thenReturn(characteristic);

        when(gatt.setCharacteristicNotification(characteristic, false)).thenReturn(true);
        when(characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID)).thenReturn(descriptor);
        when(gatt.writeCharacteristic(characteristic)).thenReturn(true);

        assertTrue(bluetoothLE.disconnectA2DPFromBLE());
        Mockito.verify(characteristic).setValue(buffer);
        Mockito.verify(gatt).writeCharacteristic(characteristic);
    }

    private class MbtBluetoothLEWrapper extends MbtBluetoothLE{
        /**
         * public constructor that will instanciate this class. It also instanciate a new
         * {@link MbtGattController MbtGattController} instance
         *
         * @param context             the application context
         * @param mbtBluetoothManager the Bluetooth manager that performs requests and receives results.
         */
        public MbtBluetoothLEWrapper(@NonNull Context context, MbtBluetoothManager mbtBluetoothManager) {
            super(context, mbtBluetoothManager);
        }

        public boolean isNotificationEnabledOnCharacteristic_test(@NonNull UUID service, @NonNull UUID characteristic){
            return super.isNotificationEnabledOnCharacteristic(service, characteristic);
        }

    }
}

