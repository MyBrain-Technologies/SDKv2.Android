package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

import android.content.Context;

/**
 * Interface used to connect to or disconnect from a bluetooth peripheral device
 */
public interface IScannable {

    /**
     * Start a classic discovery scan in order to find a bluetooth device
     * @param context: The context in which the scan will be started.
     */
    void startScanDiscovery(Context context, String deviceName);

    /**
     * Disconnect from the peripheral device
     * @return true upon success, false otherwise
     */
    void stopScanDiscovery();

}
