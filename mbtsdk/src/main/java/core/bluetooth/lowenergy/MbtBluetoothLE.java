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
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.Log;


import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import config.AmpGainConfig;
import config.DeviceConfig;
import config.FilterConfig;
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
import engine.clientevents.ConnectionStateReceiver;
import features.MbtFeatures;
import utils.BroadcastUtils;
import utils.LogUtils;
import utils.MbtAsyncWaitOperation;

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

    private final static String CONNECT_GATT_METHOD = "connectGatt";
    private final static String REMOVE_BOND_METHOD = "removeBond";
    private final static String REFRESH_METHOD = "refresh";

    private MbtAsyncWaitOperation asyncOperation = new MbtAsyncWaitOperation();

    private MbtAsyncWaitOperation asyncConfiguration = new MbtAsyncWaitOperation();

    /**
     * An internal event used to notify MbtBluetoothLE that A2DP has disconnected.
     */

    @NonNull
    private StreamState streamingState = StreamState.IDLE;

    private MbtGattController mbtGattController;

    private BluetoothLeScanner bluetoothLeScanner;

    BluetoothGatt gatt;

    private ConnectionStateReceiver receiver = new ConnectionStateReceiver() {
        @Override
        public void onError(BaseError error, String additionnalInfo) { }

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
        if(isStreaming())
            return true;

        if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        //Adding small sleep to "free" bluetooth
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
    }

    /**
     * Enable notifications on HeadsetStatus characteristic in order to have the saturation and DC Offset values
     */
    public boolean activateDeviceStatusMonitoring(){
        if (!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        return enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_HEADSET_STATUS));
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
        if(!isStreaming())
            return true;

        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_EEG))
            return false;

        return enableOrDisableNotificationsOnCharacteristic(false, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG));
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
        if (!notificationDescriptor.setValue(enableNotification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
            final StringBuilder sb = new StringBuilder();
            for (final byte value : enableNotification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) {
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
            asyncOperation.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout());
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
        if(!getCurrentState().equals(BtState.DEVICE_FOUND) && !getCurrentState().equals(BtState.CONNECTING))
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
                MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX + new MelomindsQRDataBase(mContext,  true).get(deviceName);
    }

    @Override
    public boolean isConnected() {
        return getCurrentState() == BtState.CONNECTED_AND_READY;
    }

    /**
     * Starts a read operation on a specific characteristic
     * @param characteristic the characteristic to read
     * @return immediatly false on error, true true if read operation has started correctly
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
        if(getCurrentState().isReadingDeviceInfoState())

        LogUtils.i(TAG, "Successfully initiated read characteristic operation");
       return true;
    }

    /**
     * Starts a write operation on a specific characteristic
     * @param characteristic the characteristic to perform write operation on
     * @param payload the payload to write to the characteristic
     * @return immediatly false on error, true otherwise
     */
   synchronized boolean startWriteOperation(@NonNull UUID characteristic, byte[] payload){
       LogUtils.d(TAG, "start write operation");
        if(!checkServiceAndCharacteristicValidity(MelomindCharacteristics.SERVICE_MEASUREMENT, characteristic)) {
            LogUtils.e(TAG, "Error: failed to check service and characteristic validity" + characteristic.toString());
            return false;
        }

        //Send buffer
        this.gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(characteristic).setValue(payload);
        if (!this.gatt.writeCharacteristic(gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(characteristic))) { //the mbtgattcontroller onCharacteristicWrite callback is invoked, reporting the result of the operation.
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
     * Callback called by the {@link MbtGattController gatt controller} when the connection state has changed.
     * @param newState the new {@link BtState state}
     */
    @Override
    public void notifyConnectionStateChanged(@NonNull BtState newState) {
        super.notifyConnectionStateChanged(newState);
        if(newState.equals(BtState.DISCONNECTED)) {
            if (isStreaming())
                notifyStreamStateChanged(StreamState.DISCONNECTED);
            BroadcastUtils.unregisterReceiver(context, receiver);
        }
    }

    void notifyDeviceConfigReceived(byte[] returnedResult, @NonNull String configType) {
        LogUtils.i(TAG, "received config from device "+configType+" | value:"+Arrays.toString(returnedResult));
        if(configType.equals(DeviceConfig.EEG_CONFIG))
            mbtBluetoothManager.notifyDeviceConfigReceived(ArrayUtils.toObject(returnedResult));

        if(configType != null)
            asyncConfiguration.stopWaitingOperation(false);

    }

    void notifyMailboxEventReceived(byte mailboxEvents){
        LogUtils.i(TAG, "received mailbox response for A2DP "+ (mailboxEvents == MailboxEvents.MBX_CONNECT_IN_A2DP ? "connection":"disconnection"));
        if(mailboxEvents == MailboxEvents.MBX_CONNECT_IN_A2DP || mailboxEvents == MailboxEvents.MBX_DISCONNECT_IN_A2DP)
            mbtBluetoothManager.notifyConnectionStateChanged(mailboxEvents == MailboxEvents.MBX_CONNECT_IN_A2DP ? BtState.AUDIO_CONNECTED : BtState.AUDIO_DISCONNECTED);
    }

    void updateConnectionState(boolean isCompleted){
        mbtBluetoothManager.updateConnectionState(isCompleted); //do nothing if the current state is CONNECTED_AND_READY
    }

    /**
     * Send a configuration request to the device if the given configType parameter is :
     * {@link DeviceConfig#MTU_CONFIG}
     * or {@link DeviceConfig#AMP_GAIN_CONFIG}
     * or {@link DeviceConfig#NOTCH_FILTER_CONFIG}
     * or {@link DeviceConfig#OFFSET_CONFIG}
     * or {@link DeviceConfig#P300_CONFIG}
     * or {@link DeviceConfig#MTU_CONFIG}
     * Send a reading request to the device to get the current device configuration if the given config parameter is
     * {@link DeviceConfig#EEG_CONFIG}
     * @param config contains the values that the device has to changed when it receives the request
     * @return
     */
    private boolean waitResultOfDeviceConfiguration(String configType, DeviceConfig config){
        boolean requestSent = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        switch (configType){
            case DeviceConfig.MTU_CONFIG:
                requestSent = changeMTU(config.getMtuValue());
                break;
            case DeviceConfig.NOTCH_FILTER_CONFIG:
                requestSent = changeFilterConfiguration(config.getNotchFilter());
                break;
                case DeviceConfig.AMP_GAIN_CONFIG:
                requestSent = changeAmpGainConfiguration(config.getGainValue());
                break;
            case DeviceConfig.P300_CONFIG:
                requestSent = switchP300Mode(config.isUseP300());
                break;
            case DeviceConfig.OFFSET_CONFIG:
                requestSent = enableOrDisableDcOffset(config.isDcOffsetEnabled());
                break;
            case DeviceConfig.EEG_CONFIG:
                requestSent = requestDeviceConfig();
                break;
        }

        if(requestSent) {
            try {
                asyncConfiguration.waitOperationResult(5000);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LogUtils.w(TAG, configType+" failed : "+e);
            }
        }

        return requestSent;
    }

    /**
     * This method manages a set of calls to perform in order to reconfigure some of the headset's
     * parameters. All parameters are held in a {@link DeviceConfig instance}
     * Each new parameter is updated one after the other. All method inside are blocking.
     * @param config the {@link DeviceConfig} instance to get new parameters from.
     */
    public void configureHeadset(DeviceConfig config){
        if(config != null){
            LogUtils.i(TAG, "configure headset "+config.toString());
            if (config.getMtuValue() != -1) //Checking whether or not there are params to send
                if(!waitResultOfDeviceConfiguration(DeviceConfig.MTU_CONFIG, config))
                    return ;

            if (config.getNotchFilter() != null)
                if(!waitResultOfDeviceConfiguration(DeviceConfig.NOTCH_FILTER_CONFIG, config))
                    return ;

                //TODO implement bandpass filter change

            if (config.getGainValue() != null)
                if(!waitResultOfDeviceConfiguration(DeviceConfig.AMP_GAIN_CONFIG, config))
                    return ;

            if(!waitResultOfDeviceConfiguration(DeviceConfig.P300_CONFIG, config))
                return ;

            if(!waitResultOfDeviceConfiguration(DeviceConfig.OFFSET_CONFIG, config))
                return ;
        }

        waitResultOfDeviceConfiguration(DeviceConfig.EEG_CONFIG, null);
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
    boolean changeMTU(@IntRange(from = 23, to = 121) final int newMTU) {
        LogUtils.i(TAG, "changing mtu to " + newMTU);
        if(!isConnected()){
            return false;
        }
        if(newMTU >= 121 || newMTU <= 23)
            return false;

        if(this.gatt == null)
            return false;

        return this.gatt.requestMtu(newMTU);
    }

    /**
     * Initiates a write operation in order to change the embedded filter values in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @param newConfig the new config.
     */
    private boolean changeFilterConfiguration(@NonNull FilterConfig newConfig){
        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX)){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));
        }

        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_NOTCH_FILT);
        nameToBytes.put((byte)newConfig.getNumVal());
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, nameToBytes.array());

    }

    /**
     * Initiates a write operation in order to change the embedded ampli gain value in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @param newConfig the new config.
     */
    private boolean changeAmpGainConfiguration(@NonNull AmpGainConfig newConfig){
        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX))
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));

        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_AMP_GAIN);
        nameToBytes.put((byte)newConfig.getNumVal());
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, nameToBytes.array());

    }

    /**
     * Initiates a write operation in order to enable or disable the p300 mode in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     * @param useP300 is true to enable, false to disable.
     */
    private boolean switchP300Mode(boolean useP300){
        LogUtils.i(TAG, "switch p300: new mode is " + (useP300 ? "enabled" : "disabled"));

        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX)){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));
        }

        ByteBuffer nameToBytes = ByteBuffer.allocate(2); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_P300_ENABLE);
        nameToBytes.put((byte)(useP300 ? 0x01 : 0x00));
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, nameToBytes.array());
    }


    /**
     * Initiates a write operation in order to enable or disable the DC Offset notifications
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * @param enableOffset is true to enable, false to disable.
     */
    private boolean enableOrDisableDcOffset(boolean enableOffset){
        LogUtils.i(TAG, "DC Offset is " + (enableOffset ? "enabled" : "disabled"));

        if(!isNotificationEnabledOnCharacteristic(MelomindCharacteristics.SERVICE_MEASUREMENT, MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX)){
            enableOrDisableNotificationsOnCharacteristic(true, gatt.getService(MelomindCharacteristics.SERVICE_MEASUREMENT).getCharacteristic(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX));
        }

        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(MailboxEvents.MBX_DC_OFFSET_ENABLE);
        buffer.put((byte)(enableOffset ? 0x01 : 0x00));
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, buffer.array());
    }

    /**
     * Initiates a write operation in order to enable or disable the p300 mode in the melomind firmware.
     * It first requires that notifications are enabled to the {@link MelomindCharacteristics#CHARAC_MEASUREMENT_MAILBOX}
     * charateristic are enabled.
     */
    private boolean requestDeviceConfig(){
        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};
        return startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, code);
    }

    public void sendExternalName(String externalName) {
        if (externalName == null)
            return;
        ByteBuffer nameToBytes = ByteBuffer.allocate(3 + externalName.getBytes().length); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_SERIAL_NUMBER);
        nameToBytes.put((byte) 0xAB);
        nameToBytes.put((byte) 0x21);
        nameToBytes.put(externalName.getBytes());

        byte[] buffer = nameToBytes.array();
        if(!startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, buffer))
            LogUtils.i(TAG, "Failed to send external name update");
        else
            LogUtils.d(TAG, "Sent external name ");
    }

    public void connectA2DPFromBLE() {
        LogUtils.i(TAG, "connect a2dp from ble");
        byte[] buffer = {MailboxEvents.MBX_CONNECT_IN_A2DP, (byte)0x25, (byte)0xA2}; //Send buffer
        if(!startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, buffer))
            LogUtils.w(TAG, "Failed to send connect A2dp request");
    }

    public void disconnectA2DPFromBLE() {
        LogUtils.i(TAG, "disconnected A2DP from BLE");
        byte[] buffer = {MailboxEvents.MBX_DISCONNECT_IN_A2DP, (byte)0x85, (byte)0x11};
        if(!startWriteOperation(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX, buffer))
            LogUtils.w(TAG, "Failed to send disconnect A2dp request");
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
        if(getCurrentState().isReadingDeviceInfoState()){
            updateConnectionState(true); //current state is set to READING_FIRMWARE_VERSION_SUCCESS or READING_HARDWARE_VERSION_SUCCESS or READING_SERIAL_NUMBER_SUCCESS or READING_SUCCESS if reading device info and future is completed
        }
    }

    protected void notifyBatteryReceived(int value) {
        if (getCurrentState().equals(BtState.BONDING))
            updateConnectionState(true); //current state is set to BONDED
        if(value != -1)
            super.notifyBatteryReceived(value);
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod(REFRESH_METHOD);
            if (localMethod != null)
                return (boolean) (Boolean) localMethod.invoke(gatt);
        } catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    void stopWaitingOperation() {
        asyncOperation.stopWaitingOperation(false);
    }
}
