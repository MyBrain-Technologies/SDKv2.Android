package core.bluetooth

import android.content.Context
import command.BluetoothCommands.Mtu
import command.DeviceCommands.UpdateExternalName
import core.device.model.MbtDevice
import core.device.model.MelomindsQRDataBase
import engine.SimpleRequestCallback
import features.MbtFeatures
import utils.AsyncUtils
import utils.LogUtils
import utils.VersionHelper

/**
 * Created by Etienne on 08/02/2018.
 * This class contains all necessary methods to manage the Bluetooth communication with the myBrain peripheral devices.
 * - 3 Bluetooth layers are used :
 * - Bluetooth Low Energy protocol is used with Melomind Headset for communication.
 * - Bluetooth SPP protocol which is used for the VPro headset communication.
 * - Bluetooth A2DP is used for Audio stream.
 * We scan first with the Low Energy Scanner as it is more efficient than the classical Bluetooth discovery scanner.
 */
class MbtBluetoothWriter(context: Context) : MbtBluetoothManager(context) {


  private fun startSendingExternalName() {
    LogUtils.i(TAG, "start sending QR code if supported")
    requestCurrentConnectedDevice(SimpleRequestCallback<MbtDevice> { device ->
      LogUtils.d(TAG, "device $device")
      updateConnectionState(true) //current state is set to QR_CODE_SENDING
      if ((device.serialNumber != null
              && ((device.externalName == MbtFeatures.MELOMIND_DEVICE_NAME) || device.externalName?.length == MbtFeatures.DEVICE_QR_CODE_LENGTH - 1) //send the QR code found in the database if the headset do not know its own QR code
              && VersionHelper(device.firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.REGISTER_EXTERNAL_NAME))) {
        AsyncUtils.executeAsync {
          val externalName = MelomindsQRDataBase(mContext, false)[device.serialNumber]
          bluetoothForDataStreaming.sendCommand(UpdateExternalName(externalName))
        }
      }
      updateConnectionState(true) //current state is set to CONNECTED in any case (success or failure) the connection process is completed and the SDK consider that everything is ready for any operation (for example ready to acquire EEG data)
    })
    switchToNextConnectionStep()
  }

  /**
   * Command sent from the SDK to the connected headset
   * in order to change its Maximum Transmission Unit
   * (maximum size of the data sent by the headset to the SDK).
   */
  private fun changeMTU() {
    updateConnectionState(true) //current state is set to CHANGING_BT_PARAMETERS
    bluetoothForDataStreaming.sendCommand(Mtu(bluetoothContext.mtu))
  }

}