package core.bluetooth;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import core.MbtManager;
import core.bluetooth.lowenergy.MbtBluetoothLE;

import static org.junit.Assert.*;

public class MbtBluetoothManagerTest {

    private Context context;
    MbtBluetoothManager bluetoothManager;
    MbtManager mbtManager;
    @Before
    public void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        mbtManager = new MbtManager(context);
        bluetoothManager = new MbtBluetoothManager(context, mbtManager);
    }

    @Test
    public void reconnectIfAudioConnected() {
    }

    @Test
    public void resetBackgroundReconnectionRetryCounter() {
    }

    @Test
    public void registerReceiverIntents() {
    }

    @Test
    public void scanAndConnect() {
        //bluetoothManager.scanAndConnect("melo_01010101",true);
    }

    @Test
    public void isBluetoothEnabled() {
    }

    @Test
    public void isLocationEnabledAndGranted() {
    }

    @Test
    public void isAlreadyConnected() {
    }

    @Test
    public void scan() {
    }

    @Test
    public void bondAndConnectAudioIfRequested() {
    }

    @Test
    public void connect() {
    }

    @Test
    public void scanDevices() {
    }

    @Test
    public void scanSingle() {
    }

    @Test
    public void stopCurrentScan() {
    }

    @Test
    public void configureHeadset() {
    }

    @Test
    public void startStream() {
    }

    @Test
    public void stopStream() {
    }

    @Test
    public void readBattery() {
    }

    @Test
    public void readFwVersion() {
    }

    @Test
    public void readHwVersion() {
    }

    @Test
    public void readSerialNumber() {
    }

    @Test
    public void readModelNumber() {
    }

    @Test
    public void disconnect() {
    }

    @Test
    public void cancelPendingConnection() {
    }

    @Test
    public void handleDataAcquired() {
    }

    @Test
    public void deinit() {
    }

    @Test
    public void onNewBluetoothRequest() {
    }

    @Test
    public void getConnectionError() {
    }

    @Test
    public void notifyDeviceInfoReceived() {
    }

    @Test
    public void notifyStreamStateChanged() {
    }

    @Test
    public void notifyNewHeadsetStatus() {
    }

    @Test
    public void isConnected() {
    }

    @Test
    public void notifyStateChanged() {
    }

    @Test
    public void connectBLEFromA2DP() {
    }

    @Test
    public void requestCurrentConnectedDevice() {
    }

    @Test
    public void connectA2DPFromBLE() {
    }

    @Test
    public void disconnectA2DPFromBLE() {
    }

    @Test
    public void isAlreadyConnectedToRequestedDevice() {
    }

    private class MbtManagerWrapper extends MbtManager{

        /**
         * @param context
         */
        public MbtManagerWrapper(Context context) {
            super(context);
        }
    }
}