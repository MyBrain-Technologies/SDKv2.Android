package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothSPP extends MbtBluetooth implements IScannable, IConnectable, IStreamable {
    @Override
    public boolean startStream() {
        return false;
    }

    @Override
    public boolean stopStream() {
        return false;
    }

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
