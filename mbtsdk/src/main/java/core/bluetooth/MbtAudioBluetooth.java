package core.bluetooth;


import android.content.Context;

abstract class MbtAudioBluetooth extends MbtBluetooth implements BluetoothInterfaces.IAudioBluetooth {

    MbtAudioBluetooth(Context context, BluetoothProtocol protocol, MbtBluetoothManager mbtBluetoothManager) {
        super(context, protocol, mbtBluetoothManager);
    }

}

