package core.device.oad;

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

    public boolean areAllPacketsSent(){
        return this.nbPacketSent == this.nbPacketToSend;
    }

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

    public void incrementNbPacketsSent() {
        this.nbPacketToSend++;
    }

    public void incrementNbBytes(int incrementer) {
        this.nbBytes += incrementer;
    }
}
