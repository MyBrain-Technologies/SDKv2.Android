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

       @Test

    public void computeRelaxIndexCalibParamsEmptyTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[0]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);  //check that IllegalArgumentException is raised if calibparams is empty
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        qualities.add(1F);
        //final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        //float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexCalibParamsNullTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  null ;  //check that IllegalArgumentException is raised if calibparams is null
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        qualities.add(1F);
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = NullPointerException.class)
    public void computeRelaxIndexPacketsNullTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        final MbtEEGPacket packets = null;  //check that IllegalArgumentException is raised if packets is null
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

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
        final MbtEEGPacket[] packets = new MbtEEGPacket[0];  //check that IllegalArgumentException is raised if packets is empty
        MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexNegativeSampRateTest() {
        final int sampRate = -1;  //check that IllegalArgumentException is raised if sampRate is negative
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

    @Test(expected = IndexOutOfBoundsException.class)
    public void computeRelaxIndexEmptyQualitiesTest() {
        final int sampRate = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example", new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void computeRelaxIndexEmptyChannelsTest() {
        final int sampRate = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example", new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>(); //check that exception is raised if channelsdata is empty
        ArrayList<Float> qualities = new ArrayList<>();
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
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
    public void resetRelaxIndexVariablesTest() {
        Map<String, float[]> expectedOutput = new HashMap<>();
        expectedOutput.put("rawRelaxIndexes",new float[]{});
        expectedOutput.put("smoothedRelaxIndexes",new float[]{});
        expectedOutput.put("histFrequencies",new float[]{});

        MBTComputeRelaxIndex.resetRelaxIndexVariables();
        Map<String, float[]> computedOutput = MBTComputeRelaxIndex.getSessionMetadata();
        assertTrue(computedOutput.get("rawRelaxIndexes").length==0); // check that the pastRelaxIndex has been cleared
        assertTrue(computedOutput.get("smoothedRelaxIndexes").length==0);// check that the smoothedRelaxIndex has been cleared
        assertTrue(computedOutput.get("histFrequencies").length==0);// check that the histFreq has been cleared
        assertTrue(computedOutput.equals(expectedOutput));
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
}