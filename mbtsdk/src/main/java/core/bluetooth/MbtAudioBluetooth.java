package core.bluetooth;


import android.content.Context;

abstract class MbtAudioBluetooth extends MbtBluetooth implements BluetoothInterfaces.IAudioStream {

    MbtAudioBluetooth(Context context, BtProtocol protocol, MbtBluetoothManager mbtBluetoothManager) {
        super(context, protocol, mbtBluetoothManager);
    }

}

