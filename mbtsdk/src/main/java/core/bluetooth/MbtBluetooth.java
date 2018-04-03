package core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utils.MbtLock;

/**
 * Created by Etienne on 08/02/2018.
 */

public abstract class MbtBluetooth implements IScannable, IConnectable{

    protected BluetoothAdapter bluetoothAdapter;

    private BtState currentState = BtState.DISCONNECTED;

    protected final MbtLock<BluetoothDevice> scanLock = new MbtLock<>();
    protected List<BluetoothDevice> scannedDevices = new ArrayList<>();

    protected final MbtLock<BtState> connectionLock = new MbtLock<>();


    public MbtBluetooth(Context context) {
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            this.bluetoothAdapter = manager.getAdapter();
        }
        if(this.bluetoothAdapter == null){
            this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //try another way to get the adapter
        }
    }

    @Override
    public void startScanDiscovery(Context context) {

    }

    @Override
    public void stopScanDiscovery() {

    }

    @Override
    public void notifyStateChanged(@NonNull BtState newState) {
        this.currentState = newState;
//        if (this.stateListener != null)
//            this.stateListener.onStateChanged(newState);
    }
}
