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

import command.DeviceCommandEvent;
import core.bluetooth.BtState;
import core.device.model.DeviceInfo;
import core.device.model.MelomindDevice;
import utils.CommandUtils;
import utils.LogUtils;
import utils.BitUtils;

import static command.DeviceCommandEvent.*;
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
                mbtBluetoothLE.onStateConnecting();
                break;

            case BluetoothGatt.STATE_CONNECTED:
                mbtBluetoothLE.onStateConnected();
                break;

            case BluetoothGatt.STATE_DISCONNECTING:
                mbtBluetoothLE.notifyConnectionStateChanged(BtState.DISCONNECTING);
                break;

            case BluetoothGatt.STATE_DISCONNECTED:
                LogUtils.e(TAG, "Gatt returned disconnected state");
                this.mbtBluetoothLE.onStateDisconnected(gatt);
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
            //write no response requested in core.device.oad fw specification
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
        Log.d(TAG, "on Characteristic Read value: "+(characteristic.getValue() == null ? characteristic.getValue() : Arrays.toString(characteristic.getValue())) );

        if (characteristic.getUuid().compareTo(CHARAC_INFO_FIRMWARE_VERSION) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.FW_VERSION, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_HARDWARE_VERSION) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.HW_VERSION, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_SERIAL_NUMBER) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.SERIAL_NUMBER, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_INFO_MODEL_NUMBER) == 0)
            mbtBluetoothLE.notifyDeviceInfoReceived(DeviceInfo.MODEL_NUMBER, new String(characteristic.getValue()));

        if (characteristic.getUuid().compareTo(CHARAC_MEASUREMENT_BATTERY_LEVEL) == 0) {
            short level = -1;
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

                level = MelomindDevice.getBatteryPercentageFromByteValue(characteristic.getValue()[0]);
                if (level == -1) {
                    LogUtils.e(TAG, "Error: received a [onCharacteristicRead] callback for battery level request " +
                            "but the returned value could not be decoded ! " +
                            "Byte value received -> " + characteristic.getValue()[3]);
                }
                LogUtils.i(TAG, "Received a [onCharacteristicRead] callback for battery level request. " +
                        "Value -> " + level);
            }
            mbtBluetoothLE.notifyBatteryReceived(level);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        //Log.d(TAG, "on Characteristic Write value: "+(characteristic.getValue() == null ? characteristic.getValue() : Arrays.toString(characteristic.getValue())) );
        mbtBluetoothLE.notifyEventReceived(DeviceCommandEvent.OTA_STATUS_TRANSFER,
                new byte[]{BitUtils.booleanToBit(status == BluetoothGatt.GATT_SUCCESS)});
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        //Log.d(TAG, "on Characteristic Changed value: "+(characteristic.getValue() == null ? characteristic.getValue() : Arrays.toString(characteristic.getValue())) );

        if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_EEG) == 0) {
            this.mbtBluetoothLE.notifyNewDataAcquired(characteristic.getValue());
        } else if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_HEADSET_STATUS) == 0) {
            this.mbtBluetoothLE.notifyNewHeadsetStatus(characteristic.getValue());
        } else if (characteristic.getUuid().compareTo(MelomindCharacteristics.CHARAC_MEASUREMENT_MAILBOX) == 0) {
            this.onMailboxEventReceived(characteristic);
            //mbtBluetoothLE.stopWaitingOperation(true, false);
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
        LogUtils.i(TAG, "Received a [onDescriptorWrite] callback with status "+((status == BluetoothGatt.GATT_SUCCESS) ? "SUCCESS" : "FAILURE"));

        mbtBluetoothLE.stopWaitingOperation(status == BluetoothGatt.GATT_SUCCESS);
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
        mbtBluetoothLE.stopWaitingOperation(mtu
        );
    }

    /**
     * Notifies that the connected headset returned a response after a characteristic writing operation
     * @param characteristic
     */
    private void onMailboxEventReceived(BluetoothGattCharacteristic characteristic) {
        //Log.d(TAG, "Mailbox event received " + Arrays.toString(characteristic.getValue()));
        byte[] response = CommandUtils.deserialize(characteristic.getValue());
        byte mailboxEvent = characteristic.getValue()[0];
        DeviceCommandEvent event = DeviceCommandEvent.getEventFromIdentifierCode(mailboxEvent);
        if (event == null){
            LogUtils.e(TAG, "Event " + mailboxEvent + " not found ");
            return;
        }

        switch (event) {
                //mailbox events received in response to a request sent by the SDK
            case MBX_CONNECT_IN_A2DP:
            case MBX_DISCONNECT_IN_A2DP:
            case MBX_SET_SERIAL_NUMBER: //this case occurs when a QR code or a serial number is sent to the headset through a writing operation);
            case MBX_SET_PRODUCT_NAME:
            case MBX_SYS_GET_STATUS:
            case MBX_SYS_REBOOT_EVT: //to this day, this case is supposed to never be called : there is no response for reboot
            case MBX_SET_NOTCH_FILT:
            case MBX_SET_AMP_GAIN:
            case MBX_GET_EEG_CONFIG:
            case MBX_P300_ENABLE:
            case MBX_DC_OFFSET_ENABLE:
            case MBX_OTA_MODE_EVT:
                notifyResponseReceived(event, response);
                break;

                //mailbox events received that are NOT in response to a request sent by the SDK
            case MBX_OTA_STATUS_EVT:
            case MBX_OTA_IDX_RESET_EVT:
                mbtBluetoothLE.notifyEventReceived(event, response);
                break;

            case MBX_SET_ADS_CONFIG:
            case MBX_SET_AUDIO_CONFIG:
            case MBX_LEAD_OFF_EVT:
            case MBX_BAD_EVT:
            default:
                break;
        }
    }

     private void notifyResponseReceived(DeviceCommandEvent mailboxEvent, byte[] response){
         if(isMailboxEventFinished(mailboxEvent , response)){
             if(isConnectionMailboxEvent(mailboxEvent))
                 mbtBluetoothLE.notifyConnectionResponseReceived(mailboxEvent, response[0]); //connection and disconnection response are composed of only one byte
             mbtBluetoothLE.stopWaitingOperation(response);
         }

     }

    /**
     * Return true if the mailboxEvent if the Bluetooth connection or disconnection event is finished (no more reponse will be received)
     * @param mailboxEvent mailbox command identifier
     */
     boolean isConnectionMailboxEvent(DeviceCommandEvent mailboxEvent){
         return (mailboxEvent == MBX_DISCONNECT_IN_A2DP || mailboxEvent == MBX_CONNECT_IN_A2DP);
     }

    /**
     * Return true if the mailboxEvent if the Bluetooth connection or disconnection event is finished (no more reponse will be received)
     * The SDK waits until timeout if a mailbox response is received for a command that is not finished
     * @param mailboxEvent mailbox command identifier
     */
     private boolean isMailboxEventFinished(DeviceCommandEvent mailboxEvent, byte[] response){
        return mailboxEvent != MBX_CONNECT_IN_A2DP //the connect a2dp command is the only command where the headset returns several responses
                || (!BitUtils.areByteEquals(MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_IN_PROGRESS), response[0])//wait another response until timeout if the connection is not in progress
                    && !BitUtils.areByteEquals(MBX_CONNECT_IN_A2DP.getResponseCodeForKey(CMD_CODE_CONNECT_IN_A2DP_LINKKEY_INVALID), response[0])); //wait another response until timeout if the linkkey invalid response is returned
     }
}