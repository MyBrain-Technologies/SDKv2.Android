package core;

import android.content.BroadcastReceiver;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.Set;

import command.CommandInterface;
import command.DeviceCommand;

import config.RecordConfig;
import config.StreamConfig;
import core.bluetooth.BluetoothContext;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.StreamState;
import core.bluetooth.requests.CommandRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.bluetooth.requests.ReadRequestEvent;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.event.DCOffsetEvent;
import core.device.DeviceEvents;
import core.device.MbtDeviceManager;
import core.device.event.SaturationEvent;
import core.device.model.DeviceInfo;
import core.device.model.MbtDevice;
import core.device.model.MelomindsQRDataBase;
import core.device.model.MbtVersion;
import core.eeg.MbtEEGManager;
import core.recording.MbtRecordingManager;
import engine.SimpleRequestCallback;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.BluetoothStateListener;
import engine.clientevents.ConnectionStateListener;
import engine.clientevents.DeviceBatteryListener;
import engine.clientevents.DeviceStatusListener;
import engine.clientevents.EegError;
import engine.clientevents.EegListener;
import engine.clientevents.HeadsetDeviceError;
import engine.clientevents.OADStateListener;
import eventbus.MbtEventBus;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.DeviceInfoEvent;
import eventbus.events.FirmwareUpdateClientEvent;
import eventbus.events.SignalProcessingEvent;
import features.MbtDeviceType;
import features.MbtFeatures;
import mbtsdk.com.mybraintech.mbtsdk.R;
import utils.LogUtils;

import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.BLUETOOTH_ENABLED;
import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.DEVICE_ENABLED;
import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.EEG_ENABLED;
import static mbtsdk.com.mybraintech.mbtsdk.BuildConfig.RECORDING_ENABLED;

/**
 * MbtManager is responsible for managing communication between all the package managers
 *
 * @author Sophie ZECRI on 29/05/2018
 */
public class MbtManager {

  private static final String TAG = MbtManager.class.getName();

  private static final boolean START = true;
  private static final boolean STOP = false;

  /**
   * Contains the currently reigstered module managers.
   */
  private Set<BaseModuleManager> registeredModuleManagers;

  /**
   * The application context
   */
  private Context mContext;

  /**
   * the application callbacks. EventBus is not available outside the SDK so the user is notified
   * using custom callback interfaces.
   */
  private ConnectionStateListener<BaseError> connectionStateListener;

  /**
   * Listener used to notify the SDK client when the current OAD state changes or when the SDK raises an error.
   */
  private OADStateListener oadStateListener;
  private EegListener<BaseError> eegListener;
  private DeviceBatteryListener<BaseError> deviceInfoListener;
  @Nullable
  private DeviceStatusListener deviceStatusListener;


  public MbtManager(Context context) {
    this.mContext = context;
    this.registeredModuleManagers = new HashSet<>();

    MbtEventBus.registerOrUnregister(true, this);

    if (DEVICE_ENABLED)
      registerManager(new MbtDeviceManager(mContext));
    if (BLUETOOTH_ENABLED)
      registerManager(new MbtBluetoothManager(mContext));
    if (EEG_ENABLED)
      registerManager(new MbtEEGManager(mContext)); //todo change protocol must not be initialized here : when connectBluetooth is called
    if (RECORDING_ENABLED)
      registerManager(new MbtRecordingManager(mContext));
  }

  /**
   * Add a new module manager instance to the Hashset
   *
   * @param manager the new module manager to add
   */
  private void registerManager(BaseModuleManager manager) {
    registeredModuleManagers.add(manager);
  }

  /**
   * Perform a new Bluetooth connection.
   *
   * @param connectionStateListener a set of callback that will notify the user about connection progress.
   */
  public void connectBluetooth(@NonNull ConnectionStateListener<BaseError> connectionStateListener,
                               boolean connectAudioIfDeviceCompatible,
                               String deviceNameRequested,
                               String deviceQrCodeRequested,
                               MbtDeviceType deviceTypeRequested,
                               int mtu) {
    this.connectionStateListener = connectionStateListener;

    if (deviceNameRequested != null && (!deviceNameRequested.startsWith(MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX) && !deviceNameRequested.startsWith(MbtFeatures.VPRO_DEVICE_NAME_PREFIX))) {
      this.connectionStateListener.onError(HeadsetDeviceError.ERROR_PREFIX, " " + (deviceTypeRequested.equals(MbtDeviceType.MELOMIND) ? MbtFeatures.MELOMIND_DEVICE_NAME_PREFIX : MbtFeatures.VPRO_DEVICE_NAME_PREFIX));
    } else if (deviceQrCodeRequested != null && (!deviceQrCodeRequested.startsWith(MbtFeatures.QR_CODE_NAME_PREFIX))) {
      this.connectionStateListener.onError(HeadsetDeviceError.ERROR_PREFIX, " " + MbtFeatures.QR_CODE_NAME_PREFIX);
    } else if (deviceQrCodeRequested != null && deviceNameRequested != null && !deviceNameRequested.equals(new MelomindsQRDataBase(mContext, true).get(deviceQrCodeRequested))) {
      this.connectionStateListener.onError(HeadsetDeviceError.ERROR_MATCHING, mContext.getString(R.string.aborted_connection));
    } else {
      MbtEventBus.postEvent(new StartOrContinueConnectionRequestEvent(true,
          new BluetoothContext(mContext,
              deviceTypeRequested,
              connectAudioIfDeviceCompatible,
              deviceNameRequested,
              deviceQrCodeRequested,
              mtu)));
    }
  }

