package core.eeg.signalprocessing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import core.eeg.storage.MbtEEGPacket;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MBTComputeRelaxIndexTest {

    /**
     * check that IllegalArgumentException is raised if calibparams has empty parameters
     */
    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexCalibParamsEmptyTest() {
        int sampRate = 250;
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(new HashMap<>());
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>(Arrays.asList(1f,2f,3f)));
        channelsData.add(new ArrayList<>(Arrays.asList(4f,5f,6f)));
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    /**
     * check that IllegalArgumentException is raised if calibparams is null
     */
    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexCalibParamsNullTest() {
        final int sampRate = 250;
        final MBTCalibrationParameters calibParams =  null ;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>(Arrays.asList(1f,2f,3f)));
        channelsData.add(new ArrayList<>(Arrays.asList(4f,5f,6f)));
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    /**
     * check that IllegalArgumentException is raised if EEG packets is null
     */
    @Test (expected = NullPointerException.class)
    public void computeRelaxIndexPacketsNullTest() {
        final int sampRate = 250;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("example",new float[]{1f});
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final MbtEEGPacket packets = null;
        MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    /**
     * check that IllegalArgumentException is raised if EEG packets is empty (no EEG packets in input)
     */
    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexPacketsEmptyTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams);
    }

    /**
     * check that IllegalArgumentException is raised if sampRate is negative
     */
    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexNegativeSampRateTest() {
        final int sampRate = -1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }


    /**
     * check that computed relax index has a relevant value and is equal to the expected output
     */
    @Test
    public void computeRelaxIndexGoodQualitiesTest() {
        final int sampRate = 2;
        float expectedOutput = 0.5f;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example", new float[]{1f});
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>(Arrays.asList(1f,2f,3f)));
        channelsData.add(new ArrayList<>(Arrays.asList(4f,5f,6f)));
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
        assertTrue(relaxIndex > 0);
        assertTrue( relaxIndex < 1);
        assertTrue(" relax index= "+relaxIndex,relaxIndex == expectedOutput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeRelaxIndexEmptyChannelsTest() {
        final int sampRate = 1;
        HashMap<String, float[]> params = new HashMap<>();
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        assertTrue(calibParams.getSize() ==  0);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>(); //check that exception is raised if channelsdata is empty
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }


    @Test
    public void getSessionMetadataTest() {
        Map<String, float[]> expectedOutput = new HashMap<>();
        expectedOutput.put("rawRelaxIndexes",new float[]{1.2f});
        expectedOutput.put("smoothedRelaxIndexes",new float[]{1f});
        expectedOutput.put("histFrequencies",new float[]{5f});
        Map<String, float[]> computedOutput = MBTComputeRelaxIndex.getSessionMetadata();
        assertNotNull(computedOutput);  //check that the returned map is not null, not empty and contains the raw RI, smoothed RI  and histFrequencies
        assertFalse(computedOutput.isEmpty());
        assertTrue(computedOutput.containsKey("rawRelaxIndexes"));
        assertNotNull(computedOutput.get("rawRelaxIndexes"));
        assertTrue(computedOutput.containsKey("smoothedRelaxIndexes"));
        assertNotNull(computedOutput.get("smoothedRelaxIndexes"));
        assertTrue(computedOutput.containsKey("histFrequencies"));
        assertNotNull(computedOutput.get("histFrequencies"));
    }

    @Test
    public void reinitRelaxIndexVariablesTest() {
        Map<String, float[]> expectedOutput = new HashMap<>();
        expectedOutput.put("rawRelaxIndexes",new float[]{});
        expectedOutput.put("smoothedRelaxIndexes",new float[]{});
        expectedOutput.put("histFrequencies",new float[]{});

        MBTComputeRelaxIndex.reinitRelaxIndexVariables();
        Map<String, float[]> computedOutput = MBTComputeRelaxIndex.getSessionMetadata();
        assertTrue(computedOutput.get("rawRelaxIndexes").length==0); // check that the pastRelaxIndex has been cleared
        assertTrue(computedOutput.get("smoothedRelaxIndexes").length==0);// check that the smoothedRelaxIndex has been cleared
        assertTrue(computedOutput.get("histFrequencies").length==0);// check that the histFreq has been cleared
        assertTrue(computedOutput.equals(expectedOutput));
    }
}