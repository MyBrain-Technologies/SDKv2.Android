package core.device;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.sql.Connection;

import command.OADCommands;
import config.ConnectionConfig;
import core.BaseModuleManager;
import core.bluetooth.BtState;
import core.bluetooth.requests.CommandRequestEvent;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.device.oad.OADContract;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.BluetoothStateListener;
import eventbus.events.BluetoothResponseEvent;
import core.device.model.FirmwareVersion;
import core.device.model.MbtDevice;
import core.device.model.MelomindDevice;
import core.device.model.VProDevice;
import core.device.event.DCOffsetEvent;
import core.device.event.SaturationEvent;
import core.device.event.OADEvent;
import core.device.oad.OADManager;
import core.eeg.acquisition.MbtDataConversion;
import eventbus.MbtEventBus;
import eventbus.events.ConfigEEGEvent;
import eventbus.events.DeviceInfoEvent;
import eventbus.events.FirmwareUpdateClientEvent;
import eventbus.events.ReconnectionReadyEvent;
import eventbus.events.ClearBluetoothEvent;
import utils.LogUtils;

import static features.MbtDeviceType.MELOMIND;

//todo sections
/**
 * The Device unit deals with all the actions related to the Headset device (beside Bluetooth).
 * It provides features that allow the client to upgrade the firmware version,
 * or handle some signal features detected by the device such as the DC offset or the Saturation,
 * and store some device information.
 *
 * For example, device information includes
 * the firmware version installed,
 * hardware version,
 * serial number,
 * model number,
 * product name,
 * audio device address,
 * sampling rate,
 * number or channels,
 * locations of the ground(s), reference(s), and acquisition channels.
 * It extends the OADContract interface to receive notifications when an OAD event occurs
 */
public class MbtDeviceManager extends BaseModuleManager implements OADContract {

    private static final String TAG = MbtDeviceManager.class.getSimpleName();

    private OADManager oadManager;

    private MbtDevice mCurrentConnectedDevice;

    public MbtDeviceManager(Context context){
        super(context);
        this.mContext = context;
    }


    @Subscribe
    public void onNewDeviceMeasure(@NonNull final DeviceEvents.RawDeviceMeasure rawDeviceMeasure){
        //TODO complete cases with differents measures.
        if(rawDeviceMeasure.getRawMeasure().length < 2)
            return;

        if(rawDeviceMeasure.getRawMeasure()[0] == 0x01)
            MbtEventBus.postEvent(new SaturationEvent(rawDeviceMeasure.getRawMeasure()[1]));
        else if (rawDeviceMeasure.getRawMeasure()[0] == 0x02){
            if(rawDeviceMeasure.getRawMeasure().length < 8)
                return;
            long timestamp = ((rawDeviceMeasure.getRawMeasure()[1] & 0xFF) << (16)) | ((rawDeviceMeasure.getRawMeasure()[2] & 0xFF) << (8) | ((rawDeviceMeasure.getRawMeasure()[3] & 0xFF))); //parsing first 3 bytes as they represents the device intenal clock

            float[] dcOffsets = new float[2]; // parsing last 4 bytes as they represent the dcOffsets
            byte[] temp = new byte[2];
            System.arraycopy(rawDeviceMeasure.getRawMeasure(), 4, temp, 0, 2);
            dcOffsets[1] = MbtDataConversion.convertRawDataToDcOffset(temp);
            System.arraycopy(rawDeviceMeasure.getRawMeasure(), 6, temp, 0, 2);
            dcOffsets[0] = MbtDataConversion.convertRawDataToDcOffset(temp);
            MbtEventBus.postEvent(new DCOffsetEvent(timestamp, dcOffsets));
        }
    }


    public MbtDevice getmCurrentConnectedDevice() {
        return mCurrentConnectedDevice;
    }

    private void setAudioConnectedDeviceAddress(String audioDeviceAddress) {
        Log.d(TAG,"new connected audio device address stored "+audioDeviceAddress);
        if(mCurrentConnectedDevice != null)
            this.mCurrentConnectedDevice.setAudioDeviceAddress(audioDeviceAddress);
    }

    private void setmCurrentConnectedDevice(MbtDevice mCurrentConnectedDevice) {
        Log.d(TAG,"new connected device stored "+mCurrentConnectedDevice);
        this.mCurrentConnectedDevice = mCurrentConnectedDevice;
    }

    @Subscribe
    public void onNewDeviceDisconnected(DeviceEvents.DisconnectedDeviceEvent deviceEvent) {
        if (oadManager != null){
            if(oadManager.getCurrentState().equals(OADManager.OADState.AWAITING_DEVICE_REBOOT))
                oadManager.onOADEvent(OADEvent.DISCONNECTED_FOR_REBOOT);
            else if(oadManager.getCurrentState().equals(OADManager.OADState.RECONNECTING))
                oadManager.onOADEvent(OADEvent.RECONNECTION_PERFORMED.setEventData(false));
            else
                oadManager.onOADEvent(OADEvent.DISCONNECTED);
        }else
            setmCurrentConnectedDevice(null);
    }

