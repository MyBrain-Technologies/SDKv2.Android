package core.bluetooth.lowenergy;


import core.bluetooth.IConnectable;
import core.bluetooth.IScannable;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothLE extends MbtBluetooth implements IScannable, IConnectable, IStreamable {
    private MbtGattController mbtGattController;

    public MbtBluetoothLE(){
        super();
        this.mbtGattController = new MbtGattController();
    }

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
