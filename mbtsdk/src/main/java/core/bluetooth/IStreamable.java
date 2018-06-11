package core.bluetooth;

/**
 * Created by Etienne on 08/02/2018.
 *
 * Interface used to start or stop streaming operations from a bluetooth peripheral device.
 * Streaming means here acquisition in real time of data from the peripheral, using supported bluetooth mechanisms.
 */
public interface IStreamable {

    /**
     * Start a stream operation
     * @return
     */
    boolean startStream();

    /**
     * Stop a stream operation
     * @return
     */
    boolean stopStream();

    interface DataStreamListener{
        void onNewData();
    }

    /**
     * Called whenever the stream state has changed.
     * @param streamState the new StreamState
     */
    void notifyStreamStateChanged(StreamState streamState);

    /**
     * The differents stream states
     */
    enum StreamState{
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
        FAILED
    }

    /**
     *
     * @return true is streaming is in progress, false otherwise.
     */
    boolean isStreaming();
}
