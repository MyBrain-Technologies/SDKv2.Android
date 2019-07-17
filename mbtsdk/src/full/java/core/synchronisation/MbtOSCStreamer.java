package core.synchronisation;

import android.util.Log;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import config.SynchronisationConfig;
import utils.LogUtils;


public class MbtOSCStreamer extends IStreamer<OSCMessage> {

    static private OSCPortOut oscPortOut;

    MbtOSCStreamer(SynchronisationConfig config) {
        super(config.streamRawEEG(), config.streamQualities(), config.getFeaturesToStream());

        try {
            oscPortOut = new OSCPortOut(InetAddress.getByName(
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
            oscPortOut.send((message));
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
}
