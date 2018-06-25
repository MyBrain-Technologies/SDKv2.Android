package core.bluetooth;

import android.support.annotation.Keep;

/**
 * Created by Vincent on 02/02/2016.
 */
@Keep
public enum BtState {
    /**
     * When something went wrong but is not necessarily related to Android itself
     */
    INTERNAL_FAILURE,

    /**
     *  Location is required in order to start the LE scan. GPS is disabled
     */
    LOCATION_IS_REQUIRED,

    /**
     *  Location is required in order to start the LE scan. Location may or may not be enabled, the user forgot
     *  to grant permissions to access FINE or COARSE location
     *  <p><strong>Note:</strong> this is needed only in Android M and next.</p>
     */
    LOCATION_PERMISSION_NOT_GRANTED,

    /**
     * Although android ble supports multiple connection, we currently consider that only one connection at a time is possible.
     * Instead of forcing the disconnection of the first device, it is preferable to notify user
     * with error state
     */
    ANOTHER_DEVICE_CONNECTED,

    /**
     *  Failed to start scan as BLE scan with the same settings is already started by the app.
     */
    SCAN_FAILED_ALREADY_STARTED,

    /**
     * Failed to start scan as app cannot be registered.
     */
    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,
    /**
     * Scanning has just started.
     */
    SCAN_STARTED,
    /**
     * Failed to find device after scanning for a defined amount of time.
     */
    SCAN_TIMEOUT,

    /**
     * Failed to retrieve data
     */
    STREAM_ERROR,

    /**
     * Failed to start power optimized scan as this feature is not supported.
     */
    SCAN_FAILED_FEATURE_UNSUPPORTED,

    /**
     * Bluetooth is available on device but not enabled (turned on).
     */
    DISABLED,

    /**
     * Should not occur (see Android Manifest <code>uses-feature android:name="android.hardware.bluetooth_le" android:required="true"</code>.
     * <p>The device does not have a Bluetooth interface or does not support Bluetooth Low Ebergy</p>
     *
     */
    NO_BLUETOOTH,
    /**
     * Failed to connect : remote server not found (not in range or turned off)
     */
    CONNECT_FAILURE,
    /**
     * Idle, not connected, awaiting order
     */
    IDLE,
    /**
     * Currently attempting to connect to a Bluetooth remote endpoint
     */
    CONNECTING,
    /**
     * Successfully connected
     */
    CONNECTED,

    /**
     * Successfully connected and ready to use. This state is used when communication is finally possible,
     * for example, when services are discovered for LE
     */
    CONNECTED_AND_READY,

    /**
     * User request to stop connecting
     */
    INTERRUPTED,

    /**
     * When connection is being disconnected
     */
    DISCONNECTING,


    /**
     * Used to notify user when a device has been found during scanning. The device can be a specific
     * one if the user specified one, or the first device scanned if no device has been specified.
     */
    DEVICE_FOUND,

    /**
     * When connection was lost
     */
    DISCONNECTED
}
