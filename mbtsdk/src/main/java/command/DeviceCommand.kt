package command

import androidx.annotation.Keep
import command.CommandInterface.MbtCommand
import engine.clientevents.BaseError
import java.nio.ByteBuffer

/**
 * Mailbox command sent from the SDK to the headset
 * in order to configure a parameter,
 * or get values stored by the headset
 * or ask the headset to perform an action.
 * It provides a callback used to return a raw response sent by the headset to the SDK
 */
@Keep
abstract class DeviceCommand<T, U : BaseError> internal constructor(
    /**
     * Unique identifier of the command.
     * This code is sent to the headset in the write characteristic operation.
     */
    var identifier: DeviceCommandEvent?) : MbtCommand<U>() {
  
  companion object {
    const val ENABLE: Byte = 0x01
    const val DISABLE: Byte = 0x00
  }
  
  /**
   * Optional header codes
   */
  private var headerCodes: ByteArray? = null

  /**
   * Buffer that hold the identifier code,
   * additional codes
   * and all the data specific to the implemented class
   */
  private var rawDataBuffer: ByteBuffer? = null

  /**
   * Change the unique identifier of the command
   */
  fun setCommandEvent(commandEvent: DeviceCommandEvent?) {
    identifier = commandEvent
  }

  /**
   * Allocate a buffer that bundle all the data to send to the headset
   * for the write characteristic operation
   */
  private fun allocateBuffer() {
    rawDataBuffer = null //reset the temporary buffer
    var bufferSize = 0 //the buffer contains at least the identifier device command identifier code

    if (headerCodes != null) bufferSize += headerCodes!!.size
    if (identifier != null) bufferSize += 1
    if (identifier != null && identifier?.additionalCodes != null) bufferSize += identifier!!.additionalCodes.size
    if (data != null) bufferSize += data!!.size//get data returns the optional data specific to the implemented class

    rawDataBuffer = ByteBuffer.allocate(bufferSize)
  }

  /**
   * Add the identifier code and the additional codes
   * to the raw data buffer to send to the headset
   */
  private fun fillHeader() {
    if (headerCodes != null) for (singleCode in headerCodes!!) {
      rawDataBuffer?.put(singleCode)
    }
    if (identifier != null) {
      rawDataBuffer?.put(identifier!!.identifierCode)
      if (identifier?.additionalCodes != null) for (singleCode in identifier!!.additionalCodes) {
        rawDataBuffer?.put(singleCode)
      }
    }
  }

  /**
   * Add the optional data specific to the implemented class
   * to the raw data buffer to send to the headset
   * @return the complete buffer (identifier + additional codes + optional data)
   */
  private fun fillPayload(): T {
    if (data != null) {
      for (singleData in data!!) {
        rawDataBuffer?.put(singleData)
      }
    }
    return rawDataBuffer?.array() as T
  }

  /**
   * Bundles the data to send to the headset
   * for the write characteristic operation / request
   * @return the bundled data in a object
   */
  override fun serialize(): T {
    allocateBuffer()
    fillHeader()
    return fillPayload()
  }

  /**
   * Returns the optional data specific to the implemented class
   * @return the optional data specific to the implemented class
   */
  abstract /*T*/ val data: ByteArray?

}