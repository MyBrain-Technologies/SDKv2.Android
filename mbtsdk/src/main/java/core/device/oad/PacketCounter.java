package core.device.oad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Counter class to use for any action where a buffer of byte packets is handled.
 * This class helps getting the number of packets,
 * the current packets added to the final buffer,
 * and the total number of bytes
 */
public class PacketCounter {

    int totalNbBytes = 0; // Number of bytes of the whole set of packet
    short nbPacketCounted = 0; // Number of packet counted
    short totalNbPackets = 0; // Total number of packet to count

    /**
     * Helper class to use for counting the number of packets sent during a transfer operation such as an OAD firmware update
     * @param packetSize the size of a packet (number of bytes allocated for a single packet)
     * @param totalNbBytes the number of bytes to send
     */
    public PacketCounter(int packetSize, int totalNbBytes) {
        setTotalNbPackets(totalNbBytes, packetSize);
    }

    /**
     * Reset the current position of the packet counter.
     */
    public void reset() {
        totalNbBytes = 0;
        nbPacketCounted = 0;
    }

    /**
     * Set the current position of the packet counter.
     * @param totalNbBytes the number of bytes to send
     * @param packetSize Size in bytes of each packet
     */
    private short setTotalNbPackets(int totalNbBytes, int packetSize){
        return totalNbPackets = (short) (
                (totalNbBytes / packetSize)
                        + ((totalNbBytes % packetSize) == 0 ?
                        0 : 1));
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
    short getIndexOfNextPacketToSend(){
        return nbPacketCounted;
    }

    public int getTotalNbBytes() {
        return totalNbBytes;
    }

    public short getNbPacketCounted() {
        return nbPacketCounted;
    }

    public short getTotalNbPackets() {
        return totalNbPackets;
    }

    public void incrementTotalNbPackets() {
        this.totalNbPackets++;
    }

    void incrementNbPacketsCounted() {
        this.nbPacketCounted++;
    }

    /**
     * Packet index is set back to the requested value
     * @param packetIndex is the new value to set to the number of packet sent
     * @return the index of the next packet to send
     */
    short setNbPacketsCounted(byte[] packetIndex) {
        short packetIndexAsShort = ByteBuffer.wrap(packetIndex).order(ByteOrder.LITTLE_ENDIAN).getShort();
        return this.nbPacketCounted = packetIndexAsShort;
    }

    public void incrementTotalNbBytes(int incrementer) {
        this.totalNbBytes += incrementer;
    }

    public void incrementNbPacketCounted() {
        this.nbPacketCounted ++;
    }
}
