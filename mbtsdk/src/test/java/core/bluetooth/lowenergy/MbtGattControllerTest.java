package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;


import utils.CommandUtils;

import static java.util.UUID.fromString;


@RunWith(PowerMockRunner.class)
@PrepareForTest(MbtGattController.class)
public class MbtGattControllerTest {

    private final byte[] MAILBOX_RAW_RESPONSE_5 = new byte[]{5,10,1,2,3,4,5};
    private final byte[] MAILBOX_RAW_RESPONSE_0 = new byte[]{0,10,1,2,3,4,5};
    private final byte[] MAILBOX_RESPONSE = new byte[]{10,1,2,3,4,5};
    private final byte[] IN_PROGRESS_RESPONSE = new byte[]{1,1,2,3,4,5};

    private final UUID SERVICE = MelomindCharacteristics.SERVICE_MEASUREMENT;
    private final UUID CHARACTERISTIC_MAILBOX = MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX;
    private final UUID CHARACTERISTIC_EEG = MelomindCharacteristics.CHARAC_MEASUREMENT_EEG;
    private final UUID CHARACTERISTIC_STATUS = MelomindCharacteristics.CHARAC_HEADSET_STATUS;
    private final UUID UNKNOWN = fromString("0-0-0-0-0");

    private MbtGattController gattController;
    private MbtGattController spyGattController;
    private MbtBluetoothLE mbtBluetoothLE;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic characteristic;


    @Before
    public void setUp() throws Exception {
        Context context = Mockito.mock(Context.class);
        mbtBluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        gattController = new MbtGattController(context,mbtBluetoothLE);
        spyGattController = PowerMockito.spy(gattController);
        gatt = Mockito.mock(BluetoothGatt.class);
        characteristic = Mockito.mock(BluetoothGattCharacteristic.class);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the mailbox has changed
     */
    @Test
    public void onCharacteristicChanged_MailboxCommandReceived(){
        Mockito.when(characteristic.getUuid()).thenReturn(CHARACTERISTIC_MAILBOX);
        Mockito.when(characteristic.getValue()).thenReturn(MAILBOX_RAW_RESPONSE_5);
        PowerMockito.spy(CommandUtils.class);

        gattController.onCharacteristicChanged(gatt, characteristic);

        Mockito.verify(mbtBluetoothLE).stopWaitingOperation(MAILBOX_RESPONSE);
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the EEG has changed
     */
    @Test
    public void onCharacteristicChanged_EegReceived(){
        Mockito.when(characteristic.getUuid()).thenReturn(CHARACTERISTIC_EEG);

        gattController.onCharacteristicChanged(gatt, characteristic);

        Mockito.verify(mbtBluetoothLE).notifyNewDataAcquired(characteristic.getValue());
    }

    /**
     * Check that the Bluetooth Gatt controller
     * notifies the Bluetooth LE class
     * when a characteristic related to the EEG has changed
     */
    @Test
    public void onCharacteristicChanged_StatusReceived(){
        Mockito.when(characteristic.getUuid()).thenReturn(CHARACTERISTIC_STATUS);

        gattController.onCharacteristicChanged(gatt, characteristic);

        Mockito.verify(mbtBluetoothLE).notifyNewHeadsetStatus(characteristic.getValue());
    }

    /**
     * Check that the Bluetooth Gatt controller
     * do not notify the Bluetooth LE class
     * when a unknown characteristic has changed
     */
    @Test
    public void onCharacteristicChanged_UnknownCharacteristicReceived(){
        Mockito.when(characteristic.getUuid()).thenReturn(UNKNOWN);

        gattController.onCharacteristicChanged(gatt, characteristic);

        Mockito.verifyZeroInteractions(mbtBluetoothLE);
    }

}