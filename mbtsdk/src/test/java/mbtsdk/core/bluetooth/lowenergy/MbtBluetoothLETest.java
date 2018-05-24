package mbtsdk.core.bluetooth.lowenergy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;

/**
 * Created by Etienne on 08/02/2018.
 */
public class MbtBluetoothLETest {

    @Mock
    Context mContext;

    @Mock
    BluetoothDevice device;

    //MbtGattController mbtGattController;

    @Before
    public void setUp() throws Exception {
        //mbtGattController = new MbtGattController();
    }

    @Test
    public void startStream() throws Exception {

    }

    @Test
    public void stopStream() throws Exception {
    }

    @Test
    public void connect() throws Exception {

    }

    @Test
    public void disconnect() throws Exception {
    }

}