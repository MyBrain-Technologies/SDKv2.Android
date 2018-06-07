package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Interface used to connect to or disconnect from a bluetooth peripheral device
 */
public interface IConnectable {

    /**
     * ConnectRequestEvent to the peripheral device
     * @return true upon success, false otherwise
     */
    boolean connect(Context context, BluetoothDevice device);

    /**
     * Disconnect from the peripheral device
     * @return true upon success, false otherwise
     */
    boolean disconnect(BluetoothDevice device);


    /**
     * Method used to notify that the connection state has changed
     * @param newState The new bluetooth connection state. Refer to @{@link BtState}
     * for the complete list of states.
     */
    void notifyStateChanged(@NonNull final BtState newState);


    boolean isConnected();

}
