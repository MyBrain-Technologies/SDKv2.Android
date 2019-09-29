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
    private String OADfilepath = StringUtils.EMPTY;

    /**
     * Version of the firmware to install on the connected headset device.
     */
    private byte[] firmwareVersion = null;

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
    String getOADfilepath() {
        return OADfilepath;
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
        return (short) packetsToSend.size();
    }

    /**
     * Set the path of the binary file that holds the firmware to install on the connected headset device.
     */
    void setOADfilepath(String OADfilepath) {
        this.OADfilepath = OADfilepath;
    }

    /**
     * Set the version of the firmware to install on the connected headset device.
     */
    public void setFirmwareVersion(byte[] firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
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
    }
}
