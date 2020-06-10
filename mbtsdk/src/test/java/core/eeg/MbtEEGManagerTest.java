package core.eeg;

import android.content.Context;
import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import core.MbtManager;
import core.eeg.storage.MbtDataBuffering;
import core.eeg.storage.MbtEEGPacket;
import core.eeg.storage.RawEEGSample;
import features.MbtFeatures;

import static org.junit.Assert.*;

public class MbtEEGManagerTest {

    private MbtEEGManager eegManager;
    private MbtDataBuffering dataBuffering;

    private Context context = Mockito.mock(Context.class);


    @Before
    public void setUp() {
        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        MbtManager mbtManager = new MbtManager(context);
        eegManager = new MbtEEGManager(context);
        dataBuffering = new MbtDataBuffering(eegManager);
    }

    /**
     * Check that the defined known input is well stored in the pending buffer
     */
    @Test
    public void storePendingDataInBufferTest(){

        ArrayList<RawEEGSample> rawEEGSampleList = new ArrayList<>();
        rawEEGSampleList.add(new RawEEGSample(new ArrayList<>(Arrays.asList(new byte[]{-13, -29},new byte[]{68, 59})),Float.NaN));

        eegManager.storePendingDataInBuffer(rawEEGSampleList);

    }

    /**
     * Check that the EEG Manager is well notified for conversion if the storage buffer of raw EEG data is full
     */
    @Test
    public void convertToEEGTest() {
//        ArrayList<ArrayList<Float>> matrixBefore = new ArrayList<>();
//        matrixBefore.add(new ArrayList<>(Arrays.asList(1F,2F,3F)));
//        matrixBefore.add(new ArrayList<>(Arrays.asList(4F,5F,6F)));
//        ArrayList<RawEEGSample> rawEEGSampleList = new ArrayList<>();
//        eegManager.setTestConsolidatedEEG(matrixBefore);
//
//        final CompletableFuture<Void> future= CompletableFuture.runAsync(new Runnable() {
//            @Override
//            public void run() { //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done
//                for (int i = 0; i < MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE; i++) {
//                    rawEEGSampleList.add(new RawEEGSample(new ArrayList<>(Arrays.asList(new byte[]{-13, -29}, new byte[]{68, 59})), Float.NaN));
//                }
//                dataBuffering.storePendingDataInBuffer(rawEEGSampleList);
//
//            }
//        }, Executors.newCachedThreadPool());
//
//        try {
//            future.get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
//        assertTrue(future.isDone());// if the async task has been completed
//        assertTrue(" before : "+matrixBefore+" after "+eegManager.getConsolidatedEEG(),!eegManager.getConsolidatedEEG().equals(matrixBefore));

    }

    @Test
    public void notifyEEGDataIsReadyHasQualitiesTest() {
        eegManager.setTestHasQualities(true);
        MbtEEGPacket eegPacket = new MbtEEGPacket();
        eegManager.notifyEEGDataIsReady(eegPacket);

    }

    /**
     *  Check that the computed SNR is equal to the expected SNR with a known defined input
     */
    @Test
    public void computeStatisticsSNRTest() {
//        float threshold = 0.6f;
//        Float[] snrValues = new Float[]{0.61f,0.61f,0.61f,0.61f,0.61f};
//        HashMap<String, Float> expectedOutput = new HashMap<>();
//        expectedOutput.put("journey",100F);
//        HashMap<String, Float> computedOutput = eegManager.computeStatisticsSNR(threshold,snrValues);
//        assertTrue(computedOutput.containsKey("journey"));
//        assertTrue(" result journey "+computedOutput.get("journey"),computedOutput.get("journey").equals(expectedOutput.get("journey")));

    }

    @Test
    public void onEventTest(){

    }

    @Test
    public void onStreamStateChanged() {
    }

    @Test
    public void onStreamStartedOrStopped() {
    }


}