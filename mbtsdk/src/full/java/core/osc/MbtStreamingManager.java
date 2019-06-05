package core.osc;

import android.content.Context;
import android.support.annotation.NonNull;

import com.illposed.osc.OSCPortOut;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import core.BaseModuleManager;
import core.MbtManager;
import core.eeg.MbtEEGManager;
import core.eeg.storage.MbtEEGPacket;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.OscEvent;

import static utils.MatrixUtils.invertFloatMatrix;

public final class MbtStreamingManager extends BaseModuleManager {

    private static final String TAG = MbtStreamingManager.class.getName();

    private OSCPortOut oscPortOut;

    public MbtStreamingManager(@NonNull Context context, MbtManager mbtManagerController) {
        super(context, mbtManagerController);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void onEvent(@NonNull final OscEvent.InitEvent event) {
        try {
            oscPortOut = new OSCPortOut(InetAddress.getByName(
                    event.getOscConfig().getIpAddress()),
                    event.getOscConfig().getPort());
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }
    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link MbtEEGManager}:
     * this manager handles EEG data acquired by the headset
     * Creates a new MbtEEGPacket instance when the raw buffer contains enough data
     * @param event contains data transmitted by the publisher : here it contains the converted EEG data matrix, the status, the number of acquisition channels and the sampling rate
     */
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void onEvent(@NonNull final ClientReadyEEGEvent event) {
        MbtEEGPacket eegPacket = event.getEegPackets();
        if(oscPortOut != null && eegPacket != null){
            if (invertFloatMatrix(eegPacket.getChannelsData()) != null)
                eegPacket.setChannelsData(invertFloatMatrix(eegPacket.getChannelsData()));
            new OSCAsyncTask(oscPortOut).execute(eegPacket);
            new OSCTupleAsyncTask(oscPortOut).execute(eegPacket);
        }
    }

}
