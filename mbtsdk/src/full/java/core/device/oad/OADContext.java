package core.device.oad;

class OADContext {

    private String filePath;
    private String firmwareVersion;
    private int nbBytesToSend;

    public OADContext() {
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public int getNbBytesToSend() {
        return nbBytesToSend;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public void setNbBytesToSend(int nbPacketToSend) {
        this.nbBytesToSend = nbPacketToSend;
    }
}
