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


import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import config.AmpGainConfig;
import config.MbtConfig;
import core.bluetooth.BtState;
import core.device.MbtDeviceManager;
import core.device.model.DeviceInfo;
import core.eeg.acquisition.MbtDataConversion;
import utils.AsyncUtils;
import utils.LogUtils;
import utils.MbtLockNew;

import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_HEADSET_STATUS;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_FIRMWARE_VERSION;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_HARDWARE_VERSION;
import static core.bluetooth.lowenergy.MelomindCharacteristics.CHARAC_INFO_MODEL_NUMBER;
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
    private final static String REFRESH_METHOD = "refresh";


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
    @Nullable
    private BluetoothGattCharacteristic modelNumber = null;

    private final MbtBluetoothLE bluetoothController;

    private MbtLockNew<String> writeExternalNameLock = new MbtLockNew<>();


    MbtGattController(Context context, MbtBluetoothLE bluetoothController) {
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


    /**
     * Callback indicating when GATT client has connected/disconnected to/from the headset
     */
    @Override
    public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String msg = "Connection state change : "+ newState;
        switch(newState) {
            case BluetoothGatt.STATE_CONNECTING:
                this.bluetoothController.updateConnectionState();//current state is set to CONNECTING
            case BluetoothGatt.STATE_CONNECTED:
                if(bluetoothController.currentState.equals(BtState.CONNECTING))
                    this.bluetoothController.updateConnectionState();//current state is set to CONNECTION_SUCCES
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                // This if is necessary because we might have disconnect after something went wrong while connecting
                // in this case the connection went well for a while, but just got lost
                refreshDeviceCache(gatt);
                gatt.close();
                // in this case the connection went well for a while, but just got lost
                   if (bluetoothController.isConnected() && !bluetoothController.isDownloadingFirmware()) //{
                        gatt.disconnect();
//                    } else {
//                        if (oadPacketTransferTimeoutLock != null && oadPacketTransferTimeoutLock.isWaiting())
//                            oadPacketTransferTimeoutLock.setResultAndNotify(false);
//                        else
//                            bluetoothController.unpairDevice(gatt.getDevice()); // disconnection due to device reboot in order to install the update
//
//                    }

                this.bluetoothController.notifyConnectionStateChanged( bluetoothController.currentState.equals(BtState.DEVICE_FOUND) || bluetoothController.currentState.equals(BtState.CONNECTING) ?
                        BtState.CONNECTION_FAILURE :  BtState.DISCONNECTED);

                if(status != BluetoothGatt.GATT_SUCCESS && !bluetoothController.isDownloadingFirmware()) {
                    //bluetoothController.connectBLEFromAudio(gatt.getDevice().getName());
                }
                break;
            case BluetoothGatt.STATE_DISCONNECTING:
                this.bluetoothController.notifyConnectionStateChanged(BtState.DISCONNECTING);
                break;
            default:
                this.bluetoothController.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE);
                gatt.close();
                msg += "Unknown value " + newState;
        }
        LogUtils.d("", msg);
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            Method localMethod = gatt.getClass().getMethod(REFRESH_METHOD);
            if (localMethod != null) {
                return (boolean) (Boolean) localMethod.invoke(gatt);
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    /**
     * Callback invoked when the list of remote services, characteristics and descriptors
     * for the remote device have been updated, ie new services have been discovered.
     * @param gatt
     * @param status
     */
    @Override
    public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
        Log.i(TAG, "services discovered ");
        super.onServicesDiscovered(gatt, status);

        // Checking if services were indeed discovered or not : getServices should be not null and contains values at this point
        if (gatt.getServices() == null || gatt.getServices().isEmpty() || status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect();
            if(bluetoothController.currentState.equals(BtState.DISCOVERING_SERVICES))
                this.bluetoothController.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE);
            return;
        }

        // Logging all available services
        for (final BluetoothGattService service : gatt.getServices()) {
            LogUtils.i(TAG, "Found Service with UUID -> " + service.getUuid().toString());
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
            this.modelNumber = this.deviceInfoService.getCharacteristic(CHARAC_INFO_MODEL_NUMBER);
        }

        // In case one of these is null, we disconnect because something went wrong
        if (this.mainService == null || this.measurement == null || this.battery == null || this.deviceInfoService == null
                || this.fwVersion == null || this.hwVersion == null || this.serialNumber == null || this.oadPacketsCharac == null || this.mailBox == null || this.headsetStatus == null){
            LogUtils.e(TAG, "error, not all characteristics have been found");
            gatt.disconnect();
            this.bluetoothController.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE);
        }else if(bluetoothController.currentState.equals(BtState.DISCOVERING_SERVICES))
            bluetoothController.updateConnectionState(); //current state is set to DISCOVERING_SUCCESS
    }

    /**
     * Callback triggered when gatt.readCharacteristic is called and no failure occured
     * @param gatt GATT client invoked {@link BluetoothGatt#readCharacteristic}
     * @param characteristic Characteristic that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed successfully.
     * */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if(characteristic.getValue() != null)
            LogUtils.i(TAG, "received charac value " + new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0) {
            String firmwareVersionValue = new String(characteristic.getValue());
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, firmwareVersionValue);
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, new String(characteristic.getValue()));
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String(characteristic.getValue()));
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_MODEL_NUMBER) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.MODEL_NUMBER, new String(characteristic.getValue()));
        }

        if (characteristic.getUuid().compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
            if (characteristic.getValue()!= null){
                if (characteristic.getValue().length < 4) {
                    final StringBuffer sb = new StringBuffer();
                    for (final byte value : characteristic.getValue()) {
                        sb.append(value);
                        sb.append(';');
                    }
                    LogUtils.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                            "but the payload of the characteristic is invalid ! \nValue(s) received -> " + sb.toString());
                    return;
                }

                final short level = MbtDeviceManager.getBatteryPercentageFromByteValue(characteristic.getValue()[0]);
                if (level == -1) {
                    LogUtils.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                            "but the returned value could not be decoded ! " +
                            "Byte value received -> " + characteristic.getValue()[3]);
                }
                bluetoothController.notifyBatteryReceived(level);
                LogUtils.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
                        "Value -> " + level);
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if(status != BluetoothGatt.GATT_SUCCESS ){
            LogUtils.e(TAG, "error writing characteristic status code is : " + status);
        }else{
            if(characteristic.getUuid().compareTo(this.oadPacketsCharac.getUuid()) == 0){
                /*if(oadFileManager.getmProgInfo().iBlocks == oadFileManager.getmProgInfo().nBlocks){ //TODO
                    if(oadPacketTransferTimeoutLock.isWaiting()){
                        oadPacketTransferTimeoutLock.setResultAndNotify(true);
                    }else{
                        LogUtils.e(TAG, "error, packet transfer timeout for end not ready");
                    }

                    notifyOADEvent(OADEvent.CRC_COMPUTING, oadFileManager.getmProgInfo().nBlocks);
                }else{
                    if(oadPacketTransferTimeoutLock.isWaiting() || oadFileManager.getmProgInfo().iBlocks <=1){
                        sendOADBlock(oadFileManager.getmProgInfo().iBlocks);
                    }else{
                        LogUtils.e(TAG, " error, lock isn't waiting yet");
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
            this.notifyMailboxEventReceived(characteristic);
            LogUtils.i(TAG, "mailbox message received with code " + characteristic.getValue()[0] +
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
            LogUtils.i(TAG, "Received a [onDescriptorWrite] callback with status SUCCESS");
        } else {
            LogUtils.e(TAG, "Received a [onDescriptorWrite] callback with Status: FAILURE.");
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
        LogUtils.i(TAG, "onMtuChanged with new value " + mtu);
        synchronized (this.bluetoothController){
            this.bluetoothController.notify();
        }

    }

    public void testAcquireDataZeros(){
        byte[] data = new byte[250];
        Arrays.fill(data,(byte) 0);
        this.bluetoothController.notifyNewDataAcquired(data);
    }

    void connectA2DPMailbox() {
        byte[] buffer = {MailboxEvents.MBX_CONNECT_IN_A2DP, (byte)0x25, (byte)0xA2}; //Send buffer
        if (this.mailBox != null) {
            this.mailBox.setValue(buffer);
        }
        if (!this.bluetoothController.gatt.writeCharacteristic(this.mailBox)) {
            Log.e(TAG, "Error: failed to send A2Dp connection request");
            this.bluetoothController.notifyConnectionStateChanged(BtState.CONNECTION_FAILURE);
            return;
        }
        //connectA2DPLock.waitAndGetResult(15000);
    }

//    int disconnectA2DPMailbox() {
//        byte[] buffer = {MailboxEvents.MBX_DISCONNECT_IN_A2DP, (byte)0x85, (byte)0x11};
//        //Send buffer
//        this.mailBox.setValue(buffer);
//        if (!this.bluetoothController.gatt.writeCharacteristic(this.mailBox)) {
//            Log.e(TAG, "Error: failed to send A2Dp disconnection request");
//            this.bluetoothController.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE);
//            return -1;
//        }
//        Integer res = disconnectA2DPLock.waitAndGetResult(15000);
//        return res == null ? MailboxEvents.CMD_CODE_CONNECT_IN_A2DP_FAILED_TIMEOUT : res;
//    }

    void notifyMailboxEventReceived(BluetoothGattCharacteristic characteristic) {
        switch(characteristic.getValue()[0]){
            case MailboxEvents.MBX_SET_ADS_CONFIG:
                break;

            case MailboxEvents.MBX_SET_AUDIO_CONFIG:
                break;

            //case MailboxEvents.MBX_SET_PRODUCT_NAME:
            case MailboxEvents.MBX_SET_SERIAL_NUMBER:
                ByteBuffer buf = ByteBuffer.allocate(characteristic.getValue().length-1);
                for (int i = 1; i < characteristic.getValue().length; i++){
                    buf.put(characteristic.getValue()[i]);
                }
                if(this.writeExternalNameLock.isWaiting())
                    this.writeExternalNameLock.setResultAndNotify(characteristic.getStringValue(1));
                break;

            case MailboxEvents.MBX_START_OTA_TXF:
                break;

            case MailboxEvents.MBX_LEAD_OFF_EVT:
                break;

            case MailboxEvents.MBX_OTA_MODE_EVT:
//                if(characteristic.getValue().length >= 2){
//                    if(this.oadcheckFwPlusLengthLock.isWaiting()){
//                        this.oadcheckFwPlusLengthLock.setResultAndNotify((int)characteristic.getValue()[1]);
//                    }
//                }
                break;

            case MailboxEvents.MBX_OTA_IDX_RESET_EVT:
                //In case fw missed a block, here is the code which get the block index requested by firmware. iBlocks is set back to the requested value.
//                if(characteristic.getValue().length >= 3){
//                    if(oadFileManager != null){
//
//                        byte[] idx = {characteristic.getValue()[1], characteristic.getValue()[2]};
//
//                               /* ByteBuffer b = ByteBuffer.allocate(2);
//                                b.put(characteristic.getValue()[1]);
//                                b.put(characteristic.getValue()[2]);*/
//                        short s = ByteBuffer.wrap(idx).order(ByteOrder.LITTLE_ENDIAN).getShort();
//                        oadFileManager.getmProgInfo().iBlocks = s;
//                        Log.i(TAG, "Fw missed block nb " + s + "restarting back from block " + s);
//                        if(s==0) //Force restart as it could be blocked
//                            sendOADBlock(0);
//                    }
//                }
                break;

            case MailboxEvents.MBX_OTA_STATUS_EVT:
//                if(value.length >= 2){
//                    if(this.oadCRCComputation.isWaiting()){
//                        byte b = characteristic.getValue()[1];
//                        this.oadCRCComputation.setResultAndNotify((b > 0));
//                    }
//                }
                break;

            case MailboxEvents.MBX_SYS_GET_STATUS:
                Log.i(TAG, "sys status" + Arrays.toString(characteristic.getValue()));
//                if(characteristic.getValue().length >= 5){
//                }
                break;

            case MailboxEvents.MBX_SET_NOTCH_FILT:
                Log.i(TAG, "received notification - Notch filter: " + new String(characteristic.getValue()));
                break;

            case MailboxEvents.MBX_SET_AMP_GAIN:
                Log.i(TAG, "received notification - amp gain : " + Arrays.toString(characteristic.getValue()));
                //if(this.ampGainNotificationLock.isWaiting())
                   // this.ampGainNotificationLock.setResultAndNotify(characteristic.getStringValue(1));
                break;

            case MailboxEvents.MBX_GET_EEG_CONFIG:
                Log.i(TAG, "received notification - eeg config: " + Arrays.toString(characteristic.getValue()));
               // if(eegConfigRetrievalLock.isWaiting()){
                 //   eegConfigRetrievalLock.setResultAndNotify(ArrayUtils.toObject(characteristic.getValue()));
                //}
                //TODO remove parsing from here
                int gainValue = AmpGainConfig.getGainFromByteValue(characteristic.getValue()[3]);//read the 3rd byte that contain the Gain value TODO: add defines for this offset !
                if (gainValue != 0) {
                    MbtDataConversion.EEG_AMP_GAIN = gainValue;
                }else {//else do not change the eegAmpGain value.
                    Log.i(TAG, "Gain code received in eeg config notification is unreadable, value: " + gainValue);
                }

                break;

            case MailboxEvents.MBX_P300_ENABLE:
                Log.i(TAG, "received answer from p300 activation");
               // p300activationLock.setResultAndNotify((int)(characteristic.getValue()[1]));
                break;

            case MailboxEvents.MBX_DC_OFFSET_ENABLE:
                break;

            case MailboxEvents.MBX_CONNECT_IN_A2DP:
                Log.i(TAG, "received A2DP connection code " + (int)(characteristic.getValue()[1]));
                if((characteristic.getValue()[1] & MailboxEvents.CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS) != 0x01)
                    bluetoothController.notifyAudioConnectionStateChanged(characteristic.getValue()[1]);
                break;

            case MailboxEvents.MBX_DISCONNECT_IN_A2DP:
                Log.i(TAG, "received A2DP connection code " + (int)(characteristic.getValue()[1]));
                //if(disconnectA2DPLock.isWaiting())
                //    disconnectA2DPLock.setResultAndNotify((int)(characteristic.getValue()[1]));
                break;

            case (byte)0xFF:
            default:
                break;

        }
    }

    boolean sendExternalName(String externalName) {
        if(externalName == null)
            return false;
        String nameReceivedFromHeadset = "";
        Log.i(TAG, "external name found is " + externalName);
        ByteBuffer nameToBytes = ByteBuffer.allocate(3 + externalName.getBytes().length); // +1 for mailbox code
        nameToBytes.put(MailboxEvents.MBX_SET_SERIAL_NUMBER);
        nameToBytes.put((byte) 0xAB);
        nameToBytes.put((byte) 0x21);
        nameToBytes.put(externalName.getBytes());

        byte[] buf = nameToBytes.array();

        this.mailBox.setValue(buf);//Send buffer
        if (!this.bluetoothController.gatt.writeCharacteristic(this.mailBox)) {
            Log.e(TAG, "Error: failed to send external name update");
            return false;
        }
        Log.i(TAG, "Success sending external name update");

        try {
            Future<String> futureNameReceivedFromHeadset = sendExternalNameAsync();
            nameReceivedFromHeadset = futureNameReceivedFromHeadset.get(MbtConfig.getBluetoothSendingExternalNameTimeout(), TimeUnit.MILLISECONDS);
            Log.i(TAG, "External name update completed. Expected name after update: " + externalName + "| Actual name stored by the headset: "+ nameReceivedFromHeadset);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.i(TAG, "External name sending has " + (e instanceof  TimeoutException ? "timed out" : "failed")); //errors are not reported
        } finally {
            stopSendingExternalName();
        }
        return nameReceivedFromHeadset.equals(externalName); //check is the headset has well stored its QR code value after the update
    }

    private Future<String> sendExternalNameAsync(){
        return AsyncUtils.executeAsync(new Callable<String>() {
            @Override
            public String call() {
                return writeExternalNameLock.waitAndGetResult(MbtConfig.getBluetoothSendingExternalNameTimeout(), TimeUnit.MILLISECONDS);
            }
        });
    }

    private void stopSendingExternalName(){
        if(writeExternalNameLock != null && writeExternalNameLock.isWaiting())
            writeExternalNameLock.unlock();
    }
}