package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Random;

import config.AmpGainConfig;
import core.bluetooth.BtState;
import core.eeg.acquisition.MbtDataConversion;
import core.recordingsession.metadata.DeviceInfo;
import features.MbtFeatures;
import utils.AsyncUtils;
import utils.FirmwareUtils;
import utils.LogUtils;
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
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

    private final MbtLock<BtState> connectionLock = new MbtLock<>();
    private final MbtLock<BtState> disconnectionLock = new MbtLock<>();
    private final MbtLock<Integer> connectA2DPLock = new MbtLock<>();
    private final MbtLock<Integer> disconnectA2DPLock = new MbtLock<>();
    private final MbtLock<Boolean> bondLock = new MbtLock<>();
    private final MbtLock<Integer> p300activationLock = new MbtLock<>();
    private final MbtLock<Byte[]> eegConfigRetrievalLock = new MbtLock<>();
    private final MbtLock<String> ampGainNotificationLock = new MbtLock<>();
    private final MbtLock<String> readDeviceInfoLock = new MbtLock<>();


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


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String msg = "Connection state change : ";
        switch(newState) {
            case BluetoothGatt.STATE_CONNECTED:
                this.bluetoothController.notifyConnectionStateChanged(BtState.CONNECTED, true); // This state is not really useful
                this.bluetoothController.notifyConnectionStateChanged(BtState.DISCOVERING_SERVICES, true);
                gatt.discoverServices(); //Discovers services offered by a remote device as well as their characteristics and descriptors. This is an asynchronous operation. Once service discovery is completed, the BluetoothGattCallback.onServicesDiscovered callback is triggered. If the discovery was successful, the remote services can be retrieved using the getServices function

                msg += "STATE_CONNECTED and now discovering services...";
                break;
            case BluetoothGatt.STATE_CONNECTING:
                this.bluetoothController.notifyConnectionStateChanged(BtState.CONNECTING, true);
                msg += "STATE_CONNECTING";
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                // This if is necessary because we might have disconnect after something went wrong while connecting
                if (this.connectionLock.isWaiting())
                    this.connectionLock.setResultAndNotify(BtState.CONNECTION_FAILURE);
                else {                    // in this case the connection went well for a while, but just got lost
                    gatt.close();
                    //this.gatt = null;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.bluetoothController.notifyConnectionStateChanged(BtState.DISCONNECTED, true);
                }
                msg += "STATE_DISCONNECTED";
                break;
            case BluetoothGatt.STATE_DISCONNECTING:
                msg += "STATE_DISCONNECTING";
                this.bluetoothController.notifyConnectionStateChanged(BtState.DISCONNECTING, true);
                break;
            default:
                this.bluetoothController.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE, true);
                gatt.close();
                msg += "Unknown value " + newState;
        }
        LogUtils.d("", msg);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        // Checking if services were indeed discovered or not : getServices should be not null and contains values at this point
        if (gatt.getServices() == null || gatt.getServices().isEmpty()) {
            gatt.disconnect();
            this.bluetoothController.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE, true);
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
        }

        // In case one of these is null, we disconnect because something went wrong
        if (this.mainService == null || this.measurement == null || this.battery == null || this.deviceInfoService == null
                || this.fwVersion == null || this.hwVersion == null || this.serialNumber == null || this.oadPacketsCharac == null || this.mailBox == null || this.headsetStatus == null){
            LogUtils.e(TAG, "error, not all characteristics have been found");
            gatt.disconnect();
            this.bluetoothController.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE, true);

        } else{
            // everything went well as expected
//            if(this.connectionLock.isWaiting())
//                this.connectionLock.setResultAndNotify(BtState.READING_DEVICE_INFO);
            this.bluetoothController.notifyConnectionStateChanged(BtState.READING_DEVICE_INFO, true);
            AsyncUtils.executeAsync(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run() {
                    MbtGattController.this.requestDeviceInformations(DeviceInfo.FW_VERSION);
                }
            });

        }

        // Starting Battery Reader Timer
        //startOrStopBatteryReader(true);
    }

    /**
     * Callback triggered when gatt.readCharacteristic is called and no failure occured
     * @param gatt
     * @param characteristic
     * @param status
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        LogUtils.i(TAG, "received " + new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0) {
            String firmwareVersionValue = new String(characteristic.getValue());
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, firmwareVersionValue); //bonding is not supported for firmware versions older than 1.7.0 so we consider than the connection process is completed
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, new String(characteristic.getValue()));
        }
        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0) {
            bluetoothController.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String(characteristic.getValue()));

            if(this.connectionLock.isWaiting())
                this.connectionLock.setResultAndNotify(BtState.CONNECTED_AND_READY);
        }

        if (characteristic.getUuid().compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
            if(bondLock.isWaiting() && status == 0) //ie SUCCESS so no bonding in progress
                bondLock.setResultAndNotify(false);

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
                LogUtils.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                        "but the returned value could not be decoded ! " +
                        "Byte value received -> " + characteristic.getValue()[3]);
            }
            bluetoothController.notifyBatteryReceived(level);

            // LogUtils.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
            //         "Value -> " + level);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        if(status !=0 ){
            LogUtils.e(TAG, "error writing characteristic nb : " + status);
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG) == 0){
            this.bluetoothController.notifyNewDataAcquired(characteristic.getValue());
        }else if(characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_HEADSET_STATUS) == 0){
            this.bluetoothController.notifyNewHeadsetStatus(characteristic.getValue());
        }else if(characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX) == 0){
            LogUtils.i(TAG, "mailbox message received with code " + characteristic.getValue()[0] +
                " and payload " + Arrays.toString(characteristic.getValue()));
            synchronized (this.bluetoothController){
                this.bluetoothController.notify();
            }
            this.bluetoothController.notifyMailboxEventReceived(characteristic);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void connectA2DPMailbox() {
        byte[] buffer = {MailboxEvents.MBX_CONNECT_IN_A2DP, (byte)0x25, (byte)0xA2};
        //Send buffer
        if (this.mailBox != null) {
            this.mailBox.setValue(buffer);
        }
        if (!this.bluetoothController.gatt.writeCharacteristic(this.mailBox)) {
            Log.e(TAG, "Error: failed to send A2Dp connection request");
            return;
        }
        connectA2DPLock.waitAndGetResult(15000);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void disconnectA2DPMailbox() {
        byte[] buffer = {MailboxEvents.MBX_DISCONNECT_IN_A2DP, (byte)0x85, (byte)0x11};
        //Send buffer
        this.mailBox.setValue(buffer);
        if (!this.bluetoothController.gatt.writeCharacteristic(this.mailBox)) {
            Log.e(TAG, "Error: failed to send A2Dp connection request");
            return;
        }
        disconnectA2DPLock.waitAndGetResult(15000);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void requestDeviceInformations(DeviceInfo deviceinfo) {
        switch(deviceinfo){
            case FW_VERSION:
                this.bluetoothController.readFwVersion();
                break;
            case HW_VERSION:
                this.bluetoothController.readHwVersion();
                break;
            case SERIAL_NUMBER:
                this.bluetoothController.readSerialNumber();
                break;
        }
    }

    void disconnectionWaitAndGetResult(int timeout){
        disconnectionLock.waitAndGetResult(timeout);
    }

    void bondingWaitAndGetResult(int timeout){
        bondLock.waitAndGetResult(timeout);
    }

    void notifyMailboxEventReceived(BluetoothGattCharacteristic characteristic) {
        switch(characteristic.getValue()[0]){
            case MailboxEvents.MBX_SET_ADS_CONFIG:
                break;
            case MailboxEvents.MBX_SET_AUDIO_CONFIG:
                break;
            //case MailboxEvents.MBX_SET_PRODUCT_NAME:
            case MailboxEvents.MBX_SET_SERIAL_NUMBER:
                Log.i(TAG, "characteristic size is " + characteristic.getValue().length);
//                        ByteBuffer buf = ByteBuffer.allocate(characteristic.getgetValue().length-1);
//                        for (int i = 1; i < characteristic.getValue().length; i++){
//                            buf.put(characteristic.getValue()[i]);
//                        }
//
//                        if(this.renamingNotificationLock.isWaiting())
//                            this.renamingNotificationLock.setResultAndNotify(characteristic.getStringValue(1));
//                            this.renamingNotificationLock.setResultAndNotify(new String(buf.array()));
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
                if(characteristic.getValue().length >= 3){
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
                }
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
                if(characteristic.getValue().length >= 5){
                }
                break;

            case MailboxEvents.MBX_SET_NOTCH_FILT:
                Log.i(TAG, "received notification - Notch filter: " + new String(characteristic.getValue()));

                break;

            case MailboxEvents.MBX_SET_AMP_GAIN:
                Log.i(TAG, "received notification - amp gain : " + Arrays.toString(characteristic.getValue()));

                if(this.ampGainNotificationLock.isWaiting())
                    this.ampGainNotificationLock.setResultAndNotify(characteristic.getStringValue(1));
                break;

            case MailboxEvents.MBX_GET_EEG_CONFIG:
                Log.i(TAG, "received notification - eeg config: " + Arrays.toString(characteristic.getValue()));

                if(eegConfigRetrievalLock.isWaiting()){
                    eegConfigRetrievalLock.setResultAndNotify(ArrayUtils.toObject(characteristic.getValue()));
                }
                //TODO remove parsing from here
                byte gainCode = characteristic.getValue()[3]; //read the 3rd byte that contain the Gain value TODO: add defines for this offset !

                if (gainCode == AmpGainConfig.AMP_GAIN_X12_DEFAULT.getNumVal()) {
                    MbtDataConversion.EEG_AMP_GAIN = 12;
                } else if (gainCode == AmpGainConfig.AMP_GAIN_X8_MEDIUM.getNumVal()) {
                    MbtDataConversion.EEG_AMP_GAIN = 8;
                } else if (gainCode == AmpGainConfig.AMP_GAIN_X6_LOW.getNumVal()) {
                    MbtDataConversion.EEG_AMP_GAIN = 6;
                } else if (gainCode == AmpGainConfig.AMP_GAIN_X4_VLOW.getNumVal()) {
                    MbtDataConversion.EEG_AMP_GAIN = 4;
                }else {
                    //else do not change the eegAmpGain value.
                    Log.i(TAG, "Gain code received in eeg config notification is unreadable, value: " + gainCode);
                }

                break;

            case MailboxEvents.MBX_P300_ENABLE:
                Log.i(TAG, "received answer from p300 activation");
                p300activationLock.setResultAndNotify((int)(characteristic.getValue()[1]));
                break;

            case MailboxEvents.MBX_DC_OFFSET_ENABLE:
                break;

            case MailboxEvents.MBX_CONNECT_IN_A2DP:
                Log.i(TAG, "received A2DP connection code " + (int)(characteristic.getValue()[1]));
                if(connectA2DPLock.isWaiting() && ((characteristic.getValue()[1] & MailboxEvents.CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS) != 0x01))
                    connectA2DPLock.setResultAndNotify((int)(characteristic.getValue()[1]));
                break;

            case MailboxEvents.MBX_DISCONNECT_IN_A2DP:
                Log.i(TAG, "received A2DP connection code " + (int)(characteristic.getValue()[1]));
                if(disconnectA2DPLock.isWaiting())
                    disconnectA2DPLock.setResultAndNotify((int)(characteristic.getValue()[1]));
                break;

            case (byte)0xFF:
            default:
                break;

        }
    }
}