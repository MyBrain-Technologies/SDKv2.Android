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


import java.nio.ByteBuffer;
import java.util.Arrays;

import config.AmpGainConfig;
import config.DeviceConfig;
import core.bluetooth.BtState;
import core.device.MbtDeviceManager;
import core.device.model.DeviceInfo;
import core.eeg.acquisition.MbtDataConversion;
import utils.LogUtils;

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

    @Nullable
    private BluetoothGattService mainService = null;
    @Nullable
    private BluetoothGattService deviceInfoService = null;
    @Nullable
    private BluetoothGattCharacteristic measurement = null;
    @Nullable
    private BluetoothGattCharacteristic headsetStatus = null;
    @Nullable
    private BluetoothGattCharacteristic mailBox = null;
    @Nullable
    private BluetoothGattCharacteristic oadPacketsCharac = null;
    @Nullable
    private BluetoothGattCharacteristic battery = null;
    @Nullable
    private BluetoothGattCharacteristic fwVersion = null;
    @Nullable
    private BluetoothGattCharacteristic hwVersion = null;
    @Nullable
    private BluetoothGattCharacteristic serialNumber = null;
    @Nullable
    private BluetoothGattCharacteristic modelNumber = null;

    private final MbtBluetoothLE mbtBluetoothLE;

    MbtGattController(Context context, MbtBluetoothLE mbtBluetoothLE) {
        super();
        this.mbtBluetoothLE = mbtBluetoothLE;
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
        String msg = "Current state was "+ mbtBluetoothLE.getCurrentState()+" but Connection state change : " + (newState == 2 ? "connected " : " code is "+newState);
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTING:
                if (mbtBluetoothLE.getCurrentState().equals(BtState.DEVICE_FOUND))
                    this.mbtBluetoothLE.updateConnectionState(false);//current state is set to CONNECTING
                break;
            case BluetoothGatt.STATE_CONNECTED:
                LogUtils.i(TAG,"gattcontroller notified that headset is connected ");
                if (mbtBluetoothLE.getCurrentState().equals(BtState.CONNECTING) || mbtBluetoothLE.getCurrentState().equals(BtState.SCAN_STARTED))
                    this.mbtBluetoothLE.updateConnectionState(true);//current state is set to CONNECTION_SUCCESS and future is completed
                else if(mbtBluetoothLE.getCurrentState().equals(BtState.IDLE))
                    this.mbtBluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
                    break;

            case BluetoothGatt.STATE_DISCONNECTING:
                this.mbtBluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTING);
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                //if(isDownloadingFirmware) //todo OAD
                //    refreshDeviceCache(gatt);// in this case the connection went well for a while, but just got lost
                gatt.close();
                LogUtils.i(TAG,"gattcontroller notified that headset is disconnected ");
                this.mbtBluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTED);
                break;
            default:
                this.mbtBluetoothLE.notifyConnectionStateChanged(BtState.INTERNAL_FAILURE);
                gatt.close();
                msg += "Unknown value " + newState;
        }
        LogUtils.d("", msg);
    }

    /**
     * Callback invoked when the list of remote services, characteristics and descriptors
     * for the remote device have been updated, ie new services have been discovered.
     *
     * @param gatt
     * @param status
     */
    @Override
    public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
        LogUtils.i(TAG, "on services discovered ");
        super.onServicesDiscovered(gatt, status);

        // Checking if services were indeed discovered or not : getServices should be not null and contains values at this point
        if (gatt.getServices() == null || gatt.getServices().isEmpty() || status != BluetoothGatt.GATT_SUCCESS) {
            gatt.disconnect();
            if (mbtBluetoothLE.getCurrentState().equals(BtState.DISCOVERING_SERVICES))
                this.mbtBluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE);
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
                || this.fwVersion == null || this.hwVersion == null || this.serialNumber == null || this.oadPacketsCharac == null || this.mailBox == null || this.headsetStatus == null) {
            LogUtils.e(TAG, "error, not all characteristics have been found");
            gatt.disconnect();
            this.mbtBluetoothLE.notifyConnectionStateChanged(BtState.DISCOVERING_FAILURE);
        } else if (mbtBluetoothLE.getCurrentState().equals(BtState.DISCOVERING_SERVICES))
            mbtBluetoothLE.updateConnectionState(true); //current state is set to DISCOVERING_SUCCESS and future is completed
    }

    /**
     * Callback triggered when gatt.readCharacteristic is called and no failure occured
     *
     * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
     * @param characteristic Characteristic that was read from the associated remote device.
     * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed successfully.
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (characteristic.getValue() != null)
            LogUtils.i(TAG, "onCharacteristicRead : " + new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_MODEL_NUMBER) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.MODEL_NUMBER, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
            LogUtils.i(TAG, "battery level status : " + status);
            int GATT_AUTHENTICATION_FAIL = 0x89;
            if(status == GATT_AUTHENTICATION_FAIL)
                mbtBluetoothLE.notifyBatteryReceived(0, false);

            if (characteristic.getValue() != null) {
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
                LogUtils.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
                        "Value -> " + level);
                mbtBluetoothLE.notifyBatteryReceived(level, status == BluetoothGatt.GATT_SUCCESS);
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
    LogUtils.d(TAG,"on characteristic write : "+(status == BluetoothGatt.GATT_SUCCESS ? "success ": "failure")+ " for characteristic "+characteristic.getUuid());
//        if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX) == 0) {
//            LogUtils.i(TAG, "mailbox message received with code " + characteristic.getValue()[0] +
//                    " and payload " + Arrays.toString(characteristic.getValue()));
//            this.notifyDeviceConfigReceived(characteristic);
//        }
        mbtBluetoothLE.completeFutureOperation();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG) == 0) {
            this.mbtBluetoothLE.notifyNewDataAcquired(characteristic.getValue());
        } else if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_HEADSET_STATUS) == 0) {
            this.mbtBluetoothLE.notifyNewHeadsetStatus(characteristic.getValue());
        } else if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX) == 0) {
            this.notifyMailboxEventReceived(characteristic);
            LogUtils.i(TAG, "mailbox message received with code " + characteristic.getValue()[0] +
                    " and payload " + Arrays.toString(characteristic.getValue()));
            mbtBluetoothLE.completeFutureOperation();
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
        mbtBluetoothLE.completeFutureOperation();
        mbtBluetoothLE.onNotificationStateChanged(status == BluetoothGatt.GATT_SUCCESS, descriptor.getCharacteristic(), descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);


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
        mbtBluetoothLE.notifyDeviceConfigReceived(new byte[]{(byte)mtu}, DeviceConfig.MTU_CONFIG);
    }

    private void notifyMailboxEventReceived(BluetoothGattCharacteristic characteristic) {
        switch (characteristic.getValue()[0]) {
            case MailboxEvents.MBX_SET_ADS_CONFIG:
            case MailboxEvents.MBX_SET_AUDIO_CONFIG:
            case MailboxEvents.MBX_START_OTA_TXF:
            case MailboxEvents.MBX_LEAD_OFF_EVT:
            case MailboxEvents.MBX_OTA_MODE_EVT:
            case MailboxEvents.MBX_OTA_IDX_RESET_EVT:
            case MailboxEvents.MBX_OTA_STATUS_EVT:
                break;

            case MailboxEvents.MBX_SET_SERIAL_NUMBER: //this case occurs when the QR code is sent to the Headset through a writing operation
                LogUtils.i(TAG, "Received response from headset to the External name update "+characteristic.getStringValue(1));
                ByteBuffer buf = ByteBuffer.allocate(characteristic.getValue().length-1);
                for (int i = 1; i < characteristic.getValue().length; i++){
                    buf.put(characteristic.getValue()[i]);
                }
                break;

            case MailboxEvents.MBX_SYS_GET_STATUS:
                LogUtils.i(TAG, "sys status" + Arrays.toString(characteristic.getValue()));
                mbtBluetoothLE.notifyMailboxEventReceived(characteristic.getValue()[0]);
                break;

            case MailboxEvents.MBX_SET_NOTCH_FILT:
                mbtBluetoothLE.notifyDeviceConfigReceived(characteristic.getValue(), DeviceConfig.NOTCH_FILTER_CONFIG);
                break;

            case MailboxEvents.MBX_SET_AMP_GAIN:
                mbtBluetoothLE.notifyDeviceConfigReceived(characteristic.getValue(), DeviceConfig.AMP_GAIN_CONFIG);
                break;

            case MailboxEvents.MBX_GET_EEG_CONFIG:
                mbtBluetoothLE.notifyDeviceConfigReceived(characteristic.getValue(), DeviceConfig.EEG_CONFIG);
                break;

            case MailboxEvents.MBX_P300_ENABLE:
                mbtBluetoothLE.notifyDeviceConfigReceived(characteristic.getValue(), DeviceConfig.P300_CONFIG);
                break;

            case MailboxEvents.MBX_DC_OFFSET_ENABLE:
                mbtBluetoothLE.notifyDeviceConfigReceived(characteristic.getValue(), DeviceConfig.OFFSET_CONFIG);
                break;

            case MailboxEvents.MBX_CONNECT_IN_A2DP:
            case MailboxEvents.MBX_DISCONNECT_IN_A2DP:
                mbtBluetoothLE.notifyMailboxEventReceived(characteristic.getValue()[0]);
                break;

            case (byte) 0xFF:
            default:
                break;

        }
    }
}