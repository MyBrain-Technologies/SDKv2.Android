package core.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import core.device.model.MbtDevice;
import core.device.model.MelomindsQRDataBase;
import engine.SimpleRequestCallback;
import features.MbtFeatures;
import utils.FirmwareUtils;
import utils.LogUtils;
import utils.MbtLock;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothA2DP extends MbtBluetooth{
    private final static String TAG = MbtBluetoothA2DP.class.getSimpleName();

    private final static String CONNECT_METHOD = "connect";
    private final static String DISCONNECT_METHOD = "disconnect";

    private final MbtLock<Boolean> timerLock = new MbtLock<>();

    private BluetoothA2dp a2dpProxy;

    private BluetoothDevice connectedDevice;

    public MbtBluetoothA2DP(@NonNull Context context, MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
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
        Log.i(TAG," connect in A2DP ");
        if (this.bluetoothAdapter != null)
            new A2DPAccessor().initA2DPProxy(context, this.bluetoothAdapter);

        LogUtils.i(TAG, "Attempting to connect A2DP to " + deviceToConnect.getName());

        // First we retrieve the Audio Manager that will help monitor the A2DP status
        // and then the instance of Bluetooth A2DP to make the necessaries calls

        if (a2dpProxy == null) { // failed to retrieve instance of Bluetooth A2DP within alloted time (5 sec)
            LogUtils.i(TAG, "Failed to retrieve instance of Bluetooth A2DP. Cannot perform A2DP operations");
            notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
            return false;
        }

        // First we check if the end-user device is currently connected to an A2DP device
        if (hasA2DPDeviceConnected()) {
            // End-user is indeed connected to an A2DP device. We retrieve it to see if it is the melomind
            LogUtils.i(TAG, "User device is currently connected to an A2DP Headset. Checking if it is the melomind");

            if (a2dpProxy.getConnectedDevices() == null || a2dpProxy.getConnectedDevices().isEmpty())
                connect(context,deviceToConnect); // Somehow end-user is no longer connected (should not happen)

            // we assume there is only one, because Android can only support one at the time
            final BluetoothDevice deviceConnected = a2dpProxy.getConnectedDevices().get(0);
            if (deviceConnected.getAddress().equals(deviceToConnect.getAddress())) { // already connected to the Melomind !
                LogUtils.i(TAG, "Already connected to the melomind.");
                notifyConnectionStateChanged(BtState.AUDIO_CONNECTED);
                return true;
            } else {
                // The user device is currently connected to a headset that is not the melomind
                // so we disconnect it now and then we connect it to the melomind
                try {
                    final boolean result = (boolean) a2dpProxy.getClass().getMethod(DISCONNECT_METHOD, BluetoothDevice.class)
                            .invoke(a2dpProxy, deviceConnected);

                    if (!result) { // according to doc : "false on immediate error, true otherwise"
                        notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
                        return false;
                    }
                    // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                    new Timer(true).scheduleAtFixedRate(new TimerTask() {
                        public final void run() {
                            if (!hasA2DPDeviceConnected()) { // the user device is no longer connected to the wrong headset
                                cancel();
                                timerLock.setResultAndNotify(true);
                                notifyConnectionStateChanged(BtState.AUDIO_DISCONNECTED);
                            }
                        }
                    }, 100, 500);
                    final Boolean status = timerLock.waitAndGetResult(5000);
                    if (status != null && status) {
                        LogUtils.i(TAG, "successfully disconnected from A2DP device -> " + deviceConnected.getName());
                        LogUtils.i(TAG, "Now connecting A2DP to " + deviceToConnect.getAddress());
                        return connect(context,deviceToConnect);
                    } else {
                        LogUtils.e(TAG, "failed to connect A2DP! Lock has expired...");
                        notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
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
            // Safe to connect to melomind via A2DP
            try {
                final boolean result = (boolean) a2dpProxy.getClass()
                        .getMethod(CONNECT_METHOD, BluetoothDevice.class)
                        .invoke(a2dpProxy, deviceToConnect);

                if (!result) { // according to doc : "false on immediate error, true otherwise"
                    LogUtils.e(TAG, "Failed to initiate connection via A2DP!");
                    notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
                    return false;
                }

                final MbtLock<Boolean> timerLock = new MbtLock<>();

                // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                new Timer(true).scheduleAtFixedRate(new TimerTask() {
                    public final void run() {
                        if (hasA2DPDeviceConnected()) {
                            cancel();
                            timerLock.setResultAndNotify(true);
                        }
                    }
                }, 100, 1500);

                // we give 20 seconds to the user to accepting bonding request
                final long timeout = deviceToConnect.getBondState() == BluetoothDevice.BOND_BONDED ? 10000 : 25000;
                final Boolean status = timerLock.waitAndGetResult(timeout);
                if (status != null && status) {
                    LogUtils.i(TAG, "Successfully connected via A2DP to " + deviceToConnect.getAddress());
                    notifyConnectionStateChanged(BtState.AUDIO_CONNECTED);
                    return true;
                } else {
                    LogUtils.i(TAG, "Cannot connect to A2DP on device" + deviceToConnect.getAddress());
                    notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
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

    private void disconnectExternalDevice(){

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
        if(timerLock.isWaiting()){
            LogUtils.d(TAG, "release a2dpLock in order to ease disconnection");
            timerLock.setResultAndNotify(false);
        }else {
            if (this.bluetoothAdapter != null) {
                connectedDevice = null;
                if (a2dpProxy != null && !a2dpProxy.getConnectedDevices().isEmpty())
                    connectedDevice = a2dpProxy.getConnectedDevices().get(0); //assuming that one device is connected and its obviously the melomind

                mbtBluetoothManager.requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                    @Override
                    public void onRequestComplete(MbtDevice device) {
                        LogUtils.d(TAG, "onRequestComplete A2dp current device is "+device.toString());

                        if(new FirmwareUtils(device.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.A2DP_FROM_HEADSET)){
                            mbtBluetoothManager.disconnectA2DPFromBLE();
                        }else{
                            try {
                                if (a2dpProxy != null)
                                    a2dpProxy.getClass().getMethod(DISCONNECT_METHOD, BluetoothDevice.class).invoke(a2dpProxy, connectedDevice);
                                connectedDevice = null;
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

    private List<BluetoothDevice> getA2DPConnectedDevices(){
        if(a2dpProxy == null)
            return Collections.emptyList();

        return a2dpProxy.getConnectedDevices();
    }

    @Override
    public boolean isConnected() {
        return getCurrentState().equals(BtState.AUDIO_CONNECTED);
    }

    BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    void resetA2dpProxy(int state) {
        Log.i(TAG, "reset A2DP Proxy ");
        if(state == BluetoothAdapter.STATE_ON && a2dpProxy == null && this.bluetoothAdapter != null)
            new A2DPAccessor().initA2DPProxy(context, this.bluetoothAdapter);
    }


    final class A2DPAccessor implements BluetoothProfile.ServiceListener {
        private final MbtLockNew<BluetoothA2dp> lock = new MbtLockNew<>();
        private A2DPMonitor a2DPMonitor = new A2DPMonitor();
        
        /**
         * The Advanced Audio Distribution Profile (A2DP) profile defines how high quality audio can be streamed from one device to another over a Bluetooth connection.
         * Android provides the BluetoothA2dp class, which is a proxy for controlling the Bluetooth A2DP Service.
         * This method blocks for 5 sec
         * onds while attempting to retrieve the instance of <code>BluetoothA2dp</code>
         * @param context   the application context
         * @param adapter   the Bluetooth Adapter to retrieve the BluetoothA2DP from
         * @return          the instance of <code>BluetoothA2dp</code> , <code>null</code> otherwise
         */
        final void initA2DPProxy(@NonNull final Context context, @NonNull final BluetoothAdapter adapter){
            Log.i(TAG, "init A2DP Proxy ");
            adapter.getProfileProxy(context, this, BluetoothProfile.A2DP); //establish the connection to the proxy
        }

        public final void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            Log.i(TAG, "onServiceConnected ");
            if(profile == BluetoothProfile.A2DP) {
                MbtBluetoothA2DP.this.a2dpProxy = (BluetoothA2dp) proxy;
                if (a2DPMonitor != null)
                    a2DPMonitor.start(500);//init A2DP state
            }
        }

        public final void onServiceDisconnected(final int profile) {
            Log.i(TAG, "onServiceDisonnected ");
            if(profile == BluetoothProfile.A2DP){
                Log.e(TAG, "device is disconnected from service");
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
            Log.i(TAG, "starting a2dp monitoring timer");
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
                if(!connectedA2DpDevices.equals(getA2DPConnectedDevices())){ //It means that something has changed. Now we need to find out what changed (getAD2PConnectedDevices returns the connected devices for this specific profile.)

                    if(connectedA2DpDevices.size() < getA2DPConnectedDevices().size()){ //Here, we have a new A2DP connection then we notify bluetooth manager
                        connectedDevice = getA2DPConnectedDevices().get(getA2DPConnectedDevices().size()-1); //As one a2dp output is possible at a time on android, it is possible to consider that last item in list is the current one
                        if(hasA2DPDeviceConnected() && connectedDevice.getName()!= null //if a Bluetooth A2DP audio peripheral is connected to a device whose name is not null.
                               && ((connectedDevice.getName().startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) || connectedDevice.getName().startsWith(MbtFeatures.A2DP_DEVICE_NAME_PREFIX)) //if device name is a valid BLE name
                                    || (connectedDevice.getName().startsWith(MelomindsQRDataBase.QR_PREFIX) && connectedDevice.getName().length() == MelomindsQRDataBase.QR_LENGTH))) //or if device name is a valid QR Code name

                            LogUtils.i(TAG," A2dp monitor found an audio connected device");
                            notifyConnectionStateChanged(BtState.AUDIO_CONNECTED, true);

                    }else //Here, either the A2DP connection has dropped or a new A2DP device is connecting.
                        notifyConnectionStateChanged(BtState.AUDIO_DISCONNECTED);

                    connectedA2DpDevices = getA2DPConnectedDevices(); //In any case, it is mandatory to updated our local connected A2DP list
                }
            }
        }
    }
}
