package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

import android.bluetooth.BluetoothDevice;

/**
 * Interface used to connect to or disconnect from a bluetooth peripheral device
 */
public interface IScannable {

    /**
     * Start a scan in order to find a bluetooth device
     */
    void startScan();

    /**
     * Disconnect from the peripheral device
     * @return true upon success, false otherwise
     */
    void stopScan();

}
