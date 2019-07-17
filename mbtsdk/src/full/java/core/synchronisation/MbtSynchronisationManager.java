package core.synchronisation;

import android.content.Context;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import core.BaseModuleManager;
import core.MbtManager;
import core.eeg.MbtEEGManager;
import core.eeg.storage.MbtEEGPacket;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.SynchronisationEvent;

import static utils.MatrixUtils.invertFloatMatrix;

/**
 * Entry point of the OSC unit
 */
public final class MbtSynchronisationManager extends BaseModuleManager {

    private static final String TAG = MbtSynchronisationManager.class.getName();
    private MbtOSCStreamer oscStreamer;

    public MbtSynchronisationManager(@NonNull Context context, MbtManager mbtManagerController) {
        super(context, mbtManagerController);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void onEvent(@NonNull final SynchronisationEvent.InitEvent event) {
        oscStreamer = new MbtOSCStreamer(event.getSynchronisationConfig());
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
        if(eegPacket != null && invertFloatMatrix(eegPacket.getChannelsData()) != null)
            eegPacket.setChannelsData(invertFloatMatrix(eegPacket.getChannelsData()));
        oscStreamer.execute(eegPacket);
    }
}
