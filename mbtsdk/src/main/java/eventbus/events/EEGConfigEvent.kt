package eventbus.events

import android.util.Log
import core.device.model.MbtDevice
import core.device.model.MelomindDevice
import core.device.model.VProDevice
import features.MbtDeviceType
import java.util.*

/**
 * Event posted when a new raw data is transmitted by the headset to the SDK through Bluetooth
 *
 * @author Sophie Zecri on 29/05/2018
 */
class EEGConfigEvent(device: MbtDevice, config: Array<Byte?>) : IEvent {
  val device: MbtDevice

  /**
   * Gets the raw EEG data array acquired
   * @return the raw EEG data array acquired
   */
  val config: MbtDevice.InternalConfig =
      if (device.deviceType == MbtDeviceType.MELOMIND) MelomindDevice.convertRawInternalConfig(config)
      else VProDevice.convertRawInternalConfig(config)

  init {
    Log.d(EEGConfigEvent::class.java.simpleName, "config " + config.contentToString())
    this.device = device
  }
}