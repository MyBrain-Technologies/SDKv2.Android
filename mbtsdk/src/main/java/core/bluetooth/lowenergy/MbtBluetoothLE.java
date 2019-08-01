package core.bluetooth.lowenergy;


import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import command.BluetoothCommands;
import command.CommandInterface;
import command.DeviceCommand;

import command.DeviceCommandEvent;
import command.OADCommands;
import config.MbtConfig;
import core.bluetooth.BtProtocol;
import core.bluetooth.BtState;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetooth;
import core.bluetooth.MbtBluetoothManager;
import core.device.model.DeviceInfo;
import core.device.model.MelomindDevice;
import core.device.model.MelomindsQRDataBase;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.ConnectionStateReceiver;
import features.MbtFeatures;
import utils.BroadcastUtils;
import utils.LogUtils;
import utils.BitUtils;
import utils.MbtAsyncWaitOperation;

import static command.DeviceCommandEvent.CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED;
import static command.DeviceCommandEvent.CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED;
import static command.DeviceCommandEvent.CMD_CODE_CONNECT_IN_A2DP_SUCCESS;
import static command.DeviceCommandEvent.MBX_CONNECT_IN_A2DP;

/**
 *
 * This class contains all required methods to interact with a LE bluetooth peripheral, such as Melomind.
 *
 * <p>In order to work {@link Manifest.permission#BLUETOOTH} and {@link Manifest.permission#BLUETOOTH_ADMIN} permissions
 * are required </p>
 *
 * Created by Etienne on 08/02/2018.
 *
 */

public class MbtBluetoothLE extends MbtBluetooth implements IStreamable {
    private static final String TAG = MbtBluetoothLE.class.getSimpleName();

    private final static boolean START = true;
    private final static boolean STOP = false;

    private final static String CONNECT_GATT_METHOD = "connectGatt";
    private final static String REMOVE_BOND_METHOD = "removeBond";
    private final static String REFRESH_METHOD = "refresh";

    private MbtAsyncWaitOperation lock = new MbtAsyncWaitOperation<>();

    /**
     * An internal event used to notify MbtBluetoothLE that A2DP has disconnected.
     */

    private MbtGattController mbtGattController;

    private BluetoothLeScanner bluetoothLeScanner;

    BluetoothGatt gatt;

