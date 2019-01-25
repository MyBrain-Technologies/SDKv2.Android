package core.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import core.device.model.MbtDevice;
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
    static final String A2DP_CONNECTED_EVENT = "A2DP_CONNECTED_EVENT";
    static final String A2DP_DISCONNECTED_EVENT = "A2DP_DISCONNECTED_EVENT";

    private final MbtLock<Boolean> timerLock = new MbtLock<>();

    private BluetoothA2dp a2dpProxy;

    private BluetoothDevice connectedDevice;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MbtBluetoothA2DP(@NonNull Context context, MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
        this.setCurrentState(BtState.IDLE);
        if (this.bluetoothAdapter != null)
            new A2DPAccessor().initA2DPProxy(context, this.bluetoothAdapter);
    }

    /**
     * This method will attempt to connect to the melomind via the A2DP protocol.
     * Since Android's A2DP Bluetooth class has many hidden methods we use reflexion to access them.
     * This method can handle the case where the end-user device is already connected to the melomind via
     * the A2DP protocol or to another device in which case it will disconnected (Android support only
     * one A2DP headset at the time).
     * @param device    the device to connect to
     * @param context   the application context
     * @return          <code>true</code> upon success, <code>false otherwise</code>
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean connect(@NonNull Context context, @NonNull BluetoothDevice device) {
        LogUtils.i(TAG, "Attempting to connect A2DP to " + device.getName());

        // First we retrieve the Audio Manager that will help monitor the A2DP status
        // and then the instance of Bluetooth A2DP to make the necessaries calls

        if (a2dpProxy == null) { // failed to retrieve instance of Bluetooth A2DP within alloted time (5 sec)
            LogUtils.i(TAG, "Failed to retrieve instance of Bluetooth A2DP. Cannot perform A2DP operations");
            notifyConnectionStateChanged(BtState.CONNECTION_FAILURE, true);
            return false;
        }

        // First we check if the end-user device is currently connected to an A2DP device
        if (context.getSystemService(Context.AUDIO_SERVICE) != null) {
            if (hasA2DPDeviceConnected()) {
                // End-user is indeed connected to an A2DP device. We retrieve it to see if it is the melomind
                LogUtils.i(TAG, "User device is currently connected to an A2DP Headset. Checking if it is the melomind");

                if (a2dpProxy.getConnectedDevices() == null || a2dpProxy.getConnectedDevices().isEmpty())
                    connect(context,device); // Somehow end-user is no longer connected (should not happen)

                // we assume there is only one, because Android can only support one at the time
                final BluetoothDevice connected = a2dpProxy.getConnectedDevices().get(0);
                if (connected.getAddress().equals(device.getAddress())) { // already connected to the Melomind !
                    LogUtils.i(TAG, "Already connected to the melomind.");
                    notifyConnectionStateChanged(BtState.CONNECTED_AND_READY, false);
                    return true;
                } else {
                    // The user device is currently connected to a headset that is not the melomind
                    // so we disconnect it now and then we connect it to the melomind
                    try {
                        final boolean result = (boolean) a2dpProxy.getClass().getMethod(DISCONNECT_METHOD, BluetoothDevice.class)
                                .invoke(a2dpProxy, connected);

                        if (!result) { // according to doc : "false on immediate error, true otherwise"
                            return false;
                        }
                        // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                        new Timer(true).scheduleAtFixedRate(new TimerTask() {
                            public final void run() {
                                if (!hasA2DPDeviceConnected()) { // the user device is no longer to connected to the wrong headset
                                    cancel();
                                    timerLock.setResultAndNotify(true);
                                }
                            }
                        }, 100, 500);

                        final Boolean status = timerLock.waitAndGetResult(5000);
                        if (status != null && status) {
                            LogUtils.i(TAG, "successfully disconnected from A2DP device -> " + connected.getName());
                            LogUtils.i(TAG, "Now connecting A2DP to " + device.getAddress());
                            return connect(context,device);
                        } else {
                            LogUtils.e(TAG, "failed to connect with A2DP! Lock has expired...");
                            // TODO fail to disconnect from wrong headset (should not happen)
                            return false;
                        }
                    } catch (@NonNull final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        final String errorMsg = " -> " + e.getMessage();
                        if (e instanceof NoSuchMethodException)
                            LogUtils.e(TAG, "Failed to find disconnect method via reflexion" + errorMsg);
                        else if (e instanceof IllegalAccessException)
                            LogUtils.e(TAG, "Failed to access disconnect method via reflexion" + errorMsg);
                        else if (e instanceof InvocationTargetException)
                            LogUtils.e(TAG, "Failed to invoke disconnect method via reflexion" + errorMsg);
                        else
                            LogUtils.e(TAG, "Unable to disconnect to current headset with reflexion. Reason" + errorMsg);
                        Log.getStackTraceString(e);
                    }
                }
            } else {
                // Safe to connect to melomind via A2DP
                try {
                    final boolean result = (boolean) a2dpProxy.getClass()
                            .getMethod(CONNECT_METHOD, BluetoothDevice.class)
                            .invoke(a2dpProxy, device);

                    if (!result) { // according to doc : "false on immediate error, true otherwise"
                        LogUtils.e(TAG, "Failed to initiate connection via A2DP!");
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
                    final long timeout = device.getBondState() == BluetoothDevice.BOND_BONDED ? 10000 : 25000;
                    final Boolean status = timerLock.waitAndGetResult(timeout);
                    if (status != null && status) {
                        LogUtils.i(TAG, "Successfully connected via A2DP to " + device.getAddress());
                        notifyConnectionStateChanged(BtState.CONNECTED, false);
                        return true;
                    } else
                        LogUtils.i(TAG, "Cannot connect to A2DP on device" + device.getAddress());
                    return false; // TODO fail to connect to melomind A2DP
                } catch (@NonNull final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    final String errorMsg = " -> " + e.getMessage();
                    if (e instanceof NoSuchMethodException)
                        LogUtils.e(TAG, "Failed to find connect method via reflexion" + errorMsg);
                    else if (e instanceof IllegalAccessException)
                        LogUtils.e(TAG, "Failed to access connect method via reflexion" + errorMsg);
                    else if (e instanceof InvocationTargetException)
                        LogUtils.e(TAG, "Failed to invoke connect method via reflexion" + errorMsg);
                    else
                        LogUtils.e(TAG, "Unable to connect to melomind with reflexion. Reason" + errorMsg);
                    Log.getStackTraceString(e);
                }
            }
        }
        return false;
    }

    /**
     * This method will attempt to disconnect to the current a2dp device via the A2DP protocol.
     * Since Android's A2DP Bluetooth class has many hidden methods we use reflexion to access them.
     * This method can handle the case where the end-user device is already connected to the melomind via
     * the A2DP protocol or to another device in which case it will disconnected (Android support only
     * one A2DP headset at the time).
     * @return <code>true</code> by default
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean disconnect() {
        if(timerLock.isWaiting()){
            LogUtils.d(TAG, "release lock in order to ease disconnection");
            timerLock.setResultAndNotify(false);
        }else {
            final BluetoothA2dp a2dpProxy;
            if (this.bluetoothAdapter != null) {
                a2dpProxy = new A2DPAccessor().getA2dp(context, this.bluetoothAdapter);
                connectedDevice = null;
                if (a2dpProxy != null && !a2dpProxy.getConnectedDevices().isEmpty())
                    connectedDevice = a2dpProxy.getConnectedDevices().get(0); //assuming that one device is connected and its obviously the melomind
                mbtBluetoothManager.requestCurrentConnectedDevice(new SimpleRequestCallback<MbtDevice>() {
                    @Override
                    public void onRequestComplete(MbtDevice object) {
                        if(new FirmwareUtils(object.getFirmwareVersion()).isFwValidForFeature(FirmwareUtils.FWFeature.A2DP_FROM_HEADSET)){
                            disconnectA2DPFromBLE();
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
        return true; //todo impossible to know here if the disconnection is really a success
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void disconnectA2DPFromBLE() {
        mbtBluetoothManager.disconnectA2DPFromBLE();
    }

    @Override
    public boolean isConnected() {
        //TODO
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private final class A2DPAccessor implements BluetoothProfile.ServiceListener {
        private final MbtLock<BluetoothA2dp> lock = new MbtLock<>();
        private A2DPMonitor a2DPMonitor = new A2DPMonitor();
        /**
         * This method blocks for 5 seconds while attempting to retrieve the instance of <code>BluetoothA2dp</code>
         * @param context   the application context
         * @param adapter   the Bluetooth Adapter to retrieve the BluetoothA2DP from
         * @return          the instance of <code>BluetoothA2dp</code> , <code>null</code> otherwise
         */
        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        @Nullable
        final BluetoothA2dp getA2dp(@NonNull final Context context, @NonNull final BluetoothAdapter adapter) {
            adapter.getProfileProxy(context, this, BluetoothProfile.A2DP);
            return this.lock.waitAndGetResult(5000);
        }

        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        final void initA2DPProxy(@NonNull final Context context, @NonNull final BluetoothAdapter adapter){
            adapter.getProfileProxy(context, this, BluetoothProfile.A2DP);
        }

        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        public final void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            Log.i(TAG, "onServiceConnected A2DP");
            if(this.lock.isWaiting())
                this.lock.setResultAndNotify((BluetoothA2dp) proxy);
            else{
                MbtBluetoothA2DP.this.a2dpProxy = (BluetoothA2dp)proxy;
                //init A2DP state
                if(a2DPMonitor != null)
                    a2DPMonitor.start(500);
            }
        }

        public final void onServiceDisconnected(final int profile) {
            if(profile == BluetoothProfile.A2DP){
                Log.e(TAG, "device is disconnected from service");
                a2DPMonitor.stop();
            }
        }
    }

    /**
     *
     */
    private class A2DPMonitor {
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
            this.pollingTimer.cancel();
            this.pollingTimer.purge();
            this.pollingTimer = null;
        }

        private class Task extends TimerTask{

            @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void run() {

                if(!connectedA2DpDevices.equals(getA2DPConnectedDevices())){
                    //It means that something has changed. Now we need to find out what changed
                    if(connectedA2DpDevices.size() < getA2DPConnectedDevices().size()){
                        //Here, we have a new A2DP connection then we notify BTManager if this new input
                        //As one a2dp output is possible at a time on android, it is possible to consider that last item in list is the current one
                        BluetoothDevice device = getA2DPConnectedDevices().get(getA2DPConnectedDevices().size()-1);
                        if(device.getName().startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) || device.getName().startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX)){
                            Intent intent = new Intent(A2DP_CONNECTED_EVENT);
                            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                            context.sendBroadcast(intent);
                        }else{
                            //TODO see if we need to perform any action in this case
                        }

                    }else{
                        //Here, either the A2DP connection has dropped or a new A2DP device is connecting.
                        //Intent intent = new Intent(BTManager.A2DP_DISCONNECTED_EVENT);
                        Intent intent = new Intent(A2DP_DISCONNECTED_EVENT);
                        context.sendBroadcast(intent);
                    }
                    //In any case, it is mandatory to updated our local connected A2DP list
                    connectedA2DpDevices = getA2DPConnectedDevices();
                }
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private boolean hasA2DPDeviceConnected(){
        // First we retrieve the Audio Manager that will help monitor the A2DP status
        // and then the instance of Bluetooth A2DP to make the necessaries calls
        final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        if(audioManager == null)
            return false;
        // First we check if the end-user device is currently connected to an A2DP device
        return audioManager.isBluetoothA2dpOn();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private List<BluetoothDevice> getA2DPConnectedDevices(){
        if(a2dpProxy == null)
            return Collections.emptyList();

        return a2dpProxy.getConnectedDevices();
    }

}
