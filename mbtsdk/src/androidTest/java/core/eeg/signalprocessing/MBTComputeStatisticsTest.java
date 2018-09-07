package core.eeg.signalprocessing;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import core.eeg.storage.MbtEEGPacket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MBTComputeStatisticsTest {

    @Before
    public void setUp() {
        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    /**
     * Check that IndexOutOfBoundsException is raised is the number of the best channel input is above the maximum number of channels
     * This test works only for a Melomind headset, or any other headset that has 2 channels.
     */
    @Test (expected = IndexOutOfBoundsException.class)
    public void computeStatisticsBadBestChannelTest() {
        final int bestChannel = 2;
        final int sampRate = 1;
        final int packetLength = 1;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, null);
        packets.setQualities(qualities);
        MBTComputeStatistics.computeStatistics(bestChannel,sampRate,packetLength,packets);
    }

    /**
     * Check that computeStatistics method with a zero samprate input will generate an empty result
     */
    /*@Test
    public void computeStatisticsZeroSampRate() {
        final int sampRate = 0; //if sampRate = 0, mainMatrix stays empty => bestChannelData is empty
        final int packetLength = 1;
        HashMap<String, float[]> params = new HashMap<>();
        params.put("Example",new float[1]);
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<>();
        channelsData.add(new ArrayList<>());
        channelsData.add(new ArrayList<>());
        ArrayList<Float> qualities = new ArrayList<>();
        qualities.add(1F);
        qualities.add(1F);
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData,null);
        packets.setQualities(qualities);
        HashMap<String, Float> result = MBTComputeStatistics.computeStatistics(0,sampRate,packetLength,packets);
        assertTrue(result.isEmpty());
    }*/

    @Test (expected = NullPointerException.class)
    public void computeStatisticsSNRNullSNRTest() {
        final float threshold = 0.7f;
        Float[] snrValues = null;
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.isEmpty());
    }

    /**
     * Check that the computed SNR is equal to 0 if all the input values are under the threshold
     */
    @Test
    public void computeStatisticsSNRUUnderThresholdTest() {
        final float threshold = 0.5f;
        Float[] snrValues = new Float[]{0.2f,0.1f,0.3f,0.26f,0.11f};
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(" result journey : "+result.get("journey"),result.get("journey") == 0);
    }

    /**
     * Check that the computed SNR is close to 100 if all the input values are above the threshold
     */
    @Test
    public void computeStatisticsSNRUAboveThresholdTest() {
        final float threshold = 0.6f;
        Float[] snrValues = new Float[]{0.61f,0.61f,0.61f,0.61f,0.61f};
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(" result journey "+result.get("journey"),result.get("journey").equals(100f));
    }

    /**
     * Check that the computed result
     */
    @Test
    public void computeStatisticsSNRNaNOnlyTest() {
        final float threshold = 0.6f;
        Float[] snrValues = new Float[]{Float.NaN,Float.NaN,Float.NaN};
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(result.get("journey") == 0);
    }

    /**
     * Check that the computed result is included in a coherent/relevant range of value for a any input generated randomly
     */
    @Test
    public void computeStatisticsSNRRandomTest() {
        final float threshold = 0.7f;
        Float[] snrValues = new Float[10];
        for (int i=0 ; i< 10 ; i++){
            snrValues[i] = new Random().nextFloat();
        }
        HashMap<String, Float> result = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValues);
        assertTrue(result.containsKey("journey"));
        assertTrue(" snrValues "+Arrays.toString(snrValues)+" result journey "+result.get("journey"),result.get("journey") > 0);
        assertTrue(" result journey "+result.get("journey"), result.get("journey") <= 100);
    }

    /**
     * Computes two results for a given input with two different threshold and check that the lower threshold gives a higher result
     */
    @Test
    public void computeStatisticsSNRCompare2ThresholdsTest() {
        final float thresholdHigh = 0.7f;
        final float thresholdLow = 0.2f;
        Float[] snrValues = new Float[10];
        for (int i=0 ; i< 10 ; i++){
            snrValues[i] = new Random().nextFloat();
        }
        HashMap<String, Float> resultLow= MBTComputeStatistics.computeStatisticsSNR(thresholdLow,snrValues);
        HashMap<String, Float> resultHigh = MBTComputeStatistics.computeStatisticsSNR(thresholdHigh,snrValues);
        assertTrue(" result high journey "+resultHigh.get("journey")+" result low journey "+resultLow.get("journey"),resultLow.get("journey") > resultHigh.get("journey"));
    }
    /**
     * Computes two results for two different defined inputs with the same threshold and check that the input with higher snrValues gives a higher result
     */
    @Test
    public void computeStatisticsSNRCompare2snrValuesTest() {
        final float threshold= 0.7f;
        Float[] snrValuesLow = new Float[]{0.6828991f, 0.8369432f, 0.44153255f, 0.038255513f, 0.7764619f, 0.85872406f, 0.06901419f, 0.9526849f, 0.69523627f, 0.32952613f};
        Float[] snrValuesHigh = new Float[]{0.7828991f, 0.9369432f, 0.54153255f, 0.138255513f, 0.8764619f, 0.95872406f, 0.16901419f, 0.9926849f, 0.79523627f, 0.42952613f};

        HashMap<String, Float> resultLow= MBTComputeStatistics.computeStatisticsSNR(threshold,snrValuesLow);
        HashMap<String, Float> resultHigh = MBTComputeStatistics.computeStatisticsSNR(threshold,snrValuesHigh);
        assertTrue("array low "+Arrays.toString(snrValuesLow)+" result high journey "+resultHigh.get("journey")+" result low journey "+resultLow.get("journey"),resultLow.get("journey") < resultHigh.get("journey"));
    }

}