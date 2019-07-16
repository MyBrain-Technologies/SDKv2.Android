package core.device.oad;

/**
 * Engine class to use for any action where a buffer of byte packets is handled.
 * This class helps getting the number of packets,
 * the current packets added to the final buffer,
 * and the total number of bytes
 */
public class PacketCounter {

        public int nbBytes = 0; // Number of bytes programmed
        public short nbPacketSent = 0; // Number of packet sent
        public short nbPacketToSend = 0; // Total number of packet to send

        void reset(int fileLength) {
            nbBytes = 0;
            nbPacketSent = 0;
            nbPacketToSend = (short) (
                    (fileLength / OADManager.OAD_PACKET_SIZE)
                            + ((fileLength % OADManager.OAD_PACKET_SIZE) == 0 ?
                            0 : 1));
        }

}
