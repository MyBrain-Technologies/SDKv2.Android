package core.device.oad;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

import core.device.model.FirmwareVersion;

/**
 * Context of an OAD update that stores values related to the firmware to install.
 */

class OADContext {

    /**
     * Path of the binary file that holds the firmware to install on the connected headset device.
     */
    private String OADfileName = StringUtils.EMPTY;

    /**
     * Version of the firmware to install on the connected headset device.
     */
    private byte[] firmwareVersion = null;

    /**
     * Number of packets to send to the connected headset device during the firmware binary file transfer.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    private short nbPacketsToSend = 0;

    /**
     * List of OAD packets that holds chunks of the OAD
     * binary file that holds the firmware to install on the connected headset device.
     */
    private ArrayList<byte[]> packetsToSend;

    /**
     * Empty constructor that create a new instance of the {@link OADContext} object.
     */
    OADContext() { }

    /**
     * Return the path of the binary file that holds the firmware to install on the connected headset device.
     */
    String getOADfileName() {
        return OADfileName;
    }

    /**
     * Return the version of the firmware to install on the connected headset device.
     */
    public byte[] getFirmwareVersionAsByteArray() {
        return firmwareVersion;
    }

    /**
     * Return the version of the firmware to install on the connected headset device.
     */
    public FirmwareVersion getFirmwareVersion() {
        return new FirmwareVersion(new String(firmwareVersion));
    }

    /**
     * Return the number of packets to send to the connected headset device during the firmware binary file transfer.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    short getNbPacketsToSend() {
        return nbPacketsToSend;
    }

    /**
     * Set the path of the binary file that holds the firmware to install on the connected headset device.
     */
    void setOADfileName(String OADfilePath) {
        this.OADfileName = OADfilePath;
    }

    /**
     * Set the version of the firmware to install on the connected headset device.
     */
    public void setFirmwareVersion(byte[] firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Set the number of packets to send to the connected headset device during the firmware binary file transfer.
     * As the OAD binary file that holds the new firmware is too big to be sent in a single request,
     * the file is chunked into small packets.
     */
    void setNbPacketsToSend(short nbPacketToSend) {
        this.nbPacketsToSend = nbPacketToSend;
    }

    /**
     * Returns the list of OAD packets that holds chunks of the OAD
     * binary file that holds the firmware to install on the connected headset device.
     */
    ArrayList<byte[]> getPacketsToSend() {
        return packetsToSend;
    }

    /**
     * Set the list of OAD packets that holds chunks of the OAD
     * binary file that holds the firmware to install on the connected headset device.
     */
    void setPacketsToSend(ArrayList<byte[]> packetsToSend) {
        this.packetsToSend = packetsToSend;
        this.nbPacketsToSend = (short) packetsToSend.size();
    }
}
