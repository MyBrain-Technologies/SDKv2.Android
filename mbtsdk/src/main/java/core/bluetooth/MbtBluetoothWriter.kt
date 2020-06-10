package core.bluetooth

import command.DeviceCommands.UpdateExternalName
import core.bluetooth.requests.CommandRequestEvent
import core.device.model.MbtDevice
import core.device.model.MelomindsQRDataBase
import features.MbtFeatures
import utils.AsyncUtils
import utils.LogUtils
import utils.VersionHelper

/** Created by Sophie on 02/06/2020.
 * This class contains all necessary methods to manage the Bluetooth write operation with the myBrain peripheral devices.
 * A write operation is any value update/change request sent to the headset device
 */
class MbtBluetoothWriter(private val manager: MbtBluetoothManager) {


  companion object {
    private val TAG = MbtBluetoothWriter::class.java.simpleName
  }

  private fun MbtDevice.shouldSendExternalName(): Boolean {
    return (serialNumber != null
        && (externalName == MbtFeatures.MELOMIND_DEVICE_NAME
        || externalName?.length == MbtFeatures.DEVICE_QR_CODE_LENGTH - 1) //send the QR code found in the database if the headset do not know its own QR code
        && VersionHelper(firmwareVersion.toString()).isValidForFeature(VersionHelper.Feature.REGISTER_EXTERNAL_NAME))
  }

  private fun MbtDevice.findExternalName(): String? {
    val bluetoothContext = manager.context
    return MelomindsQRDataBase(bluetoothContext.context, false)[serialNumber]
  }

  fun startSendingExternalName(device: MbtDevice) {
    LogUtils.i(TAG, "start sending QR code if supported for device $device")
    if (device.shouldSendExternalName()) {
      AsyncUtils.executeAsync {
        val externalName = device.findExternalName()
        val request = CommandRequestEvent(UpdateExternalName(externalName))
        manager.parseRequest(request)
      }
    }
  }

}