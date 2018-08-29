package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Random;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.storage.RawEEGSample;

import static org.junit.Assert.*;

public class MbtEEGManagerTest {

    private MbtEEGManager eegManager;

    private Context context = Mockito.mock(Context.class);


    @Before
    public void setUp() throws Exception{
        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        MbtManager mbtManager = new MbtManager(context);
        eegManager = new MbtEEGManager(context,mbtManager, BtProtocol.BLUETOOTH_LE);
    }

    @Test
    public void storePendingDataInBufferTestSizeBuffer() { //check result if buffer size is reached
        byte[] eegBytes = new byte[250];
        new Random().nextBytes(eegBytes);

        ArrayList<byte[]> eegBytesList = new ArrayList<>();
        eegBytesList.add(eegBytes);

        ArrayList<RawEEGSample> eegList = new ArrayList();
        eegList.add(new RawEEGSample(eegBytesList));

        eegManager.storePendingDataInBuffer(eegList);
    }

    @Test
    public void convertToEEG() {
    }

    @Test
    public void notifyEEGDataIsReady() {
    }

    @Test
    public void computeStatistics() {
    }

    @Test
    public void computeStatisticsSNR() {
    }

    @Test
    public void getConsolidatedEEG() {
    }

    @Test
    public void deinit() {
    }

    @Test
    public void onEvent() {
    }

    @Test
    public void onStreamStateChanged() {
    }

    @Test
    public void onStreamStartedOrStopped() {
    }
}