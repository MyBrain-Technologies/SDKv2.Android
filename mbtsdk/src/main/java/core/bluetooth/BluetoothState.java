package core.bluetooth;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import engine.clientevents.BaseError;
import engine.clientevents.BasicError;
import engine.clientevents.BluetoothError;
import engine.clientevents.EegError;
import engine.clientevents.OADError;
import engine.clientevents.MobileDeviceError;

/**
 * The Bluetooth current state describes the state of connection between a headset device and a mobile device.
 * As the SDK needs to perform specific operations according to the value of the current connection state, the BtState enum provides all the possible states to the Bluetooth unit Manager.
 * All the states are classified in a chronological order in this Enum, except those that we consider as a "failure state".
 * These failure states are assigned if something went wrong during the Bluetooth connection process,
 * In this case, the operation is stopped and the headset is disconnected.
 * The failure that happened is always transmitted to the SDK via the current state variable contained in the MbtBluetoothLE or MbtBluetoothSPP class.
 * During a connection, we consider that the current state is a failure state if CONNECTED_AND_READY state state has not be reached.
 * The SDK user is notified for every failure that stop a started connection process through the onError method.
 * This method returns the failure in a BaseError type object that describe the origin of the failure in details.
 * Created by Vincent on 02/02/2016.
 */
@Keep
public enum BluetoothState {



    /**
     * Initial state that corresponds to a standby mode :
     * it represents a state where the mobile device is not connected to any headset and is awaiting order from the user or the SDK.
     * For example, he is awaiting for the user to call the connect method.
     * The IDLE state is automatically returned few minutes after the DATA_BT_DISCONNECTED state is returned
     * (= after disconnection or lost connection or failure during the connection process).
     */
    IDLE,

    /**
     * All the prerequisites are ok to start a bluetooth connection operation
     * (device is not already connected, bluetooth is enabled, location is enabled & location permission is granted)
     */
    READY_FOR_BLUETOOTH_OPERATION,

    /**
     * In case all the connection prerequisites are valid,
     * a scanning has just started to look for an available headset using the LE scan discovery.
     * The Low Energy Scanner is used first, as it is more efficient than the classical Bluetooth discovery Scanner.
     * A specific headset can be targeted if the user specify a headset name when he call the connect method.
     */
    SCAN_STARTED,

    /**
     * Used to notify user when a device has been found during scanning. The device can be a specific
     * one if the user specified one, or the first device scanned if no device has been specified.
     */
    DEVICE_FOUND,

    /**
     * Currently attempting to connect to a Bluetooth remote endpoint
     */
    DATA_BT_CONNECTING,

    /**
     * Successfully connected in BLE or SPP
     */
    DATA_BT_CONNECTION_SUCCESS,

    /**
     *  Retrieving the services and characteristics (list of data where a value is associated to a name) that the connected headset deliver.
     *  This operation is included in the connection process to ensure that a communication has well been established between the headset and the mobile device.
     *  (If the communication has not been established, the data sent by the headset cannot be retrieved by the SDK, so we consider that the connection is not valid)
     */
    DISCOVERING_SERVICES,

    /**
     * Successfully received the services delivered by the connected headset.
     */
    DISCOVERING_SUCCESS,

    /**
     * Getting the headset device informations such as the Serial number, the Firmware or the Hardware version by reading the values returned by the characteristics discovery.
     * This operation is included in the connection process to ensure that the received characteristics can be read (and contains values / are not empty ?) by the SDK.
     * (If the communication is established, but no data (empty data ?) is sent by the headset, we consider that the connection is not valid)
     */
     READING_FIRMWARE_VERSION,

     READING_FIRMWARE_VERSION_SUCCESS,

     READING_HARDWARE_VERSION,

     READING_HARDWARE_VERSION_SUCCESS,

     READING_SERIAL_NUMBER,

     READING_SERIAL_NUMBER_SUCCESS,

     READING_MODEL_NUMBER,

    /**
     * Succes to get all the device informations (Serial number, Firmware version, Hardware version, Model number).
     */
    READING_SUCCESS,


    /**
     * Exchanging and storing of the long term keys for the next times a connection is initiated.
     * This operation is included in the connection process only for headsets whose firmware version are higher than or equal to 1.7.0.
     * Headsets whose firmware version are lower than 1.7.0 can not handle this operation so the bonding step is just skipped.
     * We consider that the headset is connected and ready to acquire data after the Device Info reading operation has returned values (= has not failed).
     */
    BONDING,


    /**
     * Successfully completed bonding operation
     */
    BONDED,

    CHANGING_BT_PARAMETERS,

    BT_PARAMETERS_CHANGED,

    /**
     * Sending the QR Code as an external name to the headset
     */

    SENDIND_QR_CODE,

