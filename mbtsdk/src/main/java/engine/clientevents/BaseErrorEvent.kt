package engine.clientevents

import androidx.annotation.Keep

@Keep
interface BaseErrorEvent<U : BaseError> {
  /**
   * Method called when an error occured during process execution
   * @param error
   */
  fun onError(error: U, additionalInfo: String?)
}