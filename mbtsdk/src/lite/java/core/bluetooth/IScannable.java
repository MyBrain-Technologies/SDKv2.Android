package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

import android.bluetooth.BluetoothDevice;

/**
 * Interface used to connect to or disconnect from a bluetooth peripheral device
 */
interface IScannable {

    /**
     * Start a classic discovery scan in order to find a bluetooth device
     * @param
     */
    BluetoothDevice startScanDiscovery(String deviceName);

    /**
     * Disconnect from the peripheral device
     */
    void stopScanDiscovery();

}
