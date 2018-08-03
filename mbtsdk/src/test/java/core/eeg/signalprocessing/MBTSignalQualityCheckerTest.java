package core.eeg.signalprocessing;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class MBTSignalQualityCheckerTest {

    // /!\ Problem : UnsatisfiedLinkError is raised /!\
    @Test
    public void testInitQualityChecker() throws Exception {
        //String version = MBTSignalQualityChecker.initQualityChecker();
        //assertTrue(MBTSignalQualityChecker.isInit); //impossible to test : isInit has a private access
        //assertEquals(version,"1.1.3");

    }
    // /!\ Problem : UnsatisfiedLinkError is raised /!\
    @Test
    public void computeQualitiesForPacketNew() throws Exception {
        int samprate = 250;
        int packetLength = 250;
        float[][] channels = new float[2][250];

        //ContextSP.SP_VERSION = MBTSignalQualityChecker.initQualityChecker();
        //assertTrue(ContextSP.SP_VERSION != null);

        assertTrue(samprate>0);
        assertTrue(packetLength>0);

        final int matrixHeight = channels.length;
        assertTrue(matrixHeight == 2);

        // Creating 2d Matrix-like array
        final float[][] matrix = new float[matrixHeight][samprate];
        for (int it = 0 ; it < matrixHeight ; it++){
            final float[] current = channels[it];
            for (int it2 = 0; it2 < samprate; it2++)
                matrix[it][it2] = current[it2];
        }

        //float[] test = MBTSignalQualityChecker.nativeComputeQualityCheckerNew(matrix, samprate, packetLength);
        //assertTrue(test.length == 2);
        //assertTrue(test[0] != Float.NaN && test[1] != Float.NaN); //check that nativeComputeQualityCheckerNew doesn't return Not a Number
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeQualitiesForPacketNegativeSampRate() {
        final int samprate = -1; //check that IllegalArgumentException is raised if sampRate is negative
        final int packetLength = 10;
        final ArrayList<ArrayList<Float>> channel = new ArrayList<>();
        MBTSignalQualityChecker.computeQualitiesForPacketNew(samprate,packetLength,channel);
    }

    @Test (expected = IllegalArgumentException.class)
    public void computeQualitiesForPacketNegativePacketLength() {
        final int samprate = 250;
        final int packetLength = -1; //check that IllegalArgumentException is raised if packetsLength is negative
        final ArrayList<ArrayList<Float>> channel = new ArrayList<>();
        MBTSignalQualityChecker.computeQualitiesForPacketNew(samprate,packetLength,channel);
    }

    @Test (expected = NullPointerException.class)
    public void computeQualitiesForPacketNullChannel() {
        final int samprate = 250;
        final int packetLength = 250;
        final ArrayList<ArrayList<Float>> channels =  null; //check that Exception is raised if channel is null
        MBTSignalQualityChecker.computeQualitiesForPacketNew(samprate,packetLength,channels);
    }

    // /!\ Problem : UnsatisfiedLinkError is raised /!\
    @Test
    public void getModifiedInputData() {
        //Float[][] result = MBTSignalQualityChecker.getModifiedInputData();
        //assertTrue(result==null || result.length !=0); //this method return null if floatArrayClass (floatArrayClass = env->FindClass("[F")) is null or current (current = env->NewFloatArray((jsize) channelData.size())) is null
    }
}