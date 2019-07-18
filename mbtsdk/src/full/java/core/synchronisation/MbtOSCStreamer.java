package core.synchronisation;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import config.SynchronisationConfig;
import utils.LogUtils;


public class MbtOSCStreamer extends IStreamer<OSCMessage> {

    /**
     * OSCPortOut is the class that sends OSC messages
     * to a specific address and port.
     */
    private OSCPortOut oscOut;

    MbtOSCStreamer(SynchronisationConfig config) {
        super(config.streamRawEEG(), config.streamQualities(), config.getFeaturesToStream());

        try {
            oscOut = new OSCPortOut(InetAddress.getByName(
                    config.getIpAddress()),
                    config.getPort());
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void stream(OSCMessage message) {
        try { // Send the messages
            LogUtils.i("Stream", "Address: " + (message).getAddress() + " | Data: " + (message).getArguments());
            oscOut.send((message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected OSCMessage initStreamRequest(ArrayList<Float> dataToStream, String address) {
        OSCMessage message = new OSCMessage(address);
        for (Float argument : dataToStream){
            message.addArgument(argument);
        }
        return message;
    }

    @Override
    protected void sendStartStreamNotification() {
        //todo send boolean true
        super.sendStartStreamNotification();
    }

    @Override
    protected void sendStopStreamNotification() {
        //todo send boolean false
        oscOut = null;
        super.sendStopStreamNotification();
    }
}
