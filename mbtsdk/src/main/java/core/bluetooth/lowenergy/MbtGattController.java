package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import core.bluetooth.BtState;
import core.recordingsession.metadata.DeviceInfo;
import utils.MbtLock;

import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_HEADSET_STATUS;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_BATTERY_LEVEL;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_EEG;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER;
import static core.bluetooth.lowenergy.MelomindCharacteristics.SERVICE_DEVICE_INFOS;
import static core.bluetooth.lowenergy.MelomindCharacteristics.SERVICE_MEASUREMENT;


/**
 * A custom Gatt controller that extends {@link BluetoothGattCallback} class.
 * All gatt operations from {@link MbtBluetoothLE} LE controller are completed here.
 *
 * @see BluetoothGattCallback
 */
final class MbtGattController extends BluetoothGattCallback {
    private final static String TAG = MbtGattController.class.getSimpleName();

    @Nullable
    private BluetoothGattService mainService = null;
    @Nullable
    private BluetoothGattService deviceInfoService = null;
    @Nullable
    private BluetoothGattCharacteristic measurement = null ;
    @Nullable
    private BluetoothGattCharacteristic headsetStatus = null ;
    @Nullable
    private BluetoothGattCharacteristic mailBox = null ;
    @Nullable
    private BluetoothGattCharacteristic oadPacketsCharac = null;
    @Nullable
    private BluetoothGattCharacteristic battery = null ;
    @Nullable
    private BluetoothGattCharacteristic fwVersion = null;
    @Nullable
    private BluetoothGattCharacteristic hwVersion = null;
    @Nullable
    private BluetoothGattCharacteristic serialNumber = null;

    private final MbtBluetoothLE bluetoothController;

    public MbtLock<Boolean> notificationLock;
    private final MbtLock<BtState> connectionLock = new MbtLock<>();
    private final MbtLock<Byte[]> eegConfigRetrievalLock = new MbtLock<>();
    private final MbtLock<String> readDeviceInfoLock = new MbtLock<>();
    private final MbtLock<Boolean> enableMailboxNotificationLock = new MbtLock<>();

    private boolean mailboxNotificationsEnabled = false;

    // Generic Descriptor UUID for Notification System
    private final UUID notificationDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public MbtGattController(Context context, MbtBluetoothLE bluetoothController) {
        super();
        this.bluetoothController = bluetoothController;
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyRead(gatt, txPhy, rxPhy, status);
    }

