package core.bluetooth;
/**
 * The differents stream states
 */
public enum StreamState{
    /**
     * No stream is in progress and no start and stop operation has been launched. This is the
     * initial state.
     */
    IDLE,

    /**
     * Stream has correctly been started
     */
    STARTED,

    /**
     * Steam has correctly been stopped
     */
    STOPPED,

    /**
     * Stream has not correctly been started or stopped.
     */
    FAILED,

    /**
     * Device is disconnected, thus stream cannot be started
     */
    DISCONNECTED

}