  /**
   * Perform a Bluetooth disconnection.
   */
  public void disconnectBluetooth(boolean isAbortion) {
    MbtEventBus.postEvent(new DisconnectRequestEvent(isAbortion));
  }

  /**
   * Perform a bluetooth read operation.
   *
   * @param deviceInfo the type of info to read
   */
  public void readBluetooth(@NonNull DeviceInfo deviceInfo, @NonNull DeviceBatteryListener<BaseError> listener) {
    this.deviceInfoListener = listener;
    MbtEventBus.postEvent(new ReadRequestEvent(deviceInfo));
  }


  /**
   * Posts an event to initiate a stream session.
   */
  public void startStream(StreamConfig streamConfig) {
    this.eegListener = streamConfig.getEegListener();
    this.deviceStatusListener = streamConfig.getDeviceStatusListener();

    for (DeviceCommand command : streamConfig.getDeviceCommands()) {
      sendCommand(command);
    }
    MbtEventBus.postEvent(
        new StreamRequestEvent(START,
            streamConfig.getRecordConfig() != null,
            streamConfig.shouldComputeQualities(),
            (deviceStatusListener != null),
            streamConfig.getRecordConfig()));
  }

  /**
   * Posts an event to stop the currently started stream session
   */
  public void stopStream(@Nullable RecordConfig recordConfig) {
    MbtEventBus.postEvent(
        new StreamRequestEvent(STOP, false,
            false, false, recordConfig));
  }

  public void startRecord(Context context) {
    MbtEventBus.postEvent(
        new StreamRequestEvent(START, true,
            false, (deviceStatusListener != null), new RecordConfig.Builder(context).create()));
  }

  public void stopRecord(@NonNull RecordConfig recordConfig) {
    MbtEventBus.postEvent(
        new StreamRequestEvent(STOP, eegListener != null, //if eeg listener is null, it means that the client has not previously called start stream (might have called start record), so the SDK should stop the streaming started when the client has called start record
            false, false, recordConfig));
  }

  /**
   * Send a command request
   * to the connected headset,
   * such as a Mailbox command,
   * in order to configure a parameter,
   * or get values stored by the headset
   * or ask the headset to perform an action.
   *
   * @param command is the command to send
   */
  public void sendCommand(@NonNull CommandInterface.MbtCommand command) {
    MbtEventBus.postEvent(new CommandRequestEvent(command));
  }

  /**
   * Perform a request to start an OAD firmware update.
   *
   * @param firmwareVersion is the firmware version to install on the connected headset device.
   * @param stateListener   is an optional (nullable) listener that notify the client when the OAD update progress & state change.
   */
  public void updateFirmware(MbtVersion firmwareVersion, OADStateListener<BaseError> stateListener) {
    this.oadStateListener = stateListener;
    DeviceEvents.StartOADUpdate event = new DeviceEvents.StartOADUpdate(firmwareVersion);
    MbtEventBus.postEvent(event);
  }

  /**
   * Apply a bandpass filter to the input signal to keep frequencies included between
   *
   * @param minFrequency   and
   * @param maxFrequency   .
   * @param size           is the number of EEG data of one channel
   * @param inputData      is the array of EEG data to filter for one channel
   * @param resultCallback is the callback that returns the filtered signal
   */
  public void bandpassFilter(float minFrequency, float maxFrequency, int size, @NonNull float[] inputData, @NonNull final SimpleRequestCallback<float[]> resultCallback) {
    if (resultCallback == null)
      return;
    if (inputData == null || inputData.length == 0 || size < 0)
      resultCallback.onRequestComplete(null);

    MbtEventBus.postEvent(new SignalProcessingEvent.GetBandpassFilter(minFrequency, maxFrequency, inputData, size),
        new MbtEventBus.Callback<SignalProcessingEvent.PostBandpassFilter>() {
          @Override
          @Subscribe
          public Void onEventCallback(SignalProcessingEvent.PostBandpassFilter filteredSignal) {
            MbtEventBus.registerOrUnregister(false, this);
            resultCallback.onRequestComplete(filteredSignal.getOutputSignal());
            return null;
          }
        });

  }

  /**
   * Sets an extended {@link BroadcastReceiver} to the connectionStateListener value
   *
   * @param connectionStateListener the new {@link BluetoothStateListener}. Set it to null if you want to reset the listener
   */
  public void setConnectionStateListener(ConnectionStateListener<BaseError> connectionStateListener) {
    this.connectionStateListener = connectionStateListener;
  }