    /**
     * Successfully connected in BLE or SPP. This state is used when communication is finally possible,
     * For example, we consider that a Melomind headset is usable for streaming if services are discovered, device info have been read, headset is bonded and QR code has been sent if necesseray
     */
    CONNECTED,

    /**
     * Successfully connected in BLE/SPP and A2DP in user requested audio connection and ready to use. This state is used when communication and audio stream are finally possible,
     * For example, we consider that a Melomind headset is usable for streaming if services are discovered, device info have been read, headset is bonded, QR code has been sent if necesseray, and A2DP is connected is the user requested it
     */
    CONNECTED_AND_READY,

    AUDIO_BT_CONNECTION_SUCCESS,

    AUDIO_BT_DISCONNECTED,

    /// FROM THIS STATE, THE CONNECTION PROCESS IS CONSIDERED COMPLETED : THE FOLLOWING STATES DEALS WITH OTHERS BLUETOOTH OPERATIONS

    /**
     * Replacing the current firmware installed by installing a different version of the firmware (should be the last firmware version, but it can also be an downgrading to an old version).
     * This operation requires a connected headset to be performed. Once the upgrade is done, a disconnection is performed to reboot the system.
     */
    UPGRADING,

    /**
     * Failed to replace the current firmware installed with a new one. This failure trigger a disconnection.
     */
    UPGRADING_FAILURE(OADError.ERROR_FIRMWARE_UPDATE_FAILED),

    /**
     * When connection is being disconnected
     */
    DISCONNECTING,

    /**
     * When connection was lost
     */
    DATA_BT_DISCONNECTED,


    /// FROM THIS STATE, THE CONNECTION PROCESS IS CONSIDERED COMPLETED : THE FOLLOWING STATES ARE ERRORS THAT CAN OCCUR DURING A BLUETOOTH OPERATION


    /**
     * Bluetooth is available on device but not enabled (turned on).
     */
    BLUETOOTH_DISABLED(MobileDeviceError.ERROR_BLUETOOTH_DISABLED),
    /**
     * Should not occur (see Android Manifest <code>uses-feature android:name="android.hardware.bluetooth_le" android:required="true"</code>.
     * <p>The device does not have a Bluetooth interface or does not support Bluetooth Low Ebergy</p>
     *
     */
    NO_BLUETOOTH(BluetoothError.ERROR_NOT_SUPPORTED),

    /**
     * When something went wrong but is not necessarily related to Android itself
     */
    INTERNAL_FAILURE(BasicError.ERROR_UNKNOWN),

    /**
     *  Location is required in order to start the LE scan. GPS is disabled
     */
    LOCATION_DISABLED(MobileDeviceError.ERROR_GPS_DISABLED),

    /**
     *  Location is required in order to start the LE scan. Location may or may not be enabled, the user forgot
     *  to grant permissions to access FINE or COARSE location
     *  <p><strong>Note:</strong> this is needed only in Android M and next.</p>
     */
    LOCATION_PERMISSION_NOT_GRANTED(MobileDeviceError.ERROR_LOCATION_PERMISSION),

    /**
     * Although android BLE supports multiple connection, we currently consider that only one connection at a time is possible.
     * Instead of forcing the disconnection of the first device, it is preferable to notify user
     * with error state so that the user can choose if he wants to disconnect the already connected device or not.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs and the SDK will returned to a CONNECTED_AND_READY state.
     */
    ANOTHER_DEVICE_CONNECTED(BluetoothError.ERROR_ALREADY_CONNECTED_ANOTHER),

    /**
     * A2DP connection cannot be established if a Jack cable already connects the headset to the mobile device
     */
    JACK_CABLE_CONNECTED(BluetoothError.ERROR_ALREADY_CONNECTED_JACK),

    /**
     *  Failed to start scan as BLE scan with the same settings is already started by the app. This state is a android.bluetooth.le.ScanCallback state reported if the scan failed.
     */
    SCAN_FAILED_ALREADY_STARTED(BluetoothError.ERROR_ALREADY_SCANNING),

    /**
     * Failed to start scanning operation
     */
    SCAN_FAILURE(BluetoothError.ERROR_SCANNING_FAILED),

    /**
     * Failed to find device during a scanning within a defined allocated amount of time.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs and the device will returned to an "IDLE" state.
     */
    SCAN_TIMEOUT(BluetoothError.ERROR_SCANNING_TIMEOUT),

    /**
     * When the user requests to cancel (stop) the scanning process that is in progress.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs and the device will returned to an "IDLE" state.
     */
    SCAN_INTERRUPTED(BluetoothError.ERROR_SCANNING_INTERRUPTED),

