package core.bluetooth;

import androidx.annotation.Keep;

/**
 * The differents stream states
 */
@Keep
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

    STREAMING,

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
