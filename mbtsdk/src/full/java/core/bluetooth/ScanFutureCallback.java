package core.bluetooth;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.Callable;

public abstract class ScanFutureCallback extends ScanCallback {

    private FutureTaskExtended futureDevice;

    protected ScanFutureCallback(Callable callable) {
        this.futureDevice = new FutureTaskExtended(callable);
    }

    @Override
    public void onScanResult(int callbackType, @NonNull ScanResult result){
        super.onScanResult(callbackType, result);
        this.futureDevice.setDevice(result.getDevice());
        Log.i("ScanFutureCallback", "future device set "+result.getDevice());
    }

    public FutureTaskExtended getFutureDevice() {
        return futureDevice;
    }
}
