package core.eeg.signalprocessing;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;

import core.eeg.storage.MbtEEGPacket;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MBTComputeStatisticsTest {

    @Before
    public void setUp() throws Exception {
        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void computeStatisticsBadBestChannelTest() {
        final int bestChannel = 2; // should be only 0 or 1 => should raise indexoutofbondsexception
        final int sampRate = 1;
        final int packetLength = 1;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        packets.setQualities(qualities);
        HashMap<String, Float> hashMap = MBTComputeStatistics.computeStatistics(bestChannel,sampRate,packetLength,packets);
    }

    @Test (expected = UnsatisfiedLinkError.class)
    public void computeStatisticsZeroSampRate() {


        final int sampRate = 0; //if sampRate = 0, mainMatrix stays empty => bestChannelData is empty
        final int packetLength = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        qualities.add(1F);
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData,null);
        packets.setQualities(qualities);
        HashMap<String, Float> result = MBTComputeStatistics.computeStatistics(0,sampRate,packetLength,packets);
        assertTrue(result.isEmpty());
    }

    @Test (expected = NullPointerException.class)
    public void computeStatisticsSNRNullSNRTest() {
        final float threshold = 0.7f;
        Float[] snrValues = null;
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.isEmpty());
    }

    @Test
    public void computeStatisticsSNRUUnderThresholdTest() {
        final float threshold = 2;
        Float[] snrValues = new Float[]{-1f,-2f,-3f,0f,1f};
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(result.get("journey") > 0);
        assertTrue(result.get("journey") <= 50);
    }

    @Test
    public void computeStatisticsSNRUAboveThresholdTest() {
        final float threshold = 0.6f;
        Float[] snrValues = new Float[]{0.6f,0.65f,0.7f,0.75f,0.85f};
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(result.get("journey") > 0);
        assertTrue(result.get("journey") < 100);
    }

    @Test
    public void computeStatisticsSNRNaNOnlyTest() {
        final float threshold = 1f;
        Float[] snrValues = new Float[]{Float.NaN,Float.NaN,Float.NaN};
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(result.get("journey") == 0);
    }

}