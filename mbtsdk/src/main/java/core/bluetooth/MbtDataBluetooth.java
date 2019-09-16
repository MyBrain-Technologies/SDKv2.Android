package core.bluetooth;


import android.content.Context;

public abstract class MbtDataBluetooth extends MbtBluetooth implements BluetoothInterfaces.IDataBluetooth {

    public MbtDataBluetooth(Context context, BtProtocol protocol, MbtBluetoothManager mbtBluetoothManager) {
        super(context, protocol, mbtBluetoothManager);
    }
}

