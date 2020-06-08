package command

import android.util.Log
import androidx.annotation.Keep
import command.CommandInterface.MbtCommand
import engine.clientevents.BaseError
import engine.clientevents.BaseErrorEvent
import engine.clientevents.ConfigError

/**
 * Created by Etienne on 08/02/2018.
 */
@Keep
interface CommandInterface<E : BaseError?> : BaseErrorEvent<E> {
  /**
   * A MbtRequest implementation object is a request
   * that is sent in suitables conditions to define when you extend this interface.
   * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
   */
  @Keep
  interface MbtRequest {
    fun onRequestSent(request: MbtCommand<*>?)
  }

  @Keep
  interface MbtResponse<N> {
    fun onResponseReceived(request: MbtCommand<*>?, response: N?)
  }

  @Keep
  interface CommandBaseErrorEvent {
    fun onError(request: MbtCommand<*>?, error: BaseError?, additionalInfo: String?)
  }

  /**
   * A ICommandCallback implementation object is a request
   * that is sent in suitables conditions to define when you extend this interface.
   * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
   * In case of failure, the onError callback is triggered to return the info associated to the failure
   */
  @Keep
  interface ICommandCallback : MbtRequest, CommandBaseErrorEvent

  /**
   * A CommandCallback implementation object is a request
   * that is sent in suitables conditions to define when you extend this interface.
   * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
   * In case of failure, the onError callback is triggered to return the info associated to the failure
   */
  @Keep
  interface SimpleCommandCallback : ICommandCallback

  /**
   * A CommandCallback implementation object is a request
   * that is sent in suitables conditions to define when you extend this interface.
   * It means that the request can fail to be sent to a receiver (peripheral object/device/class).
   * In case of failure, the onError callback is triggered to return the info associated to the failure
   * If it succeeded to be sent, the MbtRequest implementation object
   * receives in return a response sent by the receiver
   * @param <N> the response Object type returned by the receiver
  </N> */
  @Keep
  interface CommandCallback<N> : ICommandCallback, MbtResponse<N>

  /**
   * Command abstact class that holds a callback used to
   * perform a request and notify the client when
   * the request has been sent
   * If the client is interested in getting the response associated to the request,
   * it also notify the client when this response is caught by the SDK.
   * A Command can be seen as a package that hold the request and its associated optional response.
   * @param <E> the expected Error Object
  </E> */
  @Keep
  abstract class MbtCommand<E : BaseError> {
    /**
     * Get the callback that returns the raw response of the headset to the SDK
     * @return the callback that returns the raw response of the headset to the SDK
     */
    @JvmField
    var commandCallback: ICommandCallback? = null

    fun onError(error: E, additionalInfo: String?) {
      Log.d(TAG, "Error: $error")
      if (commandCallback != null) commandCallback!!.onError(this, error, additionalInfo)
    }

    fun onRequestSent() {
      Log.d(TAG, "Command sent $this")
      if (commandCallback != null) commandCallback!!.onRequestSent(this)
    }

    open fun onResponseReceived(response: Any?) {
      Log.d(TAG, "Response received $this")
      if (commandCallback != null && commandCallback is CommandCallback<*>) (commandCallback as CommandCallback<Any>).onResponseReceived(this, response)
    }

    val isResponseExpected: Boolean
      get() = commandCallback is CommandCallback<*>
    /**
     * Init the command to send to the headset
     * Init a command callback that handle responses if
     * @param responseExpected is true.
     * No response can be retrieved in the onResponseReceived callback if responseExcepted is false.
     */
    /**
     * Init the command to send to the headset
     */
    protected fun init() {
      init(true)
    }
    protected fun init(responseExpected: Boolean = true) {
      if (commandCallback == null) {
        commandCallback = if (responseExpected) //if a response is expected once the request is sent, we use a CommandCallback object (other object that extend MbtResponse)
          object : CommandCallback<Any?> {
            override fun onError(request: MbtCommand<*>?, error: BaseError?, additionalInfo: String?) {}
            override fun onRequestSent(request: MbtCommand<*>?) {}
            override fun onResponseReceived(request: MbtCommand<*>?, response: Any?) {}
          } else object : ICommandCallback {
          //if no response is expected once the request is sent, we use a ICommandCallback object (other object that does not extend MbtResponse)
          override fun onError(request: MbtCommand<*>?, error: BaseError?, additionalInfo: String?) {}
          override fun onRequestSent(request: MbtCommand<*>?) {}
        }
      }
      if (!isValid) commandCallback!!.onError(this, ConfigError.ERROR_INVALID_PARAMS, invalidityError)
    }

    /**
     * Returns true if the client inputs
     * are valid for sending the command
     */
    abstract val isValid: Boolean

    /**
     * Returns a String message that contain the reason of invalidity input.
     * Returns null if the input is valid (isValid returns true)
     * @return the reason of invalidity input as a String
     */
    abstract val invalidityError: String?

    /**
     * Bundles the data to send to the headset
     * for the write characteristic operation / request
     * @return the bundled data in a object
     */
    abstract fun serialize(): Any?

    companion object {
      private val TAG = MbtCommand::class.java.name
    }
  }
}