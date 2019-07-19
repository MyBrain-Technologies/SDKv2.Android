package core.device.oad;

/**
 * Engine class to use for any action where a buffer of byte packets is handled.
 * This class helps getting the number of packets,
 * the current packets added to the final buffer,
 * and the total number of bytes
 */
class PacketCounter {

    /**
     * Size in bytes of each packet
     */
    private final int packetSize;

    int nbBytes = 0; // Number of bytes programmed
    short nbPacketSent = 0; // Number of packet sent
    short nbPacketToSend = 0; // Total number of packet to send

    PacketCounter(int packetSize, int fileLength) {
        this.packetSize = packetSize;
        this.reset(fileLength);
    }

    /**
     * Reset the packet counter.
     * @param fileLength the number of bytes to send
     */
    void reset(int fileLength) {
        nbBytes = 0;
        nbPacketSent = 0;
        nbPacketToSend = (short) (
                (fileLength / packetSize)
                        + ((fileLength % packetSize) == 0 ?
                        0 : 1));
    }

}
