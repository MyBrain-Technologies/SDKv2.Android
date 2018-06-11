package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import core.bluetooth.BtState;
import core.bluetooth.MbtBluetooth;
import core.recordingsession.metadata.DeviceInfo;
import utils.MbtLock;

import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_HEADSET_STATUS;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_SERIAL_NUMBER;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_BATTERY_LEVEL;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_BRAIN_ACTIVITY;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_MEASUREMENT_OAD_PACKETS_TRANSFER;
import static core.bluetooth.lowenergy.MelomindCharacteristics.SERVICE_DEVICE_INFOS;
import static core.bluetooth.lowenergy.MelomindCharacteristics.SERVICE_MEASUREMENT;

final class MbtGattController extends BluetoothGattCallback {
    private final static String TAG = MbtGattController.class.getSimpleName();

    private BluetoothGatt gatt = null;
    private BluetoothGattService mainService = null;
    private BluetoothGattService deviceInfoService = null;
    private BluetoothGattCharacteristic measurement = null ;
    private BluetoothGattCharacteristic headsetStatus = null ;
    private BluetoothGattCharacteristic mailBox = null ;
    private BluetoothGattCharacteristic oadPacketsCharac = null;
    private BluetoothGattCharacteristic battery = null ;
    private BluetoothGattCharacteristic fwVersion = null;
    private BluetoothGattCharacteristic hwVersion = null;
    private BluetoothGattCharacteristic serialNumber = null;

    private MbtBluetooth bluetoothController;

    public MbtLock<Boolean> notificationLock;
    private final MbtLock<BtState> connectionLock = new MbtLock<>();
    private final MbtLock<Byte[]> eegConfigRetrievalLock = new MbtLock<>();
    private final MbtLock<Short> batteryLock = new MbtLock<>();
    private final MbtLock<String> readDeviceInfoLock = new MbtLock<>();
    private final MbtLock<Boolean> enableMailboxNotificationLock = new MbtLock<>();

    // A handler to process code in the Main UI Thread
    private final Handler uiAccess;

    private boolean mailboxNotificationsEnabled = false;

    // Generic Descriptor UUID for Notification System
    private final UUID notificationDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public MbtGattController(Context context, MbtBluetooth bluetoothController) {
        super();
        this.bluetoothController = bluetoothController;
        this.uiAccess = new Handler(context.getMainLooper());
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
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String msg = "Connection state change : ";
        switch(newState) {
            case BluetoothGatt.STATE_CONNECTED:
                //gatt.requestMtu(MAX_MTU);
                gatt.discoverServices();
                this.bluetoothController.notifyStateChanged(BtState.CONNECTED);
                msg += "STATE_CONNECTED and now discovering services...";
                break;
            case BluetoothGatt.STATE_CONNECTING:
                this.bluetoothController.notifyStateChanged(BtState.CONNECTING);
                msg += "STATE_CONNECTING";
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                // This if is necessary because we might have disconnect after something went wrong while connecting
                if (this.connectionLock.isWaiting())
                    this.connectionLock.setResultAndNotify(BtState.CONNECT_FAILURE);
                else {                    // in this case the connection went well for a while, but just got lost
                    this.gatt.close();
                    this.gatt = null;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.bluetoothController.notifyStateChanged(BtState.DISCONNECTED);
                }
                msg += "STATE_DISCONNECTED";
                break;
            case BluetoothGatt.STATE_DISCONNECTING:
                msg += "STATE_DISCONNECTING";
                this.bluetoothController.notifyStateChanged(BtState.DISCONNECTING);
                break;
            default:
                this.bluetoothController.notifyStateChanged(BtState.INTERNAL_FAILURE);
                this.gatt.close();
                this.gatt = null;
                msg += "Unknown value " + newState;
        }
        Log.d("", msg);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
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
            this.measurement = this.mainService.getCharacteristic(CHARAC_MEASUREMENT_BRAIN_ACTIVITY);
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
            this.bluetoothController.notifyStateChanged(BtState.CONNECTED_AND_READY);
        }