    /**
     * When the user requests to cancel (stop) the connection process that is in progress.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs. This failure trigger a disconnection.
     */
    CONNECTION_INTERRUPTED(BluetoothError.ERROR_CONNECTION_INTERRUPTED),

    /**
     * Failed to establish bluetooth connection with the device : this can be a BLE connection or an A2DP connection
     */
    CONNECTION_FAILURE(BluetoothError.ERROR_CONNECT_FAILED),

    /**
     * Failed to establish Audio bluetooth connection with an unpaired device whose Android version is higher than API 28 (Android 9)
     * Read User Guide to pair the device and establish Audio Bluetooth connection
     */
    AUDIO_CONNECTION_UNSUPPORTED(BluetoothError.ERROR_SETTINGS_INTERFACE_ACTION),

    /**
     * Failed to retrieve the services and characteristics of the connected headset.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs.
     * This failure trigger a disconnection.
     */
    DISCOVERING_FAILURE(BluetoothError.ERROR_CONNECT_FAILED),

    /**
     * Failed to get the device informations (Serial number, Firmware version, Hardware version, Model number).
     * The connection process that was running is automatically cancelled (stopped) if this state occurs.
     * This failure trigger a disconnection.
     */
    READING_FAILURE(BluetoothError.ERROR_CONNECT_FAILED),
    /**
     * Failed to exchange and/or store the long term keys.
     * The connection process that was running is automatically cancelled (stopped) if this state occurs.
     * This failure trigger a disconnection.
     */
    BONDING_FAILURE(BluetoothError.ERROR_CONNECT_FAILED),

    /**
     * Failed to retrieve data. Bluetooth SPP only
     */
    STREAM_ERROR(EegError.ERROR_FAIL_START_STREAMING);



    /**
     * If something went wrong during the Bluetooth connection process, the operation is stopped and the headset is disconnected.
     * The failure/error that happened is transmitted to the SDK via the current state.
     * During a connection, we consider that the current state is a failure state if the headset has not reached the CONNECTED_AND_READY state.
     * The SDK user is notified for every failure that stop a started connection process through the onError method.
     * This method returns the failure in a BaseError type object that describe the origin of the failure in details.
     * If the current state is a failure state, the associatedError contains this BaseError object.
     * Otherwise, the associatedError value is null.
     */
    @Nullable
    private BaseError associatedError;

    /**
     * This constructor is used for the states that are not considered as a failure state (more explanation are available on the description of the associatedError variable)
     * It is included between 0 and the value of BtState.CONNECTED_AND_READY.ordinal(),
     * which returns the ordinal of this enumeration constant (its position in its enum declaration, where the initial constant is assigned an ordinal of zero).
     * For example, the IDLE state is the initial state so its order value is 0.
     */
    BluetoothState() { }

    /**
     * This constructor is used for the states that are considered as a failure state
     * @param error is the Error to return to the SDK user
     */
    BluetoothState(@Nullable BaseError error) {
        this.associatedError = error;
    }

    /**
     * The order is the chronological position of the state that should occur during the Bluetooth connection process.
     * It is equal to the ordinal value
     */
    public Integer getOrder() {
        return this.ordinal();
    }

    @Nullable
    public BaseError getAssociatedError() {
        return associatedError;
    }

    public boolean isAFailureState(){
        return this.associatedError != null;
    }

    /**
     * A reset to IDLE state need to be performed if headset is disconnect, or if the connection process failed before having reached the DATA_BT_CONNECTION_SUCCESS state
     */
    public boolean isResettableState(BluetoothState previousState){
        return (this.equals(DATA_BT_DISCONNECTED) || (this.isDisconnectableState() && previousState.ordinal() < BluetoothState.DATA_BT_CONNECTION_SUCCESS.ordinal()));
    }

    /**
     * A disconnection need to be performed if the connection process failed while the headset was connected
     */
    public boolean isDisconnectableState(){
        return (this.isAFailureState());
    }

    public boolean isReadingDeviceInfoState(){
        return (this.equals(READING_FIRMWARE_VERSION) || this.equals(READING_HARDWARE_VERSION) || this.equals(READING_SERIAL_NUMBER) || this.equals(READING_MODEL_NUMBER));
    }

    public boolean isConnectionInProgress(){
        return this.getOrder() < CONNECTED_AND_READY.ordinal();
    }

    /**
     * @return the step that follow (in chronological order, based on the enum value) the current step
     */
    public BluetoothState getNextConnectionStep(){
        return (!this.isAFailureState() && this.isConnectionInProgress()) ? BluetoothState.values()[this.ordinal()+1]: IDLE;
    }

    public boolean isAudioState(){
        return (this.equals(AUDIO_BT_CONNECTION_SUCCESS) || this.equals(AUDIO_BT_DISCONNECTED));
    }

}
