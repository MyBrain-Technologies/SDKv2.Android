package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.utils.ArrayUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import command.DeviceCommand;
import core.bluetooth.MbtBluetoothManager;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.eq;

public class MbtGattControllerTest {

    private final byte[] RESPONSE = new byte[]{0,1,2,3,4,5};
    private final byte[] IN_PROGRESS_RESPONSE = new byte[]{1,1,2,3,4,5};

    private MbtGattController gattController;
    private MbtBluetoothLE mbtBluetoothLE;

    @Before
    public void setUp() throws Exception {
        Context context = Mockito.mock(Context.class);
        mbtBluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        gattController = new MbtGattController(context,mbtBluetoothLE);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( serial number command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_SerialNumber(){
        byte code = MailboxEvents.MBX_SET_SERIAL_NUMBER;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( product name command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_ProductName(){
        byte code = MailboxEvents.MBX_SET_PRODUCT_NAME;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( system status command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_SystemStatus(){
        byte code = MailboxEvents.MBX_SYS_GET_STATUS;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( reboot command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_SystemReboot(){
        byte code = MailboxEvents.MBX_SYS_REBOOT_EVT;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( notch filter command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_NotchFilter(){
        byte code = MailboxEvents.MBX_SET_NOTCH_FILT;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * (amplifier gain command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_AmpGain(){
        byte code = MailboxEvents.MBX_SET_AMP_GAIN;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( EEG config command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_EegConfig(){
        byte code = MailboxEvents.MBX_GET_EEG_CONFIG;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( triggers command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_Triggers(){
        byte code = MailboxEvents.MBX_P300_ENABLE;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( DC offset command ) has changed
     */
    @Test
    public void notifyMailboxEventReceived_DC_offset(){
        byte code = MailboxEvents.MBX_DC_OFFSET_ENABLE;
        notifyMailboxEventReceived(code);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * does not notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( connect command with a "in progress" response from the headset )
     * and when the Bluetooth audio connection has changed
     */
    @Test
    public void notifyMailboxEventReceived_ConnectAudio_inProgress(){
        byte code = MailboxEvents.MBX_CONNECT_IN_A2DP;
        ArgumentCaptor<DeviceCommand> captor = ArgumentCaptor.forClass(DeviceCommand.class);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        Mockito.when(characteristic.getValue()).thenReturn(getCharacteristic(code));
        Mockito.when(characteristic.getUuid()).thenReturn(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX);

        gattController.onCharacteristicChanged(gatt, characteristic);

        Mockito.verify(mbtBluetoothLE, Mockito.never()).notifyCommandResponseReceived(Mockito.eq(IN_PROGRESS_RESPONSE), captor.capture());
        Mockito.verify(mbtBluetoothLE).stopWaitingOperation();
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( connect command with a response that is not "in progress"  )
     * and when the Bluetooth audio connection has changed
     */
    @Test
    public void notifyMailboxEventReceived_ConnectAudio_notInProgress(){
        byte code = MailboxEvents.MBX_CONNECT_IN_A2DP;
        notifyMailboxEventReceived(code);
        notifyConnectionResponseReceived(code, RESPONSE);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox
     * ( disconnect command ) and when the Bluetooth audio connection has changed
     */
    @Test
    public void notifyMailboxEventReceived_DisconnectAudio(){
        byte code = MailboxEvents.MBX_DISCONNECT_IN_A2DP;
        notifyMailboxEventReceived(code);
        notifyConnectionResponseReceived(code, RESPONSE);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the Bluetooth connection has changed
     */
    private void notifyConnectionResponseReceived(byte code, byte[] response){
        Mockito.verify(mbtBluetoothLE).notifyConnectionResponseReceived(code, response[0]);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox has changed
     */
    private void notifyMailboxEventReceived(byte code){
        ArgumentCaptor<DeviceCommand> captor = ArgumentCaptor.forClass(DeviceCommand.class);
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
        byte[] characteristicValue = getCharacteristic(code);
        Mockito.when(characteristic.getValue()).thenReturn(characteristicValue);
        Mockito.when(characteristic.getUuid()).thenReturn(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX);

        gattController.onCharacteristicChanged(gatt, characteristic);

        Mockito.verify(mbtBluetoothLE).notifyCommandResponseReceived(Mockito.eq(RESPONSE), captor.capture());
        Mockito.verify(mbtBluetoothLE).stopWaitingOperation();
        assertEquals("RESPONSE: " + Arrays.toString(RESPONSE) + " CHARACTERISTIC: " + Arrays.toString(Arrays.copyOfRange(characteristicValue, 1, characteristicValue.length)), Arrays.toString(RESPONSE), Arrays.toString(Arrays.copyOfRange(characteristicValue, 1, characteristicValue.length)));
    }

    private byte[] getCharacteristic(byte mailboxEventsCode){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write( mailboxEventsCode );
        try {
            outputStream.write( RESPONSE );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }
}