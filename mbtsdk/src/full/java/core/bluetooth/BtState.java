package core.bluetooth;

import android.support.annotation.Keep;

/**
 * Created by Vincent on 02/02/2016.
 */
@Keep
public enum BtState {

    /**
     * Initial state that corresponds to a standby mode : it represents a state where the mobile device is not connected to any headset and is awaiting order from the user or the SDK.
     * For example, he is awaiting for the user to call the connect method.
     * The IDLE state is automatically returned few minutes after the DISCONNECTED state is returned (= after disconnection or lost connection or failure during the connection process).
     */
    IDLE,

    /**
     * Bluetooth is available on device but not enabled (turned on).
     */
    BLUETOOTH_DISABLED,

    /**
     * Should not occur (see Android Manifest <code>uses-feature android:name="android.hardware.bluetooth_le" android:required="true"</code>.
     * <p>The device does not have a Bluetooth interface or does not support Bluetooth Low Ebergy</p>
     *
     */
    NO_BLUETOOTH,

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
     * Although android BLE supports multiple connection, we currently consider that only one connection at a time is possible.
     * Instead of forcing the disconnection of the first device, it is preferable to notify user
     * with error state so that the user can choose if he wants to disconnect the already connected device or not.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs and the SDK will returned to a CONNECTED_AND_READY state.
     */
    ANOTHER_DEVICE_CONNECTED,

    /**
     *  Failed to start scan as BLE scan with the same settings is already started by the app. This state is a android.bluetooth.le.ScanCallback state reported if the scan failed.
     */
    SCAN_FAILED_ALREADY_STARTED,

    /**
     * Failed to start scanning operation
     */
    SCAN_FAILED,

    /**
     * In case all the connection prerequisites are valid, a scanning has just started to look for an available headset using the LE scan discovery.
     * The Low Energy Scanner is used first, as it is more efficient than the classical Bluetooth discovery Scanner.
     * A specific headset can be targeted if the user specify a headset name when he call the connect method.
     */
    SCAN_STARTED,

    /**
     * Failed to find device during a scanning within a defined allocated amount of time.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs and the device will returned to an "IDLE" state.
     */
    SCAN_TIMEOUT,

    /**
     * When the user requests to cancel (stop) the scanning process that is in progress.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs and the device will returned to an "IDLE" state.
     */
    SCAN_INTERRUPTED,

    /**
     * Failed to retrieve data. Bluetooth SPP only
     */
    STREAM_ERROR,


    /**
     * Failed to establish bluetooth connection with the device : this can be a BLE connection or an A2DP connection
     */
    CONNECTION_FAILURE,

    /**
     * Currently attempting to connect to a Bluetooth remote endpoint
     */
    CONNECTING,

    /**
     * Successfully connected
     */
    CONNECTED,

    /**
     *  Retrieving the services and characteristics (list of data where a value is associated to a name) that the connected headset deliver.
     *  This operation is included in the connection process to ensure that a communication has well been established between the headset and the mobile device.
     *  (If the communication has not been established, the data sent by the headset cannot be retrieved by the SDK, so we consider that the connection is not valid)
     */
    DISCOVERING_SERVICES,

    /**
     * Failed to retrieve the services and characteristics of the connected headset.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs.
     * This failure trigger a disconnection.
     */
    DISCOVERING_FAILURE,

    /**
     * Getting the headset device informations such as the Serial number, the Firmware or the Hardware version by reading the values returned by the characteristics discovery.
     * This operation is included in the connection process to ensure that the received characteristics can be read (and contains values / are not empty ?) by the SDK.
     * (If the communication is established, but no data (empty data ?) is sent by the headset, we consider that the connection is not valid)
     */
     READING_DEVICE_INFO,

    /**
     * Failed to get at least one of the device informations (Serial number, Firmware version, Hardware version).
     * The connection process that was running is automatically cancelled (stopped) if this state occurs.
     * This failure trigger a disconnection.
     */
    READING_FAILURE,

    /**
     * Exchanging and storing of the long term keys for the next times a connection is initiated.
     * This operation is included in the connection process only for headsets whose firmware version are higher than or equal to 1.7.0.
     * Headsets whose firmware version are lower than 1.7.0 can not handle this operation so the bonding step is just skipped.
     * We consider that the headset is connected and ready to acquire data after the Device Info reading operation has returned values (= has not failed).
     */
    BONDING,

    /**
     * Failed to exchange and/or store the long term keys.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs.
     * This failure trigger a disconnection.
     */
    BONDING_FAILURE,

    /**
     * Successfully connected and ready to use. This state is used when communication is finally possible,
     * for example, when services are discovered for LE
     */
    CONNECTED_AND_READY,

    /**
     * When the user requests to cancel (stop) the connection process that is in progress.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs. This failure trigger a disconnection.
     */
    CONNECTION_INTERRUPTED,

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
    DISCONNECTED,

    /**
     * When audio only is connected
     */
    AUDIO_CONNECTED,
    /**
     * When audio only is connected
     */
    AUDIO_DISCONNECTED,

    /**
     * Replacing the current firmware installed by installing a different version of the firmware (should be the last firmware version, but it can also be an downgrading to an old version).
     * This operation requires a connected headset to be performed. Once the upgrade is done, a disconnection is performed to reboot the system.
     */
    UPGRADING,

    /**
     * Failed to replace the current firmware installed with a new one. This failure trigger a disconnection.
     */
    UPGRADE_FAILED



}