        // Starting Battery Reader Timer
        //startOrStopBatteryReader(true);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0) {
            bluetoothController.getMelomindDevice().setFirmwareVersion(new String(characteristic.getValue())) ;
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0) {
            bluetoothController.getMelomindDevice().setHardwareVersion(new String(characteristic.getValue()));
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0) {
            bluetoothController.getMelomindDevice().setSerialNumber(new String(characteristic.getValue()));
        }

        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0) {
            if(this.readDeviceInfoLock.isWaiting())
                this.readDeviceInfoLock.setResultAndNotify(new String(characteristic.getValue()));
        }

        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0) {
            if(this.readDeviceInfoLock.isWaiting())
                this.readDeviceInfoLock.setResultAndNotify(new String(characteristic.getValue()));
        }

        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0) {
            if(this.readDeviceInfoLock.isWaiting())
                Log.i(TAG, "received " + new String(characteristic.getValue()));
            this.readDeviceInfoLock.setResultAndNotify(new String(characteristic.getValue()));
        }

        if (characteristic.getUuid().compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
            if (this.batteryLock.isWaiting()) {
                if (characteristic.getValue().length < 4) {
                    final StringBuffer sb = new StringBuffer();
                    for (final byte value : characteristic.getValue()) {
                        sb.append(value);
                        sb.append(';');
                    }
                    Log.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                            "but the payload of the characteristic is invalid ! \nValue(s) received -> " + sb.toString());
                    this.batteryLock.setResultAndNotify(null);
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
                    this.batteryLock.setResultAndNotify(null);
                    return;
                }

                // Log.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
                //         "Value -> " + level);
                this.batteryLock.setResultAndNotify(level);
            } else
                Log.e(TAG, "Received a [onCharacteristicRead] callback for battery level request " +
                        "but apparently no such request was made ! Did the lock expired ?");
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
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
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (characteristic.getUuid().compareTo(this.measurement.getUuid()) == 0){
            this.bluetoothController.acquireData(characteristic.getValue());
        }//TODO else
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        // Check for EEG Notification status
        if (this.notificationLock.isWaiting()){
            if (descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
                Log.i(TAG, "Received a [onDescriptorWrite] callback for status on a request to remotely " +
                        "enable notification for EEG.\nStatus: notification for EEG now ENABLED.");
                this.notificationLock.setResultAndNotify(Boolean.TRUE);
            } else {
                Log.e(TAG, "Received a [onDescriptorWrite] callback for status on a request to remotely " +
                        "enable notification for EEG.\nStatus: FAILURE.");
                this.notificationLock.setResultAndNotify(Boolean.FALSE);
            }
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
    }

    /**
     * Initiates a read battery operation on this correct BtProtocol
     */
    public void readBattery(int previousBatteryLevel) {

        if (!this.gatt.readCharacteristic(this.battery)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current battery value from remote device");
            return;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current battery value from remote device");

        final Short level = this.batteryLock.waitAndGetResult(1000);
        if (level == null) {
            Log.e(TAG, "Error: failed to fetch battery level within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return;
        }

        Log.i(TAG, "Successfully retrieved battery value from remote device within allotted" +
                " time of 1 second. Battery level is now -> " + level);

        // We only notify if battery level has indeed changed from last time
        if (level != previousBatteryLevel) {
            uiAccess.post(new Runnable() {
                public final void run() {
                    bluetoothController.getMbtBluetoothManager().updateBatteryLevel(level); //update value of current battery level
                }

            });
        }
    }

    /**
     * Initiates a read firmware version operation on this correct BtProtocol
     */
    public void readFwVersion(){
        if (!this.gatt.readCharacteristic(this.fwVersion)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current fwVersion value from remote device");
            return;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current fwVersion value from remote device");

        final String fwVersion = this.readDeviceInfoLock.waitAndGetResult(5000);
        if (fwVersion == null) {
            Log.e(TAG, "Error: failed to fetch fwVersion value within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return;
        }

        Log.i(TAG, "Successfully retrieved fwVersion value from remote device within allocated");

        // We only notify if battery level has indeed changed from last time
        //bluetoothController.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, fwVersion);
        this.bluetoothController.deviceInfoReceived(DeviceInfo.FW_VERSION, fwVersion);
    }
    /**
     * Initiates a read hardware version operation on this correct BtProtocol
     */
    public void readHwVersion(){
        if (!this.gatt.readCharacteristic(this.hwVersion)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current hwVersion value from remote device");
            return;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current hwVersion value from remote device");

        final String hwVersion = this.readDeviceInfoLock.waitAndGetResult(5000);
        if (hwVersion == null) {
            Log.e(TAG, "Error: failed to fetch hwVersion value within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return;
        }

        Log.i(TAG, "Successfully retrieved hwVersion value from remote device within allocated");

        // We only notify if battery level has indeed changed from last time
        this.bluetoothController.deviceInfoReceived(DeviceInfo.HW_VERSION, hwVersion);
        //bluetoothController.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, hwVersion);
    }

    /**
     * Initiates a read serial number operation on this correct BtProtocol
     */
    public void readSerialNumber(){
    if (!this.gatt.readCharacteristic(this.serialNumber)) {
            Log.e(TAG, "Error: failed to initiate read characteristic operation in order " +
                    "to retrieve the current serial number from remote device");
            return;
        }

        Log.i(TAG, "Successfully initiated read characteristic operation in order " +
                "to retrieve the current serial number from remote device");

        final String serialNumber = this.readDeviceInfoLock.waitAndGetResult(5000);
        if (serialNumber == null) {
            Log.e(TAG, "Error: failed to fetch serial number within allotted time of 1 second " +
                    "or fetched value was invalid !");
            return;
        }

        Log.i(TAG, "Successfully retrieved serial number from remote device within allocated");

        // We only notify if battery level has indeed changed from last time
        //bluetoothController.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, serialNumber);
        this.bluetoothController.deviceInfoReceived(DeviceInfo.SERIAL_NUMBER,serialNumber);
    }

    public Byte[] getEegConfig() {

        byte[] code = {MailboxEvents.MBX_GET_EEG_CONFIG};

        //Send buffer
        this.mailBox.setValue(code);
        if (!this.gatt.writeCharacteristic(this.mailBox)) {
            Log.e(TAG, "Error: failed to initiate write characteristic operation in order " +
                    "to send the MBX_GET_EEG_CONFIG command");
            return null;
        }

        return this.eegConfigRetrievalLock.waitAndGetResult(3000);
    }
    /**
     * Enables the notification for mailbox data.
     * <p><strong>Note:</strong> calling this method will enable mailbox communications between bluetooth device and application
     * @return  <code>true</code> if the notification has been successfully established within the 2 seconds of allotted time,
     *          <code>false otherwise</code>
     */
    public synchronized boolean enableNotificationOnMailbox() {
        if (this.mailBox == null){
            Log.e(TAG, "Error, mailbox is null");
            return false;
        }

//                throw new IllegalStateException("Error: impossible to enable notification on mailbox " +
//                        "because the Gatt Controller is not correctly initialized");

        Log.i(TAG, "Request received to enable notification for mailbox.");
        if(this.gatt == null){
            Log.e(TAG, "Error, gatt is null");
            return false;
        }

        if(mailboxNotificationsEnabled){
            Log.w(TAG, "notifications already enabled");
            return true;
        }

//            Log.i(TAG, "Now setting connection priority to HIGH for better performance before " +
//                    "enabling notification remotely");
//            if (!this.gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
//                Log.e(TAG, "Error: failed to set connection priority to HIGH for better performance!");
//                return false;
//            }
//            Log.i(TAG, "Successfully set connection priority to HIGH for better performance.");
//            Log.i(TAG, "It is now safe to enable notification remotely, which will start " +
//                    "the EEG acquisition on the headset");


        Log.i(TAG, "Now enabling local notification for mailbox...");
        if (!this.gatt.setCharacteristicNotification(this.mailBox, true)) {
            Log.e(TAG, "Failed to enable local notification for mailbox!");
            return false;
        }

        final BluetoothGattDescriptor notificationDescriptor =
                this.mailBox.getDescriptor(this.notificationDescriptorUUID);
        if (notificationDescriptor == null) {
            Log.e(TAG, String.format("Error: mailbox characteristic with " +
                            "UUID <%s> does not have a descriptor (UUID <%s>) to enable notification remotely!",
                    this.mailBox.getUuid().toString(), notificationDescriptorUUID.toString()));
            return false;
        }

        Log.i(TAG, "Now enabling remote notification for mailbox...");
        if (!notificationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            final StringBuilder sb = new StringBuilder();
            for (final byte value : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
                sb.append(value);
                sb.append(';');
            }
            Log.e(TAG, String.format("Error: measurement characteristic's notification descriptor with " +
                            "UUID <%s> could not store the ENABLE notification value <%s>.",
                    notificationDescriptorUUID.toString(), sb.toString()));
            return false;
        }

        if (!this.gatt.writeDescriptor(notificationDescriptor)) {
            Log.e(TAG, "Error: failed to initiate write descriptor operation in order to remotely " +
                    "enable notification on mailbox!");
            return false;
        }

        Log.i(TAG, "Successfully initiated write descriptor operation in order to remotely " +
                "enable notification for mailbox: now waiting for confirmation from headset.");

        final Boolean result = this.enableMailboxNotificationLock.waitAndGetResult(10000);
        if (result == null)
            Log.e(TAG, "Error: waiting for confirmation from headset to have notification " +
                    "ENABLED for MAILBOX has expired or the write descriptor operation has failed");
        else if (result != null && result) {
            Log.i(TAG, "Notification for Mailbox are now finally enabled. ");
            mailboxNotificationsEnabled = true;
            return true;
        }
        return false;
    }

}