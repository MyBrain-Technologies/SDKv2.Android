package core.synchronisation;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import config.SynchronisationConfig;
import utils.LogUtils;

/**
 * Open Sound Control (OSC)
 * is a simple content format, although it is often though of as a protocol for the transmission of data over a network.
 * It can be used as a replacement for MIDI - as a network-protocol for the exchange of musical control data between soft- and hardware over a UDP/IP network
 */
public class MbtOSCProcessor extends AbstractStreamer<OSCMessage, OSCPortOut, SynchronisationConfig.OSC> {

    private static final String TAG = MbtOSCProcessor.class.getName();

    /**
     * OSCPortOut is the class that sends OSC messages
     * to a specific address and port.
     */

    MbtOSCProcessor(SynchronisationConfig.OSC config) {
        super(config);
    }

    @Override
    void initStreamer(SynchronisationConfig.OSC config) {
        try {
            LogUtils.i("Init", "Address: " + config.getIpAddress() + " | Port: " +  config.getPort());

            streamer = new OSCPortOut(InetAddress.getByName(
                    config.getIpAddress()),
                    config.getPort());
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stream(OSCMessage message) {
        try { // Send the messages
            if (streamer != null){
                streamer.send((message));
                LogUtils.i("Stream", "Address: " + (message).getAddress() + " | Data: " + (message).getArguments());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public OSCMessage initStreamRequest(String address, Object... dataToStream) {

        OSCMessage message = new OSCMessage(address);
        for (Object argument : dataToStream){
            if(argument instanceof ArrayList){
                for (Object value : (ArrayList)argument){
                    message.addArgument(value);
                }
            }else
                message.addArgument(dataToStream);
        }
        return message;
    }

}
