package core.bluetooth.requests;

import core.device.model.MbtDevice;

/**
 * An event class when a connection request is being sent by the user.
 */
public class QRCodeSendingRequestEvent extends BluetoothRequests {

    private MbtDevice device;

    public QRCodeSendingRequestEvent(MbtDevice device){
            this.device = device;
    }

    public MbtDevice getDevice() {
            return device;
    }
}