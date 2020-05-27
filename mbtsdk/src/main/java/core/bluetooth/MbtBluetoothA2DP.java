package core.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import config.MbtConfig;
import core.device.model.MbtDevice;
import core.device.model.MelomindsQRDataBase;
import engine.SimpleRequestCallback;
import features.MbtFeatures;
import utils.VersionHelper;
import utils.LogUtils;
import utils.MbtAsyncWaitOperation;

import static utils.MbtAsyncWaitOperation.CANCEL;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothA2DP 
        extends MbtAudioBluetooth {

    private final static String TAG = MbtBluetoothA2DP.class.getSimpleName();

    private final static String CONNECT_METHOD = "connect";
    private final static String DISCONNECT_METHOD = "disconnect";

    private BluetoothA2dp a2dpProxy;
    
    private MbtAsyncWaitOperation asyncInit = new MbtAsyncWaitOperation<Boolean>();
    private MbtAsyncWaitOperation asyncConnection = new MbtAsyncWaitOperation<Boolean>();
    private MbtAsyncWaitOperation asyncDisconnection = new MbtAsyncWaitOperation<Boolean>();

    public MbtBluetoothA2DP(@NonNull Context context, MbtBluetoothManager mbtBluetoothManager) {
        super(context, BtProtocol.BLUETOOTH_A2DP, mbtBluetoothManager);
    }

    /**
     * A Bluetooth connection lets users stream audio on Bluetooth-enabled devices.
     * The connect method will attempt to connect to the headset via the Advanced Audio Distribution Profile protocol.
     * This protocol is used to transmit the stereo audio signals.
     * Since Android's A2DP Bluetooth class has many hidden methods we use reflexion to access them.
     * This method can handle the case where the end-user device is already connected to the melomind via
     * the A2DP protocol or to another device in which case it will disconnected (Android support only
     * one A2DP headset at the time).
     * @param deviceToConnect    the device to connect to
     * @param context   the application context
     * @return          <code>true</code> upon success, <code>false otherwise</code>
     */
    @Override
    public boolean connect(@NonNull Context context, @NonNull BluetoothDevice deviceToConnect) {
        if(deviceToConnect == null)
            return false;
        LogUtils.d(TAG, "Attempting to connect A2DP to " + deviceToConnect.getName() + " address is "+deviceToConnect.getAddress());

        // First we retrieve the Audio Manager that will help monitor the A2DP status
        // and then the instance of Bluetooth A2DP to make the necessaries calls

        if (a2dpProxy == null) { // failed to retrieve instance of Bluetooth A2DP within alloted time (5 sec)
            LogUtils.e(TAG, "Failed to retrieve instance of Bluetooth A2DP. Cannot perform A2DP operations");
            //notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
            return false;
        }

        // First we check if the end-user device is currently connected to an A2DP device
        if (hasA2DPDeviceConnected()) {
            // End-user is indeed connected to an A2DP device. We retrieve it to see if it is the melomind
            LogUtils.d(TAG, "User device is currently connected to an A2DP Headset. Checking if it is the melomind");

//            if (a2dpProxy.getcurrentDevices() == null || a2dpProxy.getcurrentDevices().isEmpty())
//                connect(context,deviceToConnect); // Somehow end-user is no longer connected (should not happen)

            // we assume there is only one, because Android can only support one at the time
            final BluetoothDevice deviceConnected = a2dpProxy.getConnectedDevices().get(0);
            if (deviceConnected.getAddress().equals(deviceToConnect.getAddress())) { // already connected to the Melomind !
                LogUtils.d(TAG, "Already connected to the melomind.");
                notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS);
                return true;
            } else {
                // The user device is currently connected to a headset that is not the melomind
                // so we disconnect it now and then we connect it to the melomind
                try {
                    final boolean result = (boolean) a2dpProxy.getClass().getMethod(DISCONNECT_METHOD, BluetoothDevice.class)
                            .invoke(a2dpProxy, deviceConnected);

                    if (!result) { // according to doc : "false on immediate error, true otherwise"
                        //notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
                        return false;
                    }
                    // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                    new Timer(true).scheduleAtFixedRate(new TimerTask() {
                        public final void run() {
                            if (!hasA2DPDeviceConnected()) { // the user device is no longer connected to the wrong headset
                                cancel();
                                asyncConnection.stopWaitingOperation(false);
                                notifyConnectionStateChanged(BluetoothState.AUDIO_BT_DISCONNECTED);
                            }
                        }
                    }, 100, 500);
                    Boolean status = false;
                    try {
                        status = (Boolean) asyncConnection.waitOperationResult(5000);
                    } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                        if(e instanceof CancellationException)
                            asyncConnection.resetWaitingOperation();
                    }
                    if (status != null && status) {
                        LogUtils.i(TAG, "successfully disconnected from A2DP device -> " + deviceConnected.getName());
                        LogUtils.i(TAG, "Now connecting A2DP to " + deviceToConnect.getAddress());
                        return connect(context,deviceToConnect);
                    } else {
                        LogUtils.e(TAG, "failed to connect A2DP! future has timed out...");
                        //notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
                        return false;
                    }
                } catch (@NonNull final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    final String errorMsg = " -> " + e.getMessage();
                    if (e instanceof NoSuchMethodException)
                        LogUtils.e(TAG, "Failed to find disconnect method via reflexion" + errorMsg);
                    else if (e instanceof IllegalAccessException)
                        LogUtils.e(TAG, "Failed to access disconnect method via reflexion" + errorMsg);
                    else
                        LogUtils.e(TAG, "Failed to invoke disconnect method via reflexion" + errorMsg);
                    Log.getStackTraceString(e);
                }
            }
        } else {
            LogUtils.i(TAG, "Initiate connection via A2DP! ");
            // Safe to connect to melomind via A2DP
            try {
                final boolean result = (boolean) a2dpProxy.getClass()
                        .getMethod(CONNECT_METHOD, BluetoothDevice.class)
                        .invoke(a2dpProxy, deviceToConnect);

                if (!result) { // according to doc : "false on immediate error, true otherwise"
                    notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
                    return false;
                }

                // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                new Timer(true).scheduleAtFixedRate(new TimerTask() {
                    public final void run() {
                        if (hasA2DPDeviceConnected()) {
                            cancel();
                            asyncConnection.stopWaitingOperation(false);

                        }
                    }
                }, 100, 1500);

                // we give 20 seconds to the user to accepting bonding request
                final int timeout = deviceToConnect.getBondState() == BluetoothDevice.BOND_BONDED ? MbtConfig.getBluetoothA2DpConnectionTimeout() : 25000;
                Boolean status = false;
                try{
                    status = (Boolean) asyncConnection.waitOperationResult(timeout);
                }catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                    if(e instanceof CancellationException)
                        asyncConnection.resetWaitingOperation();
                    LogUtils.i(TAG," A2dp Connection failed: "+e);
                }
                if (status != null && status) {
                    LogUtils.i(TAG, "Successfully connected via A2DP to " + deviceToConnect.getAddress());
                    notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS);
                    return true;
                } else {
                    LogUtils.i(TAG, "Cannot connect to A2DP on device" + deviceToConnect.getAddress());
                    notifyConnectionStateChanged(BluetoothState.CONNECTION_FAILURE);
                    return false;
                }
            } catch (@NonNull final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                final String errorMsg = " -> " + e.getMessage();
                if (e instanceof NoSuchMethodException)
                    LogUtils.e(TAG, "Failed to find connect method via reflexion" + errorMsg);
                else if (e instanceof IllegalAccessException)
                    LogUtils.e(TAG, "Failed to access connect method via reflexion" + errorMsg);
                else LogUtils.e(TAG, "Failed to invoke connect method via reflexion" + errorMsg);
                Log.getStackTraceString(e);
            }
        }
        return false;
    }

    void initA2dpProxy(){
        if (this.bluetoothAdapter != null)
            new A2DPAccessor().initA2DPProxy(context, this.bluetoothAdapter);
    }

    /**
     * This method will attempt to disconnect to the current a2dp device via the A2DP protocol.
     * Since Android's A2DP Bluetooth class has many hidden methods we use reflexion to access them.
     * This method can handle the case where the end-user device is already connected to the melomind via
     * the A2DP protocol or to another device in which case it will disconnected (Android support only
     * one A2DP headset at the time).
     * @return <code>true</code> by default
     */
    @Override
    public boolean disconnect() {
        if(asyncConnection.isWaiting()){
            asyncConnection.stopWaitingOperation(CANCEL);
        }else {
            LogUtils.d(TAG, "Disconnect audio");
            if (this.bluetoothAdapter != null) {
                currentDevice = null;
                if (a2dpProxy != null && !a2dpProxy.getConnectedDevices().isEmpty())
                    currentDevice = a2dpProxy.getConnectedDevices().get(0); //assuming that one device is connected and its obviously the melomind

                mbtBluetoothManager.requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                    @Override
                    public void onRequestComplete(MbtDevice device) {
                        if(device == null)
                            return;
                        if(new VersionHelper(device.getFirmwareVersion().toString()).isValidForFeature(VersionHelper.Feature.A2DP_FROM_HEADSET)){
                            mbtBluetoothManager.disconnectA2DPFromBLE();
                            try {
                                asyncDisconnection.waitOperationResult(MbtConfig.getBluetoothA2DpConnectionTimeout());
                            } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                               if(e instanceof CancellationException)
                                   asyncDisconnection.resetWaitingOperation();
                            } finally {
                                asyncDisconnection.stopWaitingOperation(CANCEL);
                            }
                        }
                        if(isConnected()){
                            try {
                                if (a2dpProxy != null)
                                    a2dpProxy.getClass().getMethod(DISCONNECT_METHOD, BluetoothDevice.class).invoke(a2dpProxy, currentDevice);
                                currentDevice = null;
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            } catch (NoSuchMethodException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        }
        return true;
    }

    /**
     * Checks whether a Bluetooth A2DP audio peripheral is connected or not.
     * @return true if a Bluetooth A2DP audio peripheral is connected, false otherwise
     */
    private boolean hasA2DPDeviceConnected(){
        // First we retrieve the Audio Manager that will help monitor the A2DP status
        // and then the instance of Bluetooth A2DP to make the necessaries calls
        final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        if(audioManager == null)
            return false;
        // First we check if the end-user device is currently connected to an A2DP device
        return audioManager.isBluetoothA2dpOn();
    }

    private List<BluetoothDevice> getA2DPcurrentDevices(){
        if(a2dpProxy == null)
            return Collections.emptyList();

        return a2dpProxy.getConnectedDevices();
    }

    @Override
    public boolean isConnected() {
        return getCurrentState().equals(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS);
    }


    void resetA2dpProxy(int state) {
        if(state == BluetoothAdapter.STATE_ON && a2dpProxy == null && this.bluetoothAdapter != null)
            new A2DPAccessor().initA2DPProxy(context, this.bluetoothAdapter);
    }

    @Override
    public boolean startStream() {
        return false;
    }

    @Override
    public boolean stopStream() {
        return false;
    }

    final class A2DPAccessor implements BluetoothProfile.ServiceListener {
        private A2DPMonitor a2DPMonitor = new A2DPMonitor();
        
        /**
         * The Advanced Audio Distribution Profile (A2DP) profile defines how high quality audio can be streamed from one device to another over a Bluetooth connection.
         * Android provides the BluetoothA2dp class, which is a proxy for controlling the Bluetooth A2DP Service.
         * This method blocks for 5 sec
         * onds while attempting to retrieve the instance of <code>BluetoothA2dp</code>
         * @param context   the application context
         * @param adapter   the Bluetooth Adapter to retrieve the BluetoothA2DP from
         */
        final void initA2DPProxy(@NonNull final Context context, @NonNull final BluetoothAdapter adapter){
            adapter.getProfileProxy(context, this, BluetoothProfile.A2DP); //establish the connection to the proxy
            try {
                asyncInit.waitOperationResult(3000);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LogUtils.d(TAG," No audio connected device ");
            }
        }

        public final void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            if(profile == BluetoothProfile.A2DP) {
                MbtBluetoothA2DP.this.a2dpProxy = (BluetoothA2dp) proxy;
                if (a2DPMonitor != null)
                    a2DPMonitor.start(500);//init A2DP state
            }
        }

        public final void onServiceDisconnected(final int profile) {
            if(profile == BluetoothProfile.A2DP){
                Log.w(TAG, "device is disconnected from service");
                a2DPMonitor.stop();
            }
        }
    }

    /**
     *
     */
    class A2DPMonitor {
        private Timer pollingTimer;
        private List<BluetoothDevice> connectedA2DpDevices;

        A2DPMonitor() {
            this.connectedA2DpDevices = Collections.emptyList();
        }

        public void start(int pollingMillis) {
            this.pollingTimer = new Timer();
            this.pollingTimer.scheduleAtFixedRate(new Task(), 200, pollingMillis);
        }

        public void stop() {
            if(pollingTimer != null){
                this.pollingTimer.cancel();
                this.pollingTimer.purge();
                this.pollingTimer = null;
            }
        }

        private class Task extends TimerTask{

            @Override
            public void run() {
                if(!connectedA2DpDevices.equals(getA2DPcurrentDevices())){ //It means that something has changed. Now we need to find out what changed (getAD2PcurrentDevices returns the connected devices for this specific profile.)
                    if(connectedA2DpDevices.size() < getA2DPcurrentDevices().size()){ //Here, we have a new A2DP connection then we notify bluetooth manager
                        final BluetoothDevice previouscurrentDevice = currentDevice;
                            currentDevice = getA2DPcurrentDevices().get(getA2DPcurrentDevices().size()-1); //As one a2dp output is possible at a time on android, it is possible to consider that last item in list is the current one
                            if(hasA2DPDeviceConnected() && currentDevice!= null && currentDevice.getName()!= null && iscurrentDeviceNameValid()) {//if a Bluetooth A2DP audio peripheral is connected to a device whose name is not null.
                                LogUtils.d(TAG, "Detected connected device "+currentDevice.getName() +" address is "+currentDevice.getAddress());
                                if(previouscurrentDevice == null || (previouscurrentDevice != null && currentDevice!= null && currentDevice != previouscurrentDevice))
                                    notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS, true);
                                asyncInit.stopWaitingOperation(false);
                            }

                    }else //Here, either the A2DP connection has dropped or a new A2DP device is connecting.
                        notifyConnectionStateChanged(BluetoothState.AUDIO_BT_DISCONNECTED);

                    connectedA2DpDevices = getA2DPcurrentDevices(); //In any case, it is mandatory to updated our local connected A2DP list

                }
            }
        }
    }

    private boolean iscurrentDeviceNameValid(){
        if(currentDevice.getName().startsWith(MelomindsQRDataBase.QR_PREFIX) && currentDevice.getName().length() == MelomindsQRDataBase.QR_LENGTH-1)  //if QR code contains only 9 digits
            currentDevice.getName().concat(MelomindsQRDataBase.QR_SUFFIX);

        return (currentDevice.getName().startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) || currentDevice.getName().startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX)) //if device name is a valid BLE name
                || (currentDevice.getName().startsWith(MelomindsQRDataBase.QR_PREFIX) && currentDevice.getName().length() == MelomindsQRDataBase.QR_LENGTH); //or if device name is a valid QR Code name
    }

    public void notifyConnectionStateChanged(BluetoothState newState, boolean notifyManager){
        setCurrentState(newState);
        if(newState.equals(BluetoothState.AUDIO_BT_DISCONNECTED) && asyncDisconnection.isWaiting())
            asyncDisconnection.stopWaitingOperation(false);
        else if (newState.equals(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS)) {
            if(asyncConnection.isWaiting())
                asyncConnection.stopWaitingOperation(false);
            if(notifyManager) //if audio is connected (and BLE is not) when the user request connection to a headset
                mbtBluetoothManager.notifyConnectionStateChanged(BluetoothState.AUDIO_BT_CONNECTION_SUCCESS);
        }
    }

    boolean isPairedDevice(BluetoothDevice device){
        return (bluetoothAdapter != null && bluetoothAdapter.getBondedDevices().contains(device));
    }
}

