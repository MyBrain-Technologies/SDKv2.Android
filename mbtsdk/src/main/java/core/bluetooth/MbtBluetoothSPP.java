package core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothSPP extends MbtBluetooth implements IStreamable {

    public MbtBluetoothSPP(Context context) {
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

    @Override
    public boolean startStream() {
        return false;
    }

    @Override
    public boolean stopStream() {
        return false;
    }
}

