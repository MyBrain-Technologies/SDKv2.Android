package core.device.oad;


import org.apache.commons.lang.StringUtils;

/**
 * Context of an OAD update that stores values related to the firmware to install.
 */

class OADContext {

    /**
     * Path of the binary file that holds the firmware to install on the connected headset device.
     */
    private String OADfilePath = StringUtils.EMPTY;

    /**
     * Version of the firmware to install on the connected headset device.
     */
    private FirmwareVersion firmwareVersion = null;

    /**
     * Number of packets to send to the connected headset device during the firmware binary file transfer.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    private int nbPacketsToSend = 0;

    /**
     * Empty constructor that create a new instance of the {@link OADContext} object.
     */
    public OADContext() { }

    /**
     * Return the path of the binary file that holds the firmware to install on the connected headset device.
     */
    public String getOADfilePath() {
        return OADfilePath;
    }

    /**
     * Return the version of the firmware to install on the connected headset device.
     */
    public FirmwareVersion getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Return the number of packets to send to the connected headset device during the firmware binary file transfer.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    public int getNbPacketsToSend() {
        return nbPacketsToSend;
    }

    /**
     * Set the path of the binary file that holds the firmware to install on the connected headset device.
     */
    public void setOADfilePath(String OADfilePath) {
        this.OADfilePath = OADfilePath;
    }

    /**
     * Set the version of the firmware to install on the connected headset device.
     */
    public void setFirmwareVersion(FirmwareVersion firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Set the number of packets to send to the connected headset device during the firmware binary file transfer.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    public void setNbPacketsToSend(int nbPacketToSend) {
        this.nbPacketsToSend = nbPacketToSend;
    }
}
