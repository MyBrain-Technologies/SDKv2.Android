package core.device.oad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Engine class to use for any action where a buffer of byte packets is handled.
 * This class helps getting the number of packets,
 * the current packets added to the final buffer,
 * and the total number of bytes
 */
public class PacketCounter {

    int nbBytes = 0; // Number of bytes programmed
    short nbPacketSent = 0; // Number of packet sent
    short nbPacketToSend = 0; // Total number of packet to send

    /**
     * Helper class to use for counting the number of packets sent during a transfer operation such as an OAD firmware update
     * @param packetSize the size of a packet (number of bytes allocated for a single packet)
     * @param nbBytesToSend the number of bytes to send
     */
    PacketCounter(int nbBytesToSend, int packetSize) {
        this.reset(nbBytesToSend, packetSize);
    }

    /**
     * Reset the packet counter.
     * @param nbBytesToSend the number of bytes to send
     * @param packetSize Size in bytes of each packet
     */
    public void reset(int nbBytesToSend, int packetSize) {
        nbBytes = 0;
        nbPacketSent = 0;
        nbPacketToSend = (short) (
                (nbBytesToSend / packetSize)
                        + ((nbBytesToSend % packetSize) == 0 ?
                        0 : 1));
    }

    /**
     * Returns true if the number of packets sent is equal to the number of packet to send, false otherwise.
     * @return true if the number of packets sent is equal to the number of packet to send, false otherwise.
     */
    boolean areAllPacketsSent(){
        return this.nbPacketSent == this.nbPacketToSend;
    }

    /**
     * Return the index of the next packet to send
     * @return the index of the next packet to send
     */
    short getIndexOfNextPacketToSend(){
        return nbPacketSent;
    }

    public int getNbBytes() {
        return nbBytes;
    }

    public short getNbPacketSent() {
        return nbPacketSent;
    }

    public short getNbPacketToSend() {
        return nbPacketToSend;
    }

    public void incrementNbPacketsToSend() {
        this.nbPacketToSend++;
    }

    void incrementNbPacketsSent() {
        this.nbPacketSent++;
    }

    /**
     * Packet index is set back to the requested value
     * @param packetIndex is the new value to set to the number of packet sent
     * @return the index of the next packet to send
     */
    short resetNbPacketsSent(byte[] packetIndex) {
        short packetIndexAsShort = ByteBuffer.wrap(packetIndex).order(ByteOrder.LITTLE_ENDIAN).getShort();
        return this.nbPacketSent = packetIndexAsShort;
    }

    public void incrementNbBytes(int incrementer) {
        this.nbBytes += incrementer;
    }
}
