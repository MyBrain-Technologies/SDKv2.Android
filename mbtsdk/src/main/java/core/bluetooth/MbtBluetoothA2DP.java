package core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothA2DP extends MbtBluetooth{


    public MbtBluetoothA2DP(Context context) {
        super(context);
    }

    @Override
    public boolean connect(Context context, BluetoothDevice device) {
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        return false;
    }
}
