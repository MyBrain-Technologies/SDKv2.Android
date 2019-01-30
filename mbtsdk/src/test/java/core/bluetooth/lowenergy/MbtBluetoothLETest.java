package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.UUID;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.spp.MbtBluetoothSPP;
import features.ScannableDevices;

import static org.junit.Assert.*;

public class MbtBluetoothLETest {

    private Context context;
    MbtBluetoothLE bluetoothLE;
    MbtBluetoothManager bluetoothManager;
    MbtManager mbtManager;

    final UUID SERVICE = MelomindCharacteristics.SERVICE_MEASUREMENT;
    final UUID CHARACTERISTIC_MAILBOX = MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX;
    final UUID CHARACTERISTIC_EEG = MelomindCharacteristics.CHARAC_MEASUREMENT_EEG;
    final UUID UNKNOWN = UUID.fromString("0-0-0-0-0");
    final UUID DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    final String DEVICE_NAME = "melo_01010101";

    @Before
    public void setUp() {

        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        context = Mockito.mock(Context.class);
        mbtManager = new MbtManager(context);
        bluetoothManager = new MbtBluetoothManager(context, mbtManager);
        bluetoothLE = new MbtBluetoothLE(context, bluetoothManager);
    }
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
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
        assertFalse(bluetoothLE.startStream());
    }/**
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
     * Assert that streaming cannot be started if headset is stopped
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
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
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
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
        assertFalse(bluetoothLE.activateDeviceStatusMonitoring());
    }

    /**
     * Check that stopStream do nothing if streaming is not in progress cause it has failed to start
     */
    @Test
    public void stopStream_Failed() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.FAILED);
        assertTrue(bluetoothLE.stopStream());
    }

    /**
     * Check that stopStream do nothing if streaming is not in progress cause it was already stopped
     */
    @Test
    public void stopStream_Stopped() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
        assertTrue(bluetoothLE.stopStream());
    }
    /**
     * Check that stopStream do nothing if streaming is not in progress
     */
    @Test
    public void stopStream_Idle() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.IDLE);
        assertTrue(bluetoothLE.stopStream());
    }
    /**
     * Check that stopStream fail if streaming is in progress but characteristic is null
     */
    @Test
    public void stopStream_started() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertFalse(bluetoothLE.stopStream());
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
        MbtConfig.scannableDevices = ScannableDevices.VPRO;
        MbtBluetoothSPP bluetoothSPP = new MbtBluetoothSPP(context, bluetoothManager);
        bluetoothSPP.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertFalse(bluetoothLE.isStreaming());
    }

    @Test
    public void notifyNewHeadsetStatus() {
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
        MbtConfig.scannableDevices = ScannableDevices.VPRO;
        MbtBluetoothSPP bluetoothSPP = new MbtBluetoothSPP(context, bluetoothManager);
        bluetoothSPP.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        assertFalse(bluetoothLE.isConnected());
    }

    /**
     * Assert that notifications can not be enabled if headset is not connected
     */
    @Test
    public void enableOrDisableNotificationsOnCharacteristic_Disconnected() {
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
        assertFalse(bluetoothLE.enableOrDisableNotificationsOnCharacteristic(true, characteristic));
    }

    /**
     * Check that the stopLowEnergyScan method failed if the user is looking for a Vpro
     */
    @Test
    public void startLowEnergyScan_Vpro() {
        MbtConfig.scannableDevices = ScannableDevices.VPRO;
        assertNull(bluetoothLE.startLowEnergyScan(true,DEVICE_NAME));
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
    }

    @Test
    public void startLowEnergyScan_FailedNullNameFilterOn() {
        assertNull(bluetoothLE.startLowEnergyScan(true,null));
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
    }

    @Test
    public void startLowEnergyScan_FailedNotNullNameFilterOn() {
        assertNull(bluetoothLE.startLowEnergyScan(true,DEVICE_NAME));
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
    }
    @Test
    public void startLowEnergyScan_FailedNullNameFilterOff() {
        assertNull(bluetoothLE.startLowEnergyScan(false,null));
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
    }

    @Test
    public void startLowEnergyScan_FailedNotNullNameFilterOff() {
        assertNull(bluetoothLE.startLowEnergyScan(false,DEVICE_NAME));
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
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
        bluetoothLE.startLowEnergyScan(true, DEVICE_NAME);
        bluetoothLE.stopLowEnergyScan();
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
    }
    /**
     * Check that the stopLowEnergyScan method failed if the user is looking for a Vpro
     */
    @Test
    public void stopLowEnergyScan_Vpro() {
        MbtConfig.scannableDevices = ScannableDevices.VPRO;
        bluetoothLE.stopLowEnergyScan();
        assertEquals(bluetoothLE.getCurrentState(), BtState.SCAN_FAILED);
    }

    @Test
    public void connect() {
    }

    @Test
    public void disconnect() {
    }

    @Test
    public void isConnected_ConnectedAndReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY,true);
        assertTrue(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Connected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Disconnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ScanFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_FAILED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_AudioConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.AUDIO_CONNECTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ConnectionFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTION_FAILURE,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ConnectionInterrupted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTION_INTERRUPTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_InternalFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_BluetoothDisabled() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BLUETOOTH_DISABLED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ReadingDeviceInfo() {
        bluetoothLE.notifyConnectionStateChanged(BtState.READING_DEVICE_INFO,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ReadingFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.READING_FAILURE,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_AudioDisconnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.AUDIO_DISCONNECTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ScanTimeout() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_TIMEOUT,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Bonding() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BONDING,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_AnotherDeviceConnected() {
        bluetoothLE.notifyConnectionStateChanged(BtState.ANOTHER_DEVICE_CONNECTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_BondingFailure() {
        bluetoothLE.notifyConnectionStateChanged(BtState.BONDING_FAILURE,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Connecting() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTING,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_DeviceFound() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DEVICE_FOUND,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Disconnecting() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTING,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_DiscoveringFailed() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Discovering() {
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_SERVICES,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Idle() {
        bluetoothLE.notifyConnectionStateChanged(BtState.IDLE,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_LocationIsRequired() {
        bluetoothLE.notifyConnectionStateChanged(BtState.LOCATION_IS_REQUIRED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_LocationPermission() {
        bluetoothLE.notifyConnectionStateChanged(BtState.LOCATION_PERMISSION_NOT_GRANTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_BluetoothNotSupported() {
        bluetoothLE.notifyConnectionStateChanged(BtState.NO_BLUETOOTH,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ScanFailedAlreadyStarted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_FAILED_ALREADY_STARTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ScanInterrupted() {
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_INTERRUPTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_ScanStarted(){
        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_STARTED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_UpgradingFailed(){
        bluetoothLE.notifyConnectionStateChanged(BtState.UPGRADE_FAILED,true);
        assertFalse(bluetoothLE.isConnected());
    }
    @Test
    public void isConnected_Upgrading(){
        bluetoothLE.notifyConnectionStateChanged(BtState.UPGRADING,true);
        assertFalse(bluetoothLE.isConnected());
    }

    @Test
    public void startReadOperation_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
        assertFalse(bluetoothLE.startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void startReadOperation_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void startReadOperation_GattCharacteristicNull() {
        assertFalse(bluetoothLE.startReadOperation(UNKNOWN));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }

    @Test
    public void startWriteOperation_GattNull() {
        bluetoothLE.gatt = null;
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};
        assertFalse(bluetoothLE.startWriteOperation(CHARACTERISTIC_MAILBOX,code));
    }
    @Test
    public void startWriteOperation_GattCharacteristicNull() {
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};
        assertFalse(bluetoothLE.startWriteOperation(UNKNOWN,code));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }

    @Test
    public void checkServiceAndCharacteristicValidity_NullGatt() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE,CHARACTERISTIC_EEG));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void checkServiceAndCharacteristicValidity_NullGattService() {
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(UNKNOWN,CHARACTERISTIC_EEG));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);

    }
    @Test
    public void checkServiceAndCharacteristicValidity_NullGattCharacteristic() {
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(SERVICE,CHARACTERISTIC_EEG));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);

    }
    @Test
    public void checkServiceAndCharacteristicValidity_InvertedServiceAndCharacteristic() {
        assertFalse(bluetoothLE.checkServiceAndCharacteristicValidity(CHARACTERISTIC_EEG,SERVICE));
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
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
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
        assertFalse(bluetoothLE.readBattery());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readBattery_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readBattery());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readBattery_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readBattery());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }

    @Test
    public void readFwVersion_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
        assertFalse(bluetoothLE.readFwVersion());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readFwVersion_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readFwVersion());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readFwVersion_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readFwVersion());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }

    @Test
    public void readHwVersion_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
        assertFalse(bluetoothLE.readHwVersion());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readHwVersion_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readHwVersion());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readHwVersion_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readHwVersion());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }


    @Test
    public void readSerialNumber_ConnectedNotReady() {
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
        assertFalse(bluetoothLE.readSerialNumber());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readSerialNumber_GattNull() {
        bluetoothLE.gatt = null;
        assertFalse(bluetoothLE.readSerialNumber());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }
    @Test
    public void readSerialNumber_GattCharacteristicNull() {
        assertFalse(bluetoothLE.readSerialNumber());
        assertEquals(bluetoothLE.getCurrentState(),BtState.READING_FAILURE);
    }

    @Test
    public void onNotificationStateChanged_started() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG,0,0);
        bluetoothLE.onNotificationStateChanged(true, characteristic, true);
        //assertTrue(bluetoothLE.isStreaming()); //todo check why characteristic.getuuid is null
    }
    @Test
    public void onNotificationStateChanged_stopped() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG,0,0);
        bluetoothLE.onNotificationStateChanged(true, characteristic, false);
        assertFalse(bluetoothLE.isStreaming());
    }
    @Test
    public void onNotificationStateChanged_failed() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG,0,0);
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
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
    }
    /**
     * Assert that streaming state do not change if streaming is in progress and the new state is not equal to DISCONNECTED
     */
    @Test
    public void notifyConnectionStateChanged_ConnectionWhileStreaming() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STARTED);
        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED, true);
        assertTrue(bluetoothLE.isStreaming());
    }
    /**
     * Assert that streaming state do not change if streaming is not progress and the new state is to DISCONNECTED
     */
    @Test
    public void notifyConnectionStateChanged_DisconnectionDuringStoppedStream() {
        bluetoothLE.notifyStreamStateChanged(IStreamable.StreamState.STOPPED);
        bluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
        assertFalse(bluetoothLE.isStreaming());
    }

    @Test
    public void changeMTU_Disconnected() {
        assertFalse(bluetoothLE.disconnect(-1));
    }

    @Test
    public void changeFilterConfiguration() {
    }

    @Test
    public void changeAmpGainConfiguration() {
    }

    @Test
    public void switchP300Mode() {
    }

    @Test
    public void requestDeviceConfig() {
    }

    @Test
    public void disconnectHeadsetAlreadyConnected() {
    }

    @Test
    public void getGattDeviceName() {
    }

    @Test
    public void connectA2DPFromBLE() {
    }

    @Test
    public void disconnectA2DPFromBLE() {
    }

    @Test
    public void requestBonding() {
    }

    @Test
    public void notifyDeviceIsBonded() {
    }

    @Test
    public void notifyMailboxEventReceived() {
    }

    @Test
    public void startScanDiscovery() {
    }

    @Test
    public void stopScanDiscovery() {
    }

    @Test
    public void notifyDeviceInfoReceived() {
    }

    @Test
    public void notifyOADEvent() {
    }

    @Test
    public void notifyConnectionStateChanged1() {
    }

    @Test
    public void notifyMailboxEvent() {
    }

    @Test
    public void notifyBatteryReceived() {
    }

    @Test
    public void notifyHeadsetStatusEvent() {
    }

    @Test
    public void notifyNewDataAcquired() {
    }

    @Test
    public void enableBluetoothOnDevice() {
    }
}