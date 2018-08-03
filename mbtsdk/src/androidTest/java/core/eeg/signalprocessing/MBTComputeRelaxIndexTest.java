package core.eeg.signalprocessing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import core.eeg.storage.MbtEEGPacket;

import static org.mockito.Mockito.mock;

public class MBTComputeRelaxIndexTest {

    // /!\ Problem : UnsatisfiedLinkError is raised if sampRate = 0 only so we cannot make this test /!\
     /*  @Test

    public void computeRelaxIndexCalibParamsEmptyTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[0]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);  //check that IllegalArgumentException is raised if calibparams is empty
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        qualities.add(1F);
        long timestamp = 1L;
        //final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        //float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexCalibParamsNullTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  null ;  //check that IllegalArgumentException is raised if calibparams is null
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = NullPointerException.class)
    public void computeRelaxIndexPacketsNullTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = null;  //check that IllegalArgumentException is raised if packets is null
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexPacketsEmptyTest() {
        final int sampRate = 0;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket[] packets = new MbtEEGPacket[0];  //check that IllegalArgumentException is raised if packets is empty
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeRelaxIndexNegativeSampRateTest() {
        final int sampRate = -1;  //check that IllegalArgumentException is raised if sampRate is negative
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    // /!\ Problem : UnsatisfiedLinkError is raised /!\
    @Test
    public void computeRelaxIndexGoodQualitiesTest() {
        final int sampRate = 2;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example", new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        ArrayList<Float> arrayTest = new ArrayList<Float>();
        arrayTest.add(1F);
        arrayTest.add(1F);
        channelsData.add(arrayTest);
        channelsData.add(arrayTest);
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        //float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
        //assertTrue(relaxIndex > 0 && relaxIndex < 1); //check that relax index has a relevant value
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void computeRelaxIndexEmptyQualitiesTest() {
        final int sampRate = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example", new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void computeRelaxIndexEmptyChannelsTest() {
        final int sampRate = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example", new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>(); //check that exception is raised if channelsdata is empty
        ArrayList<Float> qualities = new ArrayList<Float>();
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        float relaxIndex = MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }*/

    // /!\ Problem : UnsatisfiedLinkError is raised /!\
    /*@Test
    public void getSessionMetadataTest() {
        Map<String, float[]> map = MBTComputeRelaxIndex.getSessionMetadata();
        assertNotNull(map);  //check that the returned map is not null, not empty and contains the raw RI, smoothed RI  and histFrequencies
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("rawRelaxIndexes"));
        assertNotNull(map.get("rawRelaxIndexes"));
        assertTrue(map.containsKey("smoothedRelaxIndexes"));
        assertNotNull(map.get("smoothedRelaxIndexes"));
        assertTrue(map.containsKey("histFrequencies"));
        assertNotNull(map.get("histFrequencies"));
    }

    @Test
    public void reinitRelaxIndexVariablesTest() {
        MBTComputeRelaxIndex.reinitRelaxIndexVariables();
        Map<String, float[]> map = MBTComputeRelaxIndex.getSessionMetadata();
        assertTrue(map.get("rawRelaxIndexes").length==0); // check that the pastRelaxIndex has been cleared
        assertTrue(map.get("smoothedRelaxIndexes").length==0);// check that the smoothedRelaxIndex has been cleared
        assertTrue(map.get("histFrequencies").length==0);// check that the histFreq has been cleared
    }*/
}