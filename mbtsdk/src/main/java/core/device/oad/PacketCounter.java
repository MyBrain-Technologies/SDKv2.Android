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

    /**
     * Helper class to use for counting the number of packets sent during a transfer operation such as an OAD firmware update
     * @param packetSize the size of a packet (number of bytes allocated for a single packet)
     * @param nbBytesToSend the number of bytes to send
     */
    PacketCounter(int packetSize, int nbBytesToSend) {
        this.packetSize = packetSize;
        this.reset(nbBytesToSend);
    }

    /**
     * Reset the packet counter.
     * @param nbBytesToSend the number of bytes to send
     */
    void reset(int nbBytesToSend) {
        nbBytes = 0;
        nbPacketSent = 0;
        nbPacketToSend = (short) (
                (nbBytesToSend / packetSize)
                        + ((nbBytesToSend % packetSize) == 0 ?
                        0 : 1));
    }

}
