package core.eeg.signalprocessing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import core.eeg.storage.MbtEEGPacket;

import static org.junit.Assert.*;

public class MBTComputeStatisticsTest {

    /*@Test (expected = IndexOutOfBoundsException.class)
    public void computeStatisticsBadBestChannelTest() {
        final int bestChannel = 2; // should be only 0 or 1 => should raise indexoutofbondsexception
        final int sampRate = 1;
        final int packetLength = 1;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        HashMap<String, Float> hashMap = MBTComputeStatistics.computeStatistics(bestChannel,sampRate,packetLength,packets);
    }*/

    // /!\ Problem : UnsatisfiedLinkError is raised if sampRate = 0 only so we cannot make this test /!\
    /*@Test
    public void computeStatisticsZeroSampRate() {


        final int bestChannel = 0;
        final int sampRate = 0; //if sampRate = 0, mainMatrix stays empty => bestChannelData is empty
        final int packetLength = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final MBTCalibrationParameters calibParams =  new MBTCalibrationParameters(params);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        //HashMap<String, Float> result = MBTComputeStatistics.computeStatistics(bestChannel,sampRate,packetLength,packets);
        //assertTrue(result.isEmpty());
    } */

    @Test (expected = NullPointerException.class)
    public void computeStatisticsSNRNullSNRTest() {
        final float threshold = 0.7f;
        Float[] snrValues = null;
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.isEmpty());
    }
}