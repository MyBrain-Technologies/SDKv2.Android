package core.synchronisation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import config.SynchronisationConfig;
import core.BaseModuleManager;
import core.bluetooth.BtState;
import core.bluetooth.requests.StreamRequestEvent;
import core.eeg.MbtEEGManager;
import core.eeg.storage.MbtEEGPacket;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.ConnectionStateEvent;

import static utils.MatrixUtils.invertFloatMatrix;

/**
 * Entry point of the OSC unit
 */
public final class MbtSynchronisationManager extends BaseModuleManager {

    private static final String TAG = MbtSynchronisationManager.class.getName();

    private AbstractStreamer streamer;

    public MbtSynchronisationManager(@NonNull Context context) {
        super(context);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onConnectionStateChanged(ConnectionStateEvent event) {
        if(event.getNewState().equals(BtState.DATA_BT_DISCONNECTED)
        && streamer != null)
            streamer = null;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onStreamRequest(@NonNull final StreamRequestEvent event) {
        if(event.isStart()) {

            if(event.getSynchronisationConfig() != null) {
                if (event.getSynchronisationConfig() instanceof SynchronisationConfig.OSC)
                    streamer = new MbtOSCProcessor((SynchronisationConfig.OSC) event.getSynchronisationConfig());

                else if (event.getSynchronisationConfig() instanceof SynchronisationConfig.LSL)
                    streamer = new MbtLSLProcessor((SynchronisationConfig.LSL)event.getSynchronisationConfig());
            }

        }else if(streamer != null) {
            try {
                Thread.sleep(500); //packets can be received with a small delay so we wait this packets
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            streamer.reset();
        }
    }

    /**
     * onEvent is called by the Event Bus when a ClientReadyEEGEvent event is posted
     * This event is published by {@link MbtEEGManager} and is received here in the Synchronisation manager
     * to stream the data over OSC
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewPackets(@NonNull final ClientReadyEEGEvent event) {

        if(streamer != null)
            streamer.execute(getPacketWithInvertedMatrix(event.getEegPackets()));

    }

    private MbtEEGPacket getPacketWithInvertedMatrix(MbtEEGPacket eegPacket){
        if(eegPacket != null && invertFloatMatrix(eegPacket.getChannelsData()) != null)
            eegPacket.setChannelsData(invertFloatMatrix(eegPacket.getChannelsData()));
        return eegPacket;
    }
}
