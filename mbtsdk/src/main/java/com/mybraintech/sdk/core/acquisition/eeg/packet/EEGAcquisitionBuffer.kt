package com.mybraintech.sdk.core.acquisition.eeg.packet

import com.mybraintech.sdk.core.acquisition.shared.RawPacketBuffer

class EEGAcquisitionBuffer(
  private var bufferSizeMaxValue: Int,
  private val lastIndex: Short = -1
) {
  //----------------------------------------------------------------------------
  // MARK: - Properties
  //----------------------------------------------------------------------------

  private var previousIndex: Short = lastIndex

  private var packetBuffer: RawPacketBuffer =
    RawPacketBuffer(bufferSizeMaxValue)

  var bufferSizeMax: Int
  get() {
    return packetBuffer.bufferSizeMax
  }
  set(value) {
    packetBuffer.bufferSizeMax = value
  }


  //----------------------------------------------------------------------------
  // MARK: - Add a packet to the buffer
  //----------------------------------------------------------------------------

  /// Add a packet to the buffer. Missing packets are filled with 0xFF.
  fun add(data: ByteArray) {
    if (data.isEmpty()) { return }

    val packetValue = EEGRawPacket(data)
    add(packetValue)
  }

//  /// Add a packet to the buffer. Missing packets are filled with 0xFF.
//  func add(rawPacketValue: [UInt8]) {
//    let packetValue = EEGRawPacket(rawValue: rawPacketValue)
//    add(rawPacket: packetValue)
//  }

  /// Add a packet to the buffer. Missing packets are filled with 0xFF.
  fun add(rawPacket: EEGRawPacket) {
//    log.verbose(rawPacket)
    addMissingPackets(rawPacket)
    packetBuffer.add(rawPacket.packetValues.toTypedArray())
  }

  //----------------------------------------------------------------------------
  // MARK: - Usable packets
  //----------------------------------------------------------------------------

  /// Return packets that can be used if the buffer is full, else nil if
  /// the packet is not full yet.
  fun getUsablePackets(): ByteArray? {
    if (!packetBuffer.isFull) { return null }
    return packetBuffer.flushBuffer().toByteArray()
  }

  //----------------------------------------------------------------------------
  // MARK: - Missing packets
  //----------------------------------------------------------------------------

  /// Add missing packets between a packet and the last registered packet
  private fun addMissingPackets(packet: EEGRawPacket) {
    val missingPacketCount = numberOfLostPackets(packet)

    if (missingPacketCount > 0) { return }

//    log.verbose("Lost \(missingPackets) packets")
    // TODO: Anh Tuan to check
    val placeholderValue = "FF".toInt(16).toByte() // 0xFF
    val count = missingPacketCount * packet.packetValuesLength
    packetBuffer.add(placeholderValue, count)
  }

  /// Return the number of packets missing between a packet and the last
  /// registered packet
  private fun numberOfLostPackets(packet: EEGRawPacket): Int { // Need a Int32
    if (packet.packetIndex == 0.toShort()) {
      previousIndex = 0
    }

    if (previousIndex == (-1).toShort()) {
      previousIndex = (packet.packetIndex - 1).toShort()
    }

    /// When packet.packetIndex = Int16.min
    if (previousIndex >= Short.MAX_VALUE) {
      previousIndex = 0
    }

    val missingPackets = (packet.packetIndex - previousIndex).toInt()

    previousIndex = packet.packetIndex
    //.clamped(min: 0, max: Int16.max)
    if (previousIndex < 0) {
      previousIndex
    }
    if (previousIndex > Short.MAX_VALUE) {
      previousIndex = Short.MAX_VALUE
    }

    return missingPackets - 1
  }
}