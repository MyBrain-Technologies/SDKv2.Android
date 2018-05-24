package eventbus.events;

public class EEGDataAcquired {

    private byte[] data;

    public EEGDataAcquired(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