    @Subscribe
    public void onNewDeviceAudioDisconnected(DeviceEvents.AudioDisconnectedDeviceEvent deviceEvent) {
        if(getmCurrentConnectedDevice() != null)
            getmCurrentConnectedDevice().setAudioDeviceAddress(null);
    }

    @Subscribe
    public void onNewDeviceConnected(DeviceEvents.FoundDeviceEvent deviceEvent) {
        if(oadManager != null)
            oadManager.onOADEvent(OADEvent
                    .RECONNECTION_PERFORMED
                    .setEventData(true));

        MbtDevice device = null;
        if (deviceEvent.getDevice() != null) {
            device = deviceEvent.getDeviceType().equals(MELOMIND) ?
                    new MelomindDevice(deviceEvent.getDevice()) : new VProDevice(deviceEvent.getDevice());
        }
        setmCurrentConnectedDevice(device);

    }

    @Subscribe
    public void onNewAudioDeviceConnected(DeviceEvents.AudioConnectedDeviceEvent deviceEvent) {
            setAudioConnectedDeviceAddress( (deviceEvent.getDevice() != null) ?
                    deviceEvent.getDevice().getAddress() : null);
    }

    @Subscribe
    public void onNewDeviceConfiguration(ConfigEEGEvent configEEGEvent){
        this.mCurrentConnectedDevice.setInternalConfig(new MbtDevice.InternalConfig(configEEGEvent.getConfig()));
    }

    /**
     * Called when a new device info event has been broadcast on the event bus.
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onDeviceInfoEvent(DeviceInfoEvent event){
        if(mCurrentConnectedDevice != null){
            switch(event.getInfotype()){
                case BATTERY:
                    LogUtils.d(TAG, "received " + event.getInfo() + " for battery level");
                    break;
                case FW_VERSION:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setFirmwareVersion((FirmwareVersion) event.getInfo());
                    break;
                case HW_VERSION:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setHardwareVersion((String) event.getInfo());
                    break;
                case SERIAL_NUMBER:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setSerialNumber((String) event.getInfo());
                    break;
                case MODEL_NUMBER:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setExternalName((String) event.getInfo());
                    break;
                case PRODUCT_NAME:
                    if(event.getInfo() != null)
                        mCurrentConnectedDevice.setProductName((String) event.getInfo());
                    break;
            }
        }
    }


    @Subscribe
    public void onGetDevice(DeviceEvents.GetDeviceEvent event){
        MbtEventBus.postEvent(new DeviceEvents.PostDeviceEvent(mCurrentConnectedDevice));
    }

    @Subscribe
    public void onOADEvent(OADEvent event) {
        if (event.isInitialEvent()){
            this.oadManager = new OADManager(mContext, this, event.getEventData());
        }else if(event.isFinalEvent()){
            this.oadManager = null;
        }
    }

    /**
     * Callback triggered when an OAD event
     *
     * is received by the Bluetooth unit
     */
    @Subscribe
    public void onBluetoothEventReceived(BluetoothResponseEvent event){
        LogUtils.d(TAG, "on Bluetooth event "+event.toString());
        if(event.isDeviceCommandEvent()){
            OADEvent oadEvent = OADEvent
                    .getEventFromMailboxCommand(event.getId())
                    .setEventData((byte[]) event.getDataValue());

            this.oadManager.onOADEvent(oadEvent);
        }
    }


    @Override
    public void stopOADUpdate() {

    }

    @Override
    public void requestFirmwareValidation(OADCommands.RequestFirmwareValidation requestFirmwareValidation) {
        MbtEventBus.postEvent(new CommandRequestEvent(requestFirmwareValidation));
    }

    @Override
    public void transferPacket(OADCommands.SendPacket sendPacket) {
        MbtEventBus.postEvent(new CommandRequestEvent(sendPacket));
    }

    @Override
    public void notifyClient(FirmwareUpdateClientEvent event) {
        MbtEventBus.postEvent(event);
    }

    @Override
    public void resetCacheAndKeys() {
        MbtEventBus.postEvent(new ClearBluetoothEvent());
    }

    @Override
    public void reconnect() {
        ConnectionConfig connectionConfig = new ConnectionConfig.Builder(null)
                .deviceName(mCurrentConnectedDevice.getProductName())
                .deviceQrCode(mCurrentConnectedDevice.getExternalName())
                .create();

        MbtEventBus.postEvent(new StartOrContinueConnectionRequestEvent(true,
                connectionConfig.getDeviceName(),
                connectionConfig.getDeviceQrCode(),
                connectionConfig.getDeviceType(),
                connectionConfig.getMtu()
             ));
    }

    @Override
    public boolean compareFirmwareVersion(FirmwareVersion firmwareVersionExpected) {
        return mCurrentConnectedDevice.getFirmwareVersion().equals(firmwareVersionExpected);
    }

}
