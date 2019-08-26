package core.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Pair;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import command.DeviceCommandEvent;
import core.bluetooth.BtState;
import core.device.event.OADEvent;
import core.device.model.DeviceInfo;
import core.device.model.FirmwareVersion;
import core.device.oad.OADManager;
import eventbus.events.BluetoothResponseEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import features.MbtDeviceType;

import static org.junit.Assert.*;

public class MbtDeviceManagerTest {

    MbtDeviceManager deviceManager;

    @Before
    public void setUp() throws Exception {
        deviceManager = new MbtDeviceManager(Mockito.mock(Context.class));
    }

    @Test
    public void onConnectionStateChanged_scanTimeout() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.SCAN_TIMEOUT));
        assertNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onConnectionStateChanged_scanFailure() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.SCAN_FAILURE));
        assertNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onConnectionStateChanged_scanInterrupted() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.SCAN_INTERRUPTED));
        assertNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onConnectionStateChanged_disconnected() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.DATA_BT_DISCONNECTED));
        assertNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onConnectionStateChanged_connectionFailure() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.CONNECTION_FAILURE));
        assertNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onConnectionStateChanged_connectionInterrupted() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.CONNECTION_INTERRUPTED));
        assertNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onConnectionStateChanged_deviceFound() {
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.DEVICE_FOUND, Mockito.mock(BluetoothDevice.class), MbtDeviceType.MELOMIND));
        assertNotNull(deviceManager.getmCurrentConnectedDevice());
    }

    @Test
    public void onOADEvent_initValid() {
        assertNull(deviceManager.getOadManager());

        deviceManager.onStartOADUpdate(new DeviceEvents.StartOADUpdate(new FirmwareVersion("1.7.1")));

        //assertNotNull(deviceManager.getOadManager());
    }

    @Test
    public void onOADEvent_nonInit() {
        deviceManager.setOadManager(null);
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.DATA_BT_DISCONNECTED));
        deviceManager.onStartOADUpdate(new DeviceEvents.StartOADUpdate(new FirmwareVersion("1.7.1")));

        assertNull(deviceManager.getOadManager());

    }

    @Test
    public void getEventFromMailboxCommand_unknown() {
        assertNull(OADEvent.getEventFromMailboxCommand(DeviceCommandEvent.MBX_LEAD_OFF_EVT));
    }
    @Test
    public void getEventFromMailboxCommand_lostPacket() {
        assertEquals(OADEvent.LOST_PACKET, OADEvent.getEventFromMailboxCommand(DeviceCommandEvent.MBX_OTA_IDX_RESET_EVT));
    }
    @Test
    public void getEventFromMailboxCommand_readback() {
        assertEquals(OADEvent.CRC_READBACK, OADEvent.getEventFromMailboxCommand(DeviceCommandEvent.MBX_OTA_STATUS_EVT));
    }
    @Test
    public void getEventFromMailboxCommand_validation() {
        assertEquals(OADEvent.FIRMWARE_VALIDATION_RESPONSE, OADEvent.getEventFromMailboxCommand(DeviceCommandEvent.MBX_OTA_MODE_EVT));
    }
    @Test
    public void getEventFromMailboxCommand_transferred() {
        assertEquals(OADEvent.PACKET_TRANSFERRED, OADEvent.getEventFromMailboxCommand(DeviceCommandEvent.OTA_STATUS_TRANSFER));
    }

    @Test
    public void onBluetoothEventReceived_lostPacket() {
        ArgumentCaptor<OADEvent> captorEvent = ArgumentCaptor.forClass(OADEvent.class);
        OADManager oadManager = Mockito.mock(OADManager.class);
        deviceManager.setOadManager(oadManager);
        deviceManager.onBluetoothEventReceived(new BluetoothResponseEvent(OADEvent.LOST_PACKET.getMailboxEvent(), 1));

        Mockito.verify(oadManager).onOADEvent(captorEvent.capture());
    }

    @Test
    public void stopOADUpdate() {

        deviceManager.stopOADUpdate();

        assertNull(deviceManager.getOadManager());
    }

    @Test
    public void compareFirmwareVersion_success() {
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        Mockito.when(bluetoothDevice.getAddress()).thenReturn("address");
        Mockito.when(bluetoothDevice.getName()).thenReturn("name");
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.DEVICE_FOUND, bluetoothDevice, MbtDeviceType.MELOMIND));
        deviceManager.onDeviceInfoEvent(new DeviceInfoEvent(DeviceInfo.FW_VERSION, "1.7.1" ));
        assertTrue(deviceManager.verifyFirmwareVersion(new FirmwareVersion("1.7.1")));
    }

    @Test
    public void compareFirmwareVersion_failure() {
        BluetoothDevice bluetoothDevice = Mockito.mock(BluetoothDevice.class);
        Mockito.when(bluetoothDevice.getAddress()).thenReturn("address");
        Mockito.when(bluetoothDevice.getName()).thenReturn("name");
        deviceManager.onDeviceInfoEvent(new DeviceInfoEvent(DeviceInfo.FW_VERSION, "1.7.2" ));
        deviceManager.onConnectionStateChanged(new ConnectionStateEvent(BtState.DEVICE_FOUND, bluetoothDevice, MbtDeviceType.MELOMIND));
        assertFalse(deviceManager.verifyFirmwareVersion(new FirmwareVersion("1.7.1")));
    }
}