    @Override
    public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String msg = "Connection state change : ";
        switch(newState) {
            case BluetoothGatt.STATE_CONNECTED:
                //gatt.requestMtu(MAX_MTU);
                gatt.discoverServices();
                this.bluetoothController.notifyConnectionStateChanged(BtState.CONNECTED);
                msg += "STATE_CONNECTED and now discovering services...";
                break;
            case BluetoothGatt.STATE_CONNECTING:
                this.bluetoothController.notifyConnectionStateChanged(BtState.CONNECTING);
                msg += "STATE_CONNECTING";
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                // This if is necessary because we might have disconnect after something went wrong while connecting
                if (this.connectionLock.isWaiting())
                    this.connectionLock.setResultAndNotify(BtState.CONNECT_FAILURE);
                else {                    // in this case the connection went well for a while, but just got lost
                    gatt.close();
                    //this.gatt = null;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.bluetoothController.notifyConnectionStateChanged(BtState.DISCONNECTED);
                }
                msg += "STATE_DISCONNECTED";
                break;
            case BluetoothGatt.STATE_DISCONNECTING:
                msg += "STATE_DISCONNECTING";
                this.bluetoothController.notifyConnectionStateChanged(BtState.DISCONNECTING);
                break;
            default:
                this.bluetoothController.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE);
                gatt.close();
                msg += "Unknown value " + newState;
        }
        Log.d("", msg);
    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        // Checking if services were indeed discovered or not
        if (gatt.getServices() == null || gatt.getServices().isEmpty()) {
            gatt.disconnect();
            return;
        }

        // Logging all available services
        for (final BluetoothGattService service : gatt.getServices()) {
            Log.i(TAG, "Found Service with UUID -> " + service.getUuid().toString());
        }
        //TODO split function in two parts: first is input checking and second is characteristics initialization

        // Retrieving main service
        this.mainService = gatt.getService(SERVICE_MEASUREMENT);
        this.deviceInfoService = gatt.getService(SERVICE_DEVICE_INFOS);
        if (this.mainService != null) {
            // Retrieving all relevant characteristics
            this.measurement = this.mainService.getCharacteristic(CHARAC_MEASUREMENT_EEG);
            this.battery = this.mainService.getCharacteristic(CHARAC_MEASUREMENT_BATTERY_LEVEL);
            this.mailBox = this.mainService.getCharacteristic(CHARAC_MEASUREMENT_MAILBOX);
            this.oadPacketsCharac = this.mainService.getCharacteristic(CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER);
            this.headsetStatus = this.mainService.getCharacteristic(CHARAC_HEADSET_STATUS);
            //write no response requested in oad fw specification
            this.oadPacketsCharac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }

        if (this.deviceInfoService != null) {
            // Retrieving all relevant characteristics
            this.fwVersion = this.deviceInfoService.getCharacteristic(CHARAC_INFO_FIRMWARE_VERSION);
            this.hwVersion = this.deviceInfoService.getCharacteristic(CHARAC_INFO_HARDWARE_VERSION);
            this.serialNumber = this.deviceInfoService.getCharacteristic(CHARAC_INFO_SERIAL_NUMBER);
        }

        // In case one of these is null, we disconnect because something went wrong
        if (this.mainService == null || this.measurement == null || this.battery == null || this.deviceInfoService == null
                || this.fwVersion == null || this.hwVersion == null || this.serialNumber == null || this.oadPacketsCharac == null || this.mailBox == null || this.headsetStatus == null){
            Log.e(TAG, "error, not all characteristics have been found");
            gatt.disconnect();
        } else{
            // everything went well as expected
            if(this.connectionLock.isWaiting())
                this.connectionLock.setResultAndNotify(BtState.CONNECTED_AND_READY);
            this.bluetoothController.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
        }

        // Starting Battery Reader Timer
        //startOrStopBatteryReader(true);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, new String(characteristic.getValue()));
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, new String(characteristic.getValue()));
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0) {
            Log.i(TAG, "received " + new String(characteristic.getValue()));
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String(characteristic.getValue()));
        }


        if (characteristic.getUuid().compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
            if (characteristic.getValue().length < 4) {
                final StringBuffer sb = new StringBuffer();
                for (final byte value : characteristic.getValue()) {
                    sb.append(value);
                    sb.append(';');
                }
                Log.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                        "but the payload of the characteristic is invalid ! \nValue(s) received -> " + sb.toString());
                return;
            }

            final short level;
            switch (characteristic.getValue()[0]) {
                case (byte) 0:
                    level = 0;
                    break;
                case (byte) 1:
                    level = 15;
                    break;
                case (byte) 2:
                    level = 30;
                    break;
                case (byte) 3:
                    level = 50;
                    break;
                case (byte) 4:
                    level = 65;
                    break;
                case (byte) 5:
                    level = 85;
                    break;
                case (byte) 6:
                    level = 100;
                    break;
                case (byte) 0xFF:
                    level = 999;
                    break;
                default:
                    level = -1;
                    break;
            }
            if (level == -1) {
                Log.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                        "but the returned value could not be decoded ! " +
                        "Byte value received -> " + characteristic.getValue()[3]);
            }
            bluetoothController.notifyBatteryReceived(level);

            // Log.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
            //         "Value -> " + level);
        }

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if(status !=0 ){
            Log.e(TAG, "error writing characteristic nb : " + status);
        }else{
            if(characteristic.getUuid().compareTo(this.oadPacketsCharac.getUuid()) == 0){
                /*if(oadFileManager.getmProgInfo().iBlocks == oadFileManager.getmProgInfo().nBlocks){ //TODO
                    if(oadPacketTransferTimeoutLock.isWaiting()){
                        oadPacketTransferTimeoutLock.setResultAndNotify(true);
                    }else{
                        Log.e(TAG, "error, packet transfer timeout for end not ready");
                    }

                    notifyOADEvent(OADEvent.CRC_COMPUTING, oadFileManager.getmProgInfo().nBlocks);
                }else{
                    if(oadPacketTransferTimeoutLock.isWaiting() || oadFileManager.getmProgInfo().iBlocks <=1){
                        sendOADBlock(oadFileManager.getmProgInfo().iBlocks);
                    }else{
                        Log.e(TAG, " error, lock isn't waiting yet");
                    }

                }*/

            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG) == 0){
            this.bluetoothController.notifyNewDataAcquired(characteristic.getValue());
        }else if(characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_HEADSET_STATUS) == 0){
            this.bluetoothController.notifyNewHeadsetStatus(characteristic.getValue());
        }else if(characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX) == 0){
            Log.i(TAG, "mailbox message received with code " + characteristic.getValue()[0] +
                " and payload " + Arrays.toString(characteristic.getValue()));
            synchronized (this.bluetoothController){
                this.bluetoothController.notify();
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        // Check for EEG Notification status
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "Received a [onDescriptorWrite] callback with status SUCCESS");
        } else {
            Log.e(TAG, "Received a [onDescriptorWrite] callback with Status: FAILURE.");
        }
        synchronized (this.bluetoothController){
            bluetoothController.notify();
            bluetoothController.onNotificationStateChanged(status == BluetoothGatt.GATT_SUCCESS, descriptor.getCharacteristic(), descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        Log.i(TAG, "onMtuChanged with new value " + mtu);
        synchronized (this.bluetoothController){
            this.bluetoothController.notify();
        }

    }


    public void testAcquireDataZeros(){
        byte[] data = new byte[250];
        Arrays.fill(data,(byte) 0);
        this.bluetoothController.notifyNewDataAcquired(data);
    }

    public void testAcquireDataOnes(){
        byte[] data = new byte[250];
        Arrays.fill(data,(byte) 1);
        this.bluetoothController.notifyNewDataAcquired(data);
    }

    public void testAcquireDataRandomByte(){
        byte[] data = new byte[250];
        new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
        this.bluetoothController.notifyNewDataAcquired(data);
    }
}