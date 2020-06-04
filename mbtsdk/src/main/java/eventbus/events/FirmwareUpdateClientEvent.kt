package eventbus.events

import core.device.oad.OADState
import engine.clientevents.BaseError

/**
 * Event posted when the OAD firmware update state changes or when an error occurs during the update.
 *
 * @author Sophie Zecri on 24/07/2019
 */
class FirmwareUpdateClientEvent : IEvent {
  var error: BaseError? = null
    private set
  var additionalInfo: String? = null
    private set
  var oadState: OADState? = null
    private set
  var oadProgress = UNDEFINED
    private set

  constructor(error: BaseError?, additionalInfo: String?) {
    this.error = error
    this.additionalInfo = additionalInfo
  }

  constructor(oadState: OADState) {
    this.oadState = oadState
    oadProgress = oadState.convertToProgress()
  }

  constructor(oadProgress: Int) {
    this.oadProgress = oadProgress
  }

  override fun toString(): String {
    return "FirmwareUpdateClientEvent{" +
        "error=" + error +
        ", additionalInfo='" + additionalInfo + '\'' +
        ", oadState=" + oadState +
        ", oadProgress=" + oadProgress +
        '}'
  }

  companion object {
    private const val UNDEFINED = -1
  }
}