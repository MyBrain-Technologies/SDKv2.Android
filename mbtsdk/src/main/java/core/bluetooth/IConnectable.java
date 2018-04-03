package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 */

/**
 * Interface used to connect to or disconnect from a bluetooth peripheral device
 */
public interface IConnectable {

    /**
     * Connect to the peripheral device
     * @return true upon success, false otherwise
     */
    boolean connect();

    /**
     * Disconnect from the peripheral device
     * @return true upon success, false otherwise
     */
    boolean disconnect();

}