  /**
   * Sets the {@link EegListener} to the connectionStateListener value
   *
   * @param EEGListener the new {@link EegListener}. Set it to null if you want to reset the listener
   */
  public void setEEGListener(EegListener<BaseError> EEGListener) {
    this.eegListener = EEGListener;
  }

  /**
   * Called when a new device info event has been broadcast on the event bus.
   */
  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onDeviceInfoEvent(DeviceInfoEvent event) {
    if (event.getDeviceInfo().equals(DeviceInfo.BATTERY)) {
      LogUtils.i(TAG, " manager received battery level " + event.getInfo());
      if (deviceInfoListener != null) {
        if (event.getInfo() == null)
          deviceInfoListener.onError(HeadsetDeviceError.ERROR_TIMEOUT_BATTERY, "");
        else {
          if (event.getInfo().equals(-1))
            deviceInfoListener.onError(HeadsetDeviceError.ERROR_DECODE_BATTERY, "");
          else
            deviceInfoListener.onBatteryLevelReceived((String) event.getInfo());
        }
      }
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onConnectionStateChanged(ConnectionStateEvent connectionStateEvent) {
    if (connectionStateListener == null)
      return;

    if (connectionStateListener instanceof BluetoothStateListener)
      ((BluetoothStateListener) connectionStateListener).onNewState(connectionStateEvent.getNewState(), connectionStateEvent.getDevice());

    switch (connectionStateEvent.getNewState()) {
      case CONNECTED_AND_READY:
        connectionStateListener.onDeviceConnected(connectionStateEvent.getDevice());
        break;
      case DATA_BT_DISCONNECTED:
        connectionStateListener.onDeviceDisconnected();
        break;
      default:
        if (connectionStateEvent.getNewState().isAFailureState())
          connectionStateListener.onError(connectionStateEvent.getNewState().getAssociatedError(), connectionStateEvent.getAdditionalInfo());
        break;
    }
  }

  /**
   * Called when a new stream state event has been broadcast on the event bus.
   *
   * @param newState
   */
  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onStreamStateChanged(StreamState newState) {
    if (eegListener != null) {
      eegListener.onNewStreamState(newState);

      if (newState == StreamState.FAILED) {
        eegListener.onError(EegError.ERROR_FAIL_START_STREAMING, null);
      } else if (newState == StreamState.DISCONNECTED) {
        eegListener.onError(BluetoothError.ERROR_NOT_CONNECTED, null);
      } else if (newState == StreamState.STOPPED) {
        eegListener = null;
      }
    }
  }

  /**
   * Called when a new saturation event has been broadcast on the event bus.
   */
  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onNewSaturationState(SaturationEvent saturationEvent) {
    if (deviceStatusListener != null) {
      deviceStatusListener.onSaturationStateChanged(saturationEvent);
    }
  }

  /**
   * Called when a new DCOffset measure event has been broadcast on the event bus.
   */
  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onNewDCOffset(DCOffsetEvent dcOffsets) {
    if (deviceStatusListener != null) {
      deviceStatusListener.onNewDCOffsetMeasured(dcOffsets);
    }
  }

  /**
   * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
   * This event is published by {@link MbtEEGManager}:
   * this manager handles EEG data acquired by the headset
   * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
   *
   * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
   */
  @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
  public void onEvent(@NonNull final ClientReadyEEGEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
    if (eegListener != null)
      eegListener.onNewPackets(event.getEegPackets());
  }

  /**
   * onFirmwareUpdateEvent is called by the Event Bus when a FirmwareUpdateClientEvent event is posted
   * This event is published by {@link MbtDeviceManager}:
   * this manager handles the OAD firmware update
   *
   * @param event contains data transmitted by the publisher : here it contains the new OAD update state or the error that occured if the SDK raised an error.
   */
  @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
  public void onFirmwareUpdateEvent(@NonNull final FirmwareUpdateClientEvent event) { //warning : do not remove this attribute (consider unsused by the IDE, but actually used)
    if (this.oadStateListener != null) {
      if (event.getError() != null)
        oadStateListener.onError(event.getError(), event.getAdditionalInfo());

      else {
        if (event.getOadState() != null)
          oadStateListener.onStateChanged(event.getOadState());

        oadStateListener.onProgressPercentChanged(event.getOadProgress());
      }
    }
  }

  public void requestCurrentConnectedDevice(@NonNull final SimpleRequestCallback<MbtDevice> callback) {
    if (callback == null)
      return;

    MbtEventBus.postEvent(new DeviceEvents.GetDeviceEvent(), new MbtEventBus.Callback<DeviceEvents.PostDeviceEvent>() {
      @Override
      @Subscribe
      public Void onEventCallback(DeviceEvents.PostDeviceEvent object) {
        MbtEventBus.registerOrUnregister(false, this);
        callback.onRequestComplete(object.getDevice());
        return null;
      }
    });
  }

  Set<BaseModuleManager> getRegisteredModuleManagers() {
    return registeredModuleManagers;
  }
}