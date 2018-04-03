package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothA2DP extends MbtBluetooth implements IScannable, IConnectable {
    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public boolean disconnect() {
        return false;
    }

    @Override
    public void startScan() {

    }

    @Override
    public void stopScan() {

    }
}
