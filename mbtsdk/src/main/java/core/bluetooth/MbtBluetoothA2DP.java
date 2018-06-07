package core.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import utils.MbtLock;

/**
 * Created by Etienne on 08/02/2018.
 */

public final class MbtBluetoothA2DP extends MbtBluetooth{
    private final static String TAG = MbtBluetoothA2DP.class.getSimpleName();

    private final MbtLock<Boolean> timerLock = new MbtLock<>();

    public MbtBluetoothA2DP(Context context, MbtBluetoothManager mbtBluetoothManager) {
        super(context, mbtBluetoothManager);
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
    @Override
    public boolean connect(Context context, BluetoothDevice device) {
        // Hack to try to show pairing popup to front
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
        //Give it some time before cancelling the discovery
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        Log.i(TAG, "Attempting to connect A2DP to " + device.getName());

        // First we retrieve the Audio Manager that will help monitor the A2DP status
        // and then the instance of Bluetooth A2DP to make the necessaries calls
        final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        final BluetoothA2dp a2dpProxy = new A2DPAccessor().getA2dp(context, this.bluetoothAdapter);

        if (a2dpProxy == null) { // failed to retrieve instance of Bluetooth A2DP within alloted time (5 sec)
            Log.i(TAG, "Failed to retrieve instance of Bluetooth A2DP. Cannot perform A2DP operations");
            return false;
        }

        // First we check if the end-user device is currently connected to an A2DP device
        if (audioManager != null) {
            if (audioManager.isBluetoothA2dpOn()) {
                // End-user is indeed connected to an A2DP device. We retrieve it to see if it is the melomind
                Log.i(TAG, "User device is currently connected to an A2DP Headset. Checking if it is the melomind");

                if (a2dpProxy.getConnectedDevices() == null || a2dpProxy.getConnectedDevices().isEmpty())
                    connect(context,device); // Somehow end-user is no longer connected (should not happen)

                // we assume there is only one, because Android can only support one at the time
                final BluetoothDevice connected = a2dpProxy.getConnectedDevices().get(0);
                if (connected.getAddress().equals(device.getAddress())) { // already connected to the Melomind !
                    Log.i(TAG, "Already connected to the melomind.");
                    //notifyA2DPStateChanged(BTState.CONNECTED);
                    return true;
                } else {
                    // The user device is currently connected to a headset that is not the melomind
                    // so we disconnect it now and then we connect it to the melomind
                    try {
                        final boolean result = (boolean) a2dpProxy.getClass().getMethod("disconnect", BluetoothDevice.class)
                                .invoke(a2dpProxy, connected);

                        if (!result) { // according to doc : "false on immediate error, true otherwise"
                            return false;
                        }
                        // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                        new Timer(true).scheduleAtFixedRate(new TimerTask() {
                            public final void run() {
                                if (!audioManager.isBluetoothA2dpOn()) { // the user device is no longer to connected to the wrong headset
                                    cancel();
                                    timerLock.setResultAndNotify(true);
                                }
                            }
                        }, 100, 500);

                        final Boolean status = timerLock.waitAndGetResult(5000);
                        if (status != null && status) {
                            Log.i(TAG, "successfully disconnected from A2DP device -> " + connected.getName());
                            Log.i(TAG, "Now connecting A2DP to " + device.getAddress());
                            return connect(context,device);
                        } else {
                            Log.e(TAG, "failed to connect with A2DP! Lock has expired...");
                            // TODO fail to disconnect from wrong headset (should not happen)
                            return false;
                        }
                    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        final String errorMsg = " -> " + e.getMessage();
                        if (e instanceof NoSuchMethodException)
                            Log.e(TAG, "Failed to find disconnect method via reflexion" + errorMsg);
                        else if (e instanceof IllegalAccessException)
                            Log.e(TAG, "Failed to access disconnect method via reflexion" + errorMsg);
                        else if (e instanceof InvocationTargetException)
                            Log.e(TAG, "Failed to invoke disconnect method via reflexion" + errorMsg);
                        else
                            Log.e(TAG, "Unable to disconnect to current headset with reflexion. Reason" + errorMsg);
                        Log.getStackTraceString(e);
                    }
                }
            } else {
                // Safe to connect to melomind via A2DP
                try {
                    final boolean result = (boolean) a2dpProxy.getClass()
                            .getMethod("connect", BluetoothDevice.class)
                            .invoke(a2dpProxy, device);

                    if (!result) { // according to doc : "false on immediate error, true otherwise"
                        Log.e(TAG, "Failed to initiate connection via A2DP!");
                        return false;
                    }

                    final MbtLock<Boolean> timerLock = new MbtLock<>();

                    // Since the disconnecting process is asynchronous we use a timer to monitor the status for a short while
                    new Timer(true).scheduleAtFixedRate(new TimerTask() {
                        public final void run() {
                            if (audioManager.isBluetoothA2dpOn()) {
                                cancel();
                                timerLock.setResultAndNotify(true);
                            }
                        }
                    }, 100, 1500);

                    // we give 20 seconds to the user to accepting bonding request
                    final long timeout = device.getBondState() == BluetoothDevice.BOND_BONDED ? 10000 : 25000;
                    final Boolean status = timerLock.waitAndGetResult(timeout);
                    if (status != null && status) {
                        Log.i(TAG, "Successfully connected via A2DP to " + device.getAddress());
                        //notifyA2DPStateChanged(BTState.CONNECTED);
                        return true;
                    } else
                        Log.i(TAG, "Cannot connect to A2DP on device" + device.getAddress());
                    return false; // TODO fail to connect to melomind A2DP
                } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    final String errorMsg = " -> " + e.getMessage();
                    if (e instanceof NoSuchMethodException)
                        Log.e(TAG, "Failed to find connect method via reflexion" + errorMsg);
                    else if (e instanceof IllegalAccessException)
                        Log.e(TAG, "Failed to access connect method via reflexion" + errorMsg);
                    else if (e instanceof InvocationTargetException)
                        Log.e(TAG, "Failed to invoke connect method via reflexion" + errorMsg);
                    else
                        Log.e(TAG, "Unable to connect to melomind with reflexion. Reason" + errorMsg);
                    Log.getStackTraceString(e);
                }
            }
        }
        return false;
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {

        boolean result = false;
        if(timerLock.isWaiting()){
            Log.d(TAG, "release lock in order to ease disconnection");
            timerLock.setResultAndNotify(false);
        }else {
            final BluetoothA2dp a2dpProxy = new A2DPAccessor().getA2dp(context, this.bluetoothAdapter);

            BluetoothDevice connected = null;
            if (a2dpProxy != null){

                if ( !a2dpProxy.getConnectedDevices().isEmpty())
                    connected = a2dpProxy.getConnectedDevices().get(0); //assuming that one device is connected and its obviously the melomind
                try {
                    result = (boolean) a2dpProxy.getClass().getMethod("disconnect", BluetoothDevice.class)
                                .invoke(a2dpProxy, connected);

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    @Override
    public boolean isConnected() {
        //TODO
        return false;
    }


    private final static class A2DPAccessor implements BluetoothProfile.ServiceListener {
        private final MbtLock<BluetoothA2dp> lock = new MbtLock<>();

        /**
         * This method blocks for 5 seconds while attempting to retrieve the instance of <code>BluetoothA2dp</code>
         * @param context   the application context
         * @param adapter   the Bluetooth Adapter to retrieve the BluetoothA2DP from
         * @return          the instance of <code>BluetoothA2dp</code> , <code>null</code> otherwise
         */
        @Nullable
        public final BluetoothA2dp getA2dp(@NonNull final Context context, @NonNull final BluetoothAdapter adapter) {
            adapter.getProfileProxy(context, this, BluetoothProfile.A2DP);
            return this.lock.waitAndGetResult(5000);
        }

        public final void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            this.lock.setResultAndNotify((BluetoothA2dp) proxy);
        }

        public final void onServiceDisconnected(final int profile) {}
    }
}
