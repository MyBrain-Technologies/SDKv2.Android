package com.mybraintech.sdk.core.acquisition.shared

import kotlin.math.min

class RawPacketBuffer(
  var bufferSizeMax: Int = 0
) {

  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  private var buffer: MutableList<Byte> = ArrayList<Byte>(0)

  val isFull: Boolean
  get() {
    return buffer.size >= bufferSizeMax
  }

  //----------------------------------------------------------------------------
  // MARK: - Initialization
  //----------------------------------------------------------------------------


//  init(bufferSizeMax: Int) {
//    self.buffer = []
//    self.bufferSizeMax = bufferSizeMax
//  }

  //----------------------------------------------------------------------------
  // MARK: - Methods
  //----------------------------------------------------------------------------

  fun add(bytes: Array<Byte>) {
    buffer.addAll(bytes)
  }

  fun add(value: Byte, count: Int) {
    val content = Array<Byte>(0) { value }
    buffer.addAll(content)
  }

  fun flushBuffer(): Array<Byte> {
    val lowerBound = 0
    val upperBound = min(buffer.size, bufferSizeMax)
    val lastRangeIndex = upperBound - 1
    val range = lowerBound until upperBound

    val content = buffer.subList(0, lastRangeIndex) // buffer.slice(range)


    TODO("How to manage to remove the items from 0 to lastRangeIndex")
    // and keep the rest to the array. For example:
    // var measurements = [1.2, 1.5, 2.9, 1.2, 1.5]
    // measurements.removeSubrange(1..<4)
    // print(measurements)
    // // Prints "[1.2, 1.5]"
//    buffer.removeSubrange(range)
//
//    return content.toTypedArray()
  }
}