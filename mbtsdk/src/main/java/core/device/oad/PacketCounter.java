package core.device.oad;

/**
 * Counter class to use for any action where a buffer of byte packets is handled.
 * This class helps getting the number of packets,
 * the current packets added to the final buffer,
 * and the total number of bytes
 */
public class PacketCounter {

    /**
     * Number of packet counted
     */
    short nbPacketCounted = -1; // Number of packet counted

    /**
     * Total number of packet to count
     */
    final short totalNbPackets; // Total number of packet to count

    /**
     * Helper class to use for counting the number of packets sent during a transfer operation such as an OAD firmware update
     * @param totalNbPackets the number of packets to count
     */
    public PacketCounter(short totalNbPackets) {
        this.totalNbPackets = totalNbPackets;
    }

    /**
     * Reset the current position of the packet counter.
     */
    public void reset() {
        nbPacketCounted = 0;
    }

    /**
     * Packet index is set back to the requested value
     * @param packetIndex is the new value to set to the number of packet sent
     */
    void resetTo(short packetIndex) {
        this.nbPacketCounted = packetIndex;
    }

    /**
     * Returns true if the number of packets sent is equal to the number of packet to send, false otherwise.
     * @return true if the number of packets sent is equal to the number of packet to send, false otherwise.
     */
    boolean areAllPacketsCounted(){
        return this.nbPacketCounted == this.totalNbPackets;
    }

    /**
     * Return the index of the next packet to send
     * @return the index of the next packet to send
     */
    public short getIndexOfNextPacket(){
        return ++nbPacketCounted;
    }

    /**
     * Return the total number of packet to count
     */
    public short getTotalNbPackets() {
        return totalNbPackets;
    }

    public void incrementNbPacketCounted() {
        this.nbPacketCounted ++;
    }
}
