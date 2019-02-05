package core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class FutureTaskExtended extends FutureTask {

    FutureTaskExtended(@NonNull Callable callable) {
        super(callable);
    }

    public void setDevice(BluetoothDevice device){
        this.set(device);
    }
}