    private ConnectionStateReceiver receiver = new ConnectionStateReceiver() {
        @Override
        public void onError(BaseError error, String additionalInfo) { }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                LogUtils.d(TAG, "received intent " + action + " for device " + (device != null ? device.getName() : null));
                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                    if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (getCurrentState().equals(BtState.BONDING))
                                    updateConnectionState(true); //current state is set to BONDED & and future is completed
                            }
                        }, 1000);
                    }
                }
            }
        }
    };

    /**
     * public constructor that will instanciate this class. It also instanciate a new
     * {@link MbtGattController MbtGattController} instance
     * @param context the application context
     * @param mbtBluetoothManager the Bluetooth manager that performs requests and receives results.
     */
    public MbtBluetoothLE(@NonNull Context context, MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        this.mbtGattController = new MbtGattController(context, this);
    }

    /**
     * This method sends a request to the headset to <strong><code>START</code></strong>
     * the EEG raw data acquisition process and
     * enables Bluetooth Low Energy notification to receive the raw data.
     * <p><strong>Note:</strong> calling this method will start the raw EEG data acquisition process
     * on the headset which will <strong>consume battery life</strong>. Please consider calling
     * {@link #stopStream()} when EEG raw data are no longer needed.</p>
     *
     * If there is already a streaming session in progress, nothing happens and true is returned.
     *
     * @return              <code>true</code> if request has been sent correctly
     *                      <code>false</code> on immediate error
     */
    @Override
    public synchronized boolean startStream() {
        return switchStream(START);
    }

    /**
     * This method sends a request to the headset to <strong><code>STOP</code></strong>
     * the EEG raw data acquisition process, therefore disabling the Bluetooth Low Energy notification
     * and cleaning reference to previously registered listener.
     * <p>Calling this method will <strong>preserve battery life</strong> by halting the raw EEG
     * data acquisition process on the headset.</p>
     *
     * If there is no streaming session in progress, nothing happens and true is returned.
     *
     * @return true upon correct EEG disability request, false on immediate error
     */
    @Override
    public boolean stopStream() {
        return switchStream(STOP);
    }

    /**
     * This method sends a request to the headset to <strong><code>START</code></strong> or <strong><code>STOP</code></strong>
     * the EEG raw data acquisition process and
     * <strong><code>ENABLES</code></strong> or <strong><code>DISABLED</code></strong>
     * Bluetooth Low Energy notification to receive the raw data.
     * If there is already a streaming session started or stopped, nothing happens and true is returned.
     *
     * @return              <code>true</code> if request has been sent correctly
     *                      <code>false</code> on immediate error
     */
    private boolean switchStream(boolean isStart) {
        if(isStreaming() == isStart)
            return true;

        if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT,
                MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        try {
            Thread.sleep(50); //Adding small sleep to "free" bluetooth
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return enableOrDisableNotificationsOnCharacteristic(isStart,
                gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)
                        .getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
    }

    /**
     * Enable notifications on HeadsetStatus characteristic in order to have the saturation and DC Offset values
     */
    public boolean activateDeviceStatusMonitoring(){
        if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT,
                MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        return enableOrDisableNotificationsOnCharacteristic(true,
                gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT)
                        .getCharacteristic(MelomindCharacteristics.CHARAC_HEADSET_STATUS));
    }

    /**
     * Whenever there is a new stream state, this method is called to notify the bluetooth manager about it.
     * @param newStreamState the new stream state based on {@link StreamState the StreamState enum}
     */
    @Override
    public void notifyStreamStateChanged(StreamState newStreamState) {
        LogUtils.i(TAG, "new streamstate with state " + newStreamState.toString());

        streamingState = newStreamState;
        super.mbtBluetoothManager.notifyStreamStateChanged(newStreamState);
    }


    /**
     * Whenever there is a new headset status received, this method is called to notify the bluetooth manager about it.
     * @param payload the new headset status as a raw byte array. This byte array has to be parsed afterward.
     */
    void notifyNewHeadsetStatus(byte[] payload){
        this.mbtBluetoothManager.notifyNewHeadsetStatus(BtProtocol.BLUETOOTH_LE, payload);
    }

    /**
     *
     * @return true if a streaming session is in progress, false otherwise
     */
    @Override
    public boolean isStreaming() {
        return streamingState == StreamState.STARTED;
    }


    /**
     * Enable or disable notifications on specific characteristic provinding this characteristic is "notification ready".
     * @param enableNotification enabling if set to true, false otherwise
     * @param characteristic the characteristic to enable or disable notification on.
     *
     * This operation is synchronous, meaning the thread running this method is blocked until the operation completes.
     * @return  <code>true</code> if the notification has been successfully established within the 2 seconds of allotted time,
     * or <code>false</code> for any error
     */
    synchronized boolean enableOrDisableNotificationsOnCharacteristic(boolean enableNotification, @NonNull BluetoothGattCharacteristic characteristic) {
        if(!isConnected() && !getCurrentState().equals(BtState.SENDIND_QR_CODE))
            return false;

        LogUtils.i(TAG, "Now enabling local notification for characteristic: " + characteristic.getUuid());
        if (!this.gatt.setCharacteristicNotification(characteristic, enableNotification)) {
            LogUtils.e(TAG, "Failed to enable local notification for characteristic: " + characteristic.getUuid());
            return false;
        }

        final BluetoothGattDescriptor notificationDescriptor =
                characteristic.getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID);
        if (notificationDescriptor == null) {
            LogUtils.e(TAG, String.format("Error: characteristic with " +
                            "UUID <%s> does not have a descriptor (UUID <%s>) to enable notification remotely!",
                    characteristic.getUuid().toString(), MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString()));
            return false;
        }

        LogUtils.i(TAG, "Now enabling remote notification for characteristic: " + characteristic.getUuid());
        if (!notificationDescriptor.setValue(enableNotification ?
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
            final StringBuilder sb = new StringBuilder();
            for (final byte value : enableNotification ?
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) {
                sb.append(value);
                sb.append(';');
            }
            LogUtils.e(TAG, String.format("Error: characteristic's notification descriptor with " +
                            "UUID <%s> could not store the ENABLE notification value <%s>.",
                    MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID.toString(), sb.toString()));
            return false;
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!this.gatt.writeDescriptor(notificationDescriptor)) {
            LogUtils.e(TAG, "Error: failed to initiate write descriptor operation in order to remotely " +
                    "enable notification for characteristic: " + characteristic.getUuid());
            return false;
        }

        LogUtils.i(TAG, "Successfully initiated write descriptor operation in order to remotely " +
                "enable notification... now waiting for confirmation from headset.");

        try {
            lock.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout());
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.d(TAG,"Enabling notification failed : "+e);
            return false;
        }
    }


    /**
     * Start bluetooth low energy scanner in order to find BLE device that matches the specific filters.
     * <p><strong>Note:</strong> This method will consume your mobile/tablet battery. Please consider calling
     * {@link #stopLowEnergyScan()} when scanning is no longer needed.</p>
     * @return Each found device that matches the specified filters
     */
    public boolean startLowEnergyScan(boolean filterOnDeviceService) {
        LogUtils.i(TAG," start low energy scan on device "+mbtBluetoothManager.getDeviceNameRequested());
        List<ScanFilter> mFilters = new ArrayList<>();

        if (super.bluetoothAdapter == null || super.bluetoothAdapter.getBluetoothLeScanner() == null){
            Log.e(TAG, "Unable to get LE scanner");
            notifyConnectionStateChanged(BtState.SCAN_FAILURE);
            return false;
        }else
            this.bluetoothLeScanner = super.bluetoothAdapter.getBluetoothLeScanner();

        currentDevice = null;

        if (filterOnDeviceService) {
            final ScanFilter.Builder filterService = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(MelomindCharacteristics.SERVICE_MEASUREMENT));

            if(mbtBluetoothManager.getDeviceNameRequested() != null)
                filterService.setDeviceName(mbtBluetoothManager.getDeviceNameRequested());

            mFilters.add(filterService.build());
        }

        final ScanSettings settings = new ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        LogUtils.i(TAG, String.format("Starting Low Energy Scan with filtering on name '%s' and service UUID '%s'", mbtBluetoothManager.getDeviceNameRequested(), MelomindCharacteristics.SERVICE_MEASUREMENT));
        this.bluetoothLeScanner.startScan(mFilters, settings, this.leScanCallback);
        if(getCurrentState().equals(BtState.READY_FOR_BLUETOOTH_OPERATION))
            mbtBluetoothManager.updateConnectionState(false); //current state is set to SCAN_STARTED
        return true; //true : scan is started
    }


    /**
     * Stops the currently bluetooth low energy scanner.
     * If a lock is currently waiting, the lock is disabled.
     */
    public void stopLowEnergyScan() {
        LogUtils.i(TAG, "Stopping Low Energy scan");
        if(this.bluetoothLeScanner != null)
            this.bluetoothLeScanner.stopScan(this.leScanCallback);
        if(!getCurrentState().equals(BtState.DEVICE_FOUND) && !getCurrentState().equals(BtState.DATA_BT_CONNECTING))
            currentDevice = null;
    }



    /**
     * callback used when scanning using bluetooth Low Energy scanner.
     */
    @NonNull
    private ScanCallback leScanCallback = new ScanCallback() {

        public void onScanResult(int callbackType, @NonNull ScanResult result) { //Callback when a BLE advertisement has been found.
            if(getCurrentState().equals(BtState.SCAN_STARTED)){
                super.onScanResult(callbackType, result);
                final BluetoothDevice device = result.getDevice();
                LogUtils.i(TAG, String.format("Stopping Low Energy Scan -> device detected " + "with name '%s' and MAC address '%s' ", device.getName(), device.getAddress()));
                currentDevice = device;
                updateConnectionState(true); //current state is set to DEVICE_FOUND and future is completed
            }
        }

        public final void onScanFailed(final int errorCode) { //Callback when scan could not be started.
            super.onScanFailed(errorCode);
            String msg = "Could not start scan. Reason -> ";
           if(errorCode == SCAN_FAILED_ALREADY_STARTED) {
               msg += "Scan already started!";
               notifyConnectionStateChanged(BtState.SCAN_FAILED_ALREADY_STARTED);
           }else{
               msg += "Scan failed. No more details.";
               notifyConnectionStateChanged(BtState.SCAN_FAILURE);
            }
            LogUtils.e(TAG, msg);
        }
    };

    /**
     * This method removes bonding of the device.
     */
    public void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod(REMOVE_BOND_METHOD, (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Starts the connect operation in order to connect the {@link BluetoothDevice bluetooth device} (peripheral)
     * to the terminal (central).
     * If the operation starts successfully, a new {@link BluetoothGatt gatt} instance will be stored.
     * @param context the context which the connection event takes place in.
     * @param device the bluetooth device to connect to.
     * @return true if operation has correctly started, false otherwise.
     */
    @Override
    public boolean connect(Context context, BluetoothDevice device) {
        if(device == null || context == null)
            return false;
        LogUtils.i(TAG," connect in Low Energy "+device.getName()+" address is "+device.getAddress());
        BroadcastUtils.registerReceiverIntents(context, receiver, BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        //Using reflexion here because min API is 21 and transport layer is not available publicly until API 23
        try {
            final Method connectGattMethod = device.getClass()
                    .getMethod(CONNECT_GATT_METHOD,
                            Context.class, boolean.class, BluetoothGattCallback.class, int.class);

            final int transport = device.getClass().getDeclaredField("TRANSPORT_LE").getInt(null);
            this.gatt = (BluetoothGatt) connectGattMethod.invoke(device, context, false, mbtGattController, transport);
            return true;

        } catch (@NonNull final NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            final String errorMsg = " -> " + e.getMessage();
            if (e instanceof NoSuchMethodException)
                LogUtils.e(TAG, "Failed to find connectGatt method via reflexion" + errorMsg);
            else if (e instanceof NoSuchFieldException)
                LogUtils.e(TAG, "Failed to find Transport LE field via reflexion" + errorMsg);
            else if (e instanceof IllegalAccessException)
                LogUtils.e(TAG, "Failed to access Transport LE field via reflexion" + errorMsg);
            else
                LogUtils.e(TAG, "Failed to invoke connectGatt method via reflexion" + errorMsg);
            Log.getStackTraceString(e);
        }
        return false;
    }


    /**
     * Disconnects from the currently connected {@link BluetoothGatt gatt instance} and sets it to null
     */
    @Override
    public boolean disconnect() {
        LogUtils.i(TAG, "Disconnect in low energy");
        if(this.gatt != null){
            this.gatt.disconnect();
        }
        this.gatt = null;
        return false;
    }

    public boolean isCurrentDeviceNameEqual(String deviceName){
        return (gatt != null && gatt.getDevice() != null && gatt.getDevice().getName().equals(deviceName));
    }

    public String getBleDeviceNameFromA2dp(String deviceName, Context mContext){
       return MelomindDevice.isDeviceNameValidForMelomind(deviceName) ?
                deviceName.replace(MbtFeatures.A2DP_DEVICE_NAME_PREFIX, MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) : //audio_ prefix is replaced by a melo_ prefix
                new MelomindsQRDataBase(mContext,  true).get(deviceName);
    }

    @Override
    public boolean isConnected() {
        return (getCurrentState() == BtState.CONNECTED_AND_READY || getCurrentState() == BtState.CONNECTED);
    }


    public boolean isConnectedDeviceReadyForCommand() {
        return (getCurrentState().ordinal() >= BtState.DATA_BT_CONNECTION_SUCCESS.ordinal());
    }

    /**
     * Starts a read operation on a specific characteristic
     * @param characteristic the characteristic to read
     * @return immediatly false on error, true true if read operation has st
     * arted correctly
     */
   boolean startReadOperation(@NonNull UUID characteristic){
       if(!isConnected() && !getCurrentState().equals(BtState.DISCOVERING_SUCCESS) && !getCurrentState().isReadingDeviceInfoState() && !getCurrentState().equals(BtState.BONDING)) {
            notifyConnectionStateChanged( getCurrentState().equals(BtState.BONDING) ?
                            BtState.BONDING_FAILURE : BtState.READING_FAILURE);
            return false;
        }
       UUID service = (characteristic.equals(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION)
                || characteristic.equals(MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION)
                || characteristic.equals(MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER)
                || characteristic.equals(MelomindCharacteristics.CHARAC_INFO_MODEL_NUMBER)) ?
             MelomindCharacteristics.SERVICE_DEVICE_INFOS : MelomindCharacteristics.SERVICE_MEASUREMENT;

        if(!checkServiceAndCharacteristicValidity(service, characteristic)) {
            notifyConnectionStateChanged( getCurrentState().equals(BtState.BONDING) ?
                    BtState.BONDING_FAILURE : BtState.READING_FAILURE);
            return false;
        }

       if (!this.gatt.readCharacteristic(gatt.getService(service).getCharacteristic(characteristic))) {
           LogUtils.e(TAG, "Error: failed to initiate read characteristic operation");
            if(getCurrentState().equals(BtState.BONDING) || getCurrentState().isReadingDeviceInfoState())
                notifyConnectionStateChanged( getCurrentState().equals(BtState.BONDING) ? // bonding is triggered by a reading battery operation
                    BtState.BONDING_FAILURE : BtState.READING_FAILURE);
            return false;
        }
        //if(getCurrentState().isReadingDeviceInfoState())

        LogUtils.i(TAG, "Successfully initiated read characteristic operation");
       return true;
    }

    /**
     * Starts a write operation on a specific characteristic
     * @param characteristic the characteristic to perform write operation on
     * @param payload the payload to write to the characteristic
     * @return immediatly false on error, true otherwise
     */
   synchronized boolean startWriteOperation(@NonNull UUID service, @NonNull UUID characteristic, byte[] payload){
        if(!checkServiceAndCharacteristicValidity(service, characteristic)) {
            LogUtils.e(TAG, "Error: failed to check service and characteristic validity" + characteristic.toString());
            return false;
        }

        //Send buffer
        this.gatt.getService(service).getCharacteristic(characteristic).setValue(payload);
       LogUtils.d(TAG, "write "+ Arrays.toString(gatt.getService(service).getCharacteristic(characteristic).getValue()));
       if (!this.gatt.writeCharacteristic(gatt.getService(service).getCharacteristic(characteristic))) { //the mbtgattcontroller onCharacteristicWrite callback is invoked, reporting the result of the operation.
            LogUtils.e(TAG, "Error: failed to write characteristic " + characteristic.toString());
            return false;
        }

       return true;
    }


    /**
     * Checks whether the service and characteristic about to be used to communicate with the remote device.
     * @param service the service to check
     * @param characteristic the characteristic to check
     * @return false if something not valid, true otherwise
     */
   boolean checkServiceAndCharacteristicValidity(@NonNull UUID service, @NonNull UUID characteristic){
       return gatt != null &&
               gatt.getService(service) != null &&
               gatt.getService(service).getCharacteristic(characteristic) != null;
   }

    /**
     * Checks if the charateristic has notifications already enabled or not.
     * @param service the Service UUID that holds the characteristic
     * @param characteristic the characteristic UUID.
     * @return true is already enabled notifications, false otherwise.
     */
    boolean isNotificationEnabledOnCharacteristic(@NonNull UUID service, @NonNull UUID characteristic){
        if(!checkServiceAndCharacteristicValidity(service, characteristic))
            return false;

        if(this.gatt.getService(service).getCharacteristic(characteristic).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID) == null)
            return false;

        return Arrays.equals(this.gatt.getService(service).getCharacteristic(characteristic).getDescriptor(MelomindCharacteristics.NOTIFICATION_DESCRIPTOR_UUID).getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    /**
     * Initiates a read battery operation on this correct BtProtocol
     */
    public boolean readBattery() {
        LogUtils.i(TAG, "read battery");
        return startReadOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_BATTERY_LEVEL);
    }

    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     */
    public boolean readFwVersion(){
        LogUtils.i(TAG, "read firmware version");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION);
    }
    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     */
    public boolean readHwVersion(){
        LogUtils.i(TAG, "read hardware version");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION);
    }

    /**
     * Initiates a read serial number operation on this correct BtProtocol
     */
    public boolean readSerialNumber(){
        LogUtils.i(TAG, "read serial number requested");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER);
    }

    /**
     * Initiates a read model number operation on this correct BtProtocol
     */
    public boolean readModelNumber(){
        LogUtils.i(TAG, "read product name");
        return startReadOperation(MelomindCharacteristics.CHARAC_INFO_MODEL_NUMBER);
    }

    /**
     * Callback called by the {@link MbtGattController gatt controller} when the notification state has changed.
     * @param isSuccess if the modification state is correctly changed
     * @param characteristic the characteristic which had its notification state changed
     * @param wasEnableRequest if the request was to enable (true) or disable (false) request.
     */
    void onNotificationStateChanged(boolean isSuccess, BluetoothGattCharacteristic characteristic, boolean wasEnableRequest) {
        if(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG.equals(characteristic.getUuid())){
            if(wasEnableRequest && isSuccess){
                notifyStreamStateChanged(StreamState.STARTED);
            }else if(!wasEnableRequest && isSuccess){
                notifyStreamStateChanged(StreamState.STOPPED);
            }else{notifyStreamStateChanged(StreamState.FAILED);}
        }else if (MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX.equals(characteristic.getUuid())){
            //TODO see what's important here
        }else if (MelomindCharacteristics.CHARAC_HEADSET_STATUS.equals(characteristic.getUuid())){
            //TODO see what's important here
        }
    }

    /**
     * Close gatt if the current state is connected & ready or upgrading
     * @param gatt
     */
    void onStateDisconnected(@NonNull BluetoothGatt gatt) {
        if(gatt != null && getCurrentState().ordinal() >= BtState.CONNECTED_AND_READY.ordinal())
            gatt.close();

        notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
    }

    void onStateConnecting() {
        if (getCurrentState().equals(BtState.DEVICE_FOUND))
            this.updateConnectionState(false);//current state is set to DATA_BT_CONNECTING
    }

    void onStateConnected() {
        if (getCurrentState().equals(BtState.DATA_BT_CONNECTING) || getCurrentState().equals(BtState.SCAN_STARTED))
            updateConnectionState(true);//current state is set to DATA_BT_CONNECTION_SUCCESS and future is completed
        else if(getCurrentState().equals(BtState.IDLE) || getCurrentState().equals(BtState.UPGRADING))
            this.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
    }

    /**
     * Callback triggered by the {@link MbtGattController} callback when the connection state has changed.
     * @param newState the new {@link BtState state}
     */
    @Override

    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        super.notifyConnectionStateChanged(newState);

        if (newState.equals(BtState.DATA_BT_DISCONNECTED)) {
            if (isStreaming())
                notifyStreamStateChanged(StreamState.DISCONNECTED);
            BroadcastUtils.unregisterReceiver(context, receiver);
        }
    }

    /**
     * Callback triggered by the {@link MbtGattController} callback
     * when an event -not related to a mailbox request sent by the SDK- occurs
     * @param mailboxEvent the event that occurs
     * @param eventData the data associated to the mailbox event detected
     */
    void notifyEventReceived(DeviceCommandEvent mailboxEvent, byte[] eventData) {
        LogUtils.d(TAG, "Event received " + Arrays.toString(eventData));
        mbtBluetoothManager.notifyEventReceived(mailboxEvent, eventData);
    }

    void stopWaitingOperation(Object response) {
        if(lock.isWaiting())
            lock.stopWaitingOperation(response);

    }

    void notifyConnectionResponseReceived(DeviceCommandEvent mailboxEvent, byte mailboxResponse) {
        if (!mbtGattController.isConnectionMailboxEvent(mailboxEvent)){
            LogUtils.e(TAG, "Error : received response is not related to Bluetooth connection");
            return;
        }
        LogUtils.i(TAG, "Received response for " + (mailboxEvent == DeviceCommandEvent.MBX_CONNECT_IN_A2DP ? "connection" : "disconnection" + " : " + mailboxResponse));

        if(mailboxEvent == DeviceCommandEvent.MBX_CONNECT_IN_A2DP){
            if(BitUtils.areByteEquals(MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_JACK_CONNECTED), mailboxResponse))
                mbtBluetoothManager.notifyConnectionStateChanged(BtState.JACK_CABLE_CONNECTED);

            else if(BitUtils.areByteEquals(MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_SUCCESS), mailboxResponse)
                || BitUtils.areByteEquals(MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_FAILED_ALREADY_CONNECTED), mailboxResponse))
                mbtBluetoothManager.notifyConnectionStateChanged(BtState.AUDIO_BT_CONNECTION_SUCCESS);
        }else
            mbtBluetoothManager.notifyConnectionStateChanged(BtState.AUDIO_BT_DISCONNECTED);
    }

    void updateConnectionState(boolean isCompleted){
        mbtBluetoothManager.updateConnectionState(isCompleted); //do nothing if the current state is CONNECTED_AND_READY
    }

    /**
     * This method waits until the device has returned a response
     * related to the SDK request (blocking method).
     */
    private Object waitResponseForCommand(CommandInterface.MbtCommand command){
        Log.d(TAG, "Wait response of device command "+command);
            try {
                return lock.waitOperationResult(11000);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LogUtils.e(TAG, "Device command response not received : "+e);
                if(e instanceof TimeoutException)
                    command.onError(BluetoothError.ERROR_TIMEOUT, "Device command sent but no response has been received.");
            }
        return null;
    }

    /**
     * This method handle a single command in order to
     * reconfigure some headset or bluetooth streaming parameters
     * or get values stored by the headset
     * or ask the headset to perform an action.
     * The command's parameters are bundled in a {@link command.CommandInterface.MbtCommand instance}
     * that can provide a nullable response callback.
     * All method inside are blocking.
     * @param command is the {@link command.CommandInterface.MbtCommand} object that defines the type of command to send
     * and the associated command parameters.
     * One of this parameter is an optional callback that returns the response
     * sent by the headset to the SDK once the command is received.
     */
    public void sendCommand(CommandInterface.MbtCommand command){
        Object response = null;

        if (!isConnectedDeviceReadyForCommand()){ //error returned if no headset is connected
            LogUtils.e(TAG, "Command not sent : "+command);
            command.onError(BluetoothError.ERROR_NOT_CONNECTED, null);
        } else { //any command is not sent if no device is connected
            if (command.isValid()){//any invalid command is not sent : validity criteria are defined in each Bluetooth implemented class , the onError callback is triggered in the constructor of the command object
                LogUtils.d(TAG, "Valid command : "+command);
                boolean requestSent = sendRequestData(command);

                if(!requestSent) {
                    LogUtils.e(TAG, "Command sending failed");
                    command.onError(BluetoothError.ERROR_REQUEST_OPERATION, null);

                }else {
                    command.onRequestSent();

                    if (command.isResponseExpected()) {
                        response = waitResponseForCommand(command);
                        command.onResponseReceived(response);
                    }
                }
            }else
                LogUtils.w(TAG, "Command not sent : "+command);
        }

        mbtBluetoothManager.notifyResponseReceived(response, command);//return null response to the client if request has not been sent
    }

    private boolean sendRequestData(CommandInterface.MbtCommand command){
        if(command instanceof BluetoothCommands.Mtu)
            return changeMTU(((Integer)command.serialize()));

        else if(command instanceof DeviceCommand)
            return writeCharacteristic((byte[])command.serialize(),
                    MelomindCharacteristics.SERVICE_MEASUREMENT,
                    command instanceof OADCommands.SendPacket ?
                            MelomindCharacteristics.CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER :
                            MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX,
                    !(command instanceof OADCommands.SendPacket));

        return false;
    }

    /**
     * Initiates a change MTU request in order to have bigger (or smaller) bluetooth notifications.
     * The default size is also the minimum size : 23
     * The maximum size is set to 121.
     * This method is synchronous and blocks the calling thread until operation is complete.
     *
     *
     * See {@link BluetoothGatt#requestMtu(int)} for more info.
     *
     * @param newMTU the new MTU value.
     *
     * @return false if request dod not start as planned, true otherwise.
     */
    boolean changeMTU(final int newMTU) {
        LogUtils.i(TAG, "change mtu " + newMTU);

        if(this.gatt == null)
            return false;

        return this.gatt.requestMtu(newMTU);
    }

    private boolean writeCharacteristic(@NonNull byte[] buffer, UUID service, UUID characteristic, boolean enableNotification) {
        Log.d(TAG, "write characteristic "+characteristic+ " for service "+service);
        if (buffer.length == 0)
            return false;

        if(!isNotificationEnabledOnCharacteristic(service, characteristic) && enableNotification){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(service).getCharacteristic(characteristic));
        }

        if (!startWriteOperation(service, characteristic, buffer)){
            LogUtils.e(TAG, "Failed to send the command the the headset");
            return false;
        }else
            LogUtils.d(TAG, "Command sent to the headset");
        return true;
    }

    /**
     * Once a device is connected in Bluetooth Low Energy / SPP for data streaming, we consider that the Bluetooth connection process is not fully completed.
     * The services offered by a remote device as well as their characteristics and descriptors are discovered to ensure that Data Streaming can be performed.
     * It means that the Bluetooth Manager retrieve all the services, which can be seen as categories of data that the headset is transmitting
     * This is an asynchronous operation.
     * Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered.
     * If the discovery was successful, the remote services can be retrieved using the getServices function
     */
    public void discoverServices() {
        LogUtils.i(TAG, "start discover services");
        updateConnectionState(false); //current state is set to DISCOVERING_SERVICES
        if(!gatt.discoverServices()){
            notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE);
            LogUtils.i(TAG, " discover services failed");
        }
    }

    /**
     * Starts a read operation of the Battery charge level to trigger an automatic bonding.
     * If the headset is already bonded, it will return the value of the battery level.
     * If the headset is not already bonded, it will bond and return an authentication failed status code (0x89 GATT_AUTH_FAIL)
     * in the {@link MbtGattController#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}onCharacteristicRead callback
     */
    public void bond() {
        LogUtils.i(TAG, "start bonding");
        if(getCurrentState().equals(BtState.READING_SUCCESS))
            updateConnectionState(false); //current state is set to BONDING
        mbtBluetoothManager.startReadOperation(DeviceInfo.BATTERY); //trigger bonding indirectly
    }

    public void notifyDeviceInfoReceived(@NonNull DeviceInfo deviceInfo, @NonNull String deviceValue){ // This method will be called when a DeviceInfoReceived is posted (fw or hw or serial number) by MbtBluetoothLE or MbtBluetoothSPP
        super.notifyDeviceInfoReceived(deviceInfo,deviceValue);
        if(getCurrentState().isReadingDeviceInfoState())
            updateConnectionState(true); //current state is set to READING_FIRMWARE_VERSION_SUCCESS or READING_HARDWARE_VERSION_SUCCESS or READING_SERIAL_NUMBER_SUCCESS or READING_SUCCESS if reading device info and future is completed
    }

    protected void notifyBatteryReceived(int value) {
        if (getCurrentState().equals(BtState.BONDING))
            updateConnectionState(true); //current state is set to BONDED
        if(value != -1)
            super.notifyBatteryReceived(value);
    }

    /**
     * This method uses reflexion to get the refresh hidden method from BluetoothGatt class. Is is used
     * to clean up the cache that Android system uses when connecting to a known BluetoothGatt peripheral.
     * It is recommanded to use it right after updating the firmware, especially when the bluetooth
     * characteristics have been updated.
     * @return true if method invocation worked, false otherwise
     */
    @Override
    public boolean clearMobileDeviceCache() {
        LogUtils.d(TAG, "Clear the cache");
        try {
            Method localMethod = gatt.getClass().getMethod(REFRESH_METHOD);
            if (localMethod != null)
                return (boolean) (Boolean) localMethod.invoke(gatt);
        } catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
        }
        return false;
    }

    @VisibleForTesting
    void setLock(MbtAsyncWaitOperation lock) {
        this.lock = lock;
    }
}