package core.eeg.signalprocessing;

import android.content.Context;
import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import core.eeg.Log;

import static androidx.test.platform.app.InstrumentationRegistry.getContext;
import static org.junit.Assert.*;

public class MBTSignalQualityCheckerTest {

    private static int samprate ;
    private static int packetLength ;
    private static Context context ;

    private static final String MMFILE422_TEST = "inputData422MMHeadsetOnTable.txt";
    private static final String MMFILE630_TEST = "inputData630GuidedTestingQC.txt";
    private static final String MMFILE1200_TEST = "inputData_1200RSEC_MMTesting.txt";

    private static final String MMFILE422_RESULT = "qualityTesting422.txt";
    private static final String MMFILE630_RESULT = "qualityTesting630.txt";
    private static final String MMFILE1200_RESULT = "qualityTesting1200.txt";

    private static final int MMFILE422_ROWSIZE = 422;
    private static final int MMFILE630_ROWSIZE = 630;
    private static final int MMFILE1200_ROWSIZE = 1200;


    @Before
    public void setUp() {
        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        Log.d("quality checker", "setup");
        samprate = 250;
        packetLength = 250;
        context = getContext();
        testInitQualityChecker();
    }



    public void testInitQualityChecker(){
        assertEquals(MBTSignalQualityChecker.qcCurrentState, MBTSignalQualityChecker.QCStateMachine.NOT_READY);
        String version = MBTSignalQualityChecker.initQualityChecker();
        assertEquals(version,"2.3.1");
        assertEquals(MBTSignalQualityChecker.qcCurrentState, MBTSignalQualityChecker.QCStateMachine.IDLE);

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

    @Test (expected = IllegalArgumentException.class)
    public void computeQualitiesForPacketNullChannel() {
        final int samprate = 250;
        final int packetLength = 250;
        final ArrayList<ArrayList<Float>> channels =  null; //check that Exception is raised if channel is null

        MBTSignalQualityChecker.computeQualitiesForPacketNew(samprate,packetLength,channels);
    }


//    @Test
//    public void getModifiedInputData() {
//        //Float[][] result = MBTSignalQualityChecker.getModifiedInputData();
//        //assertTrue(result==null || result.length !=0); //this method return null if floatArrayClass (floatArrayClass = env->FindClass("[F")) is null or current (current = env->NewFloatArray((jsize) channelData.size())) is null
//    }


    @Test
    public void computeQualitiesGuidedTestingQC()  {
        assertTrue(MBTSignalQualityChecker.qcCurrentState == MBTSignalQualityChecker.QCStateMachine.IDLE);
        if(context!=null)
            computeQualities(MMFILE630_ROWSIZE,MMFILE630_TEST,MMFILE630_RESULT);

    }

    @Test
    public void computeQualitiesHeadsetOnTable()  {
        assertTrue(MBTSignalQualityChecker.qcCurrentState == MBTSignalQualityChecker.QCStateMachine.IDLE);
        if(context!=null)
            computeQualities(MMFILE422_ROWSIZE,MMFILE422_TEST,MMFILE422_RESULT);

    }

    @Test
    public void computeQualitiesTestingLong()  {
        assertTrue(MBTSignalQualityChecker.qcCurrentState == MBTSignalQualityChecker.QCStateMachine.IDLE);
        if(context!=null)
            computeQualities(MMFILE1200_ROWSIZE,MMFILE1200_TEST,MMFILE1200_RESULT);

    }

    public void deinitQualityChecker() {

        Log.d("deinit", "state is " + MBTSignalQualityChecker.qcCurrentState);

        assertTrue(MBTSignalQualityChecker.qcCurrentState == MBTSignalQualityChecker.QCStateMachine.IDLE || MBTSignalQualityChecker.qcCurrentState == MBTSignalQualityChecker.QCStateMachine.COMPUTING);
        MBTSignalQualityChecker.deinitQualityChecker();

    }


    private void computeQualities(int row, String inputFileName,String outputFileName){

        float[][] inputMatrix = createInputMatrix(inputFileName,row);
        float[] expectedResults = createOutputMatrix(outputFileName,row);

        float[] results = MBTSignalQualityChecker.nativeComputeQualityCheckerNew(inputMatrix, samprate, packetLength);
        String difference="\n expectedResults | results";

        for (int i=0;i<expectedResults.length;i++){
            if(expectedResults[i]!=results[i]){
                difference = difference.concat("\n").concat("Index ["+String.valueOf(i)+"] : ").concat(Float.toString(expectedResults[i])).concat(" | ").concat(Float.toString(results[i]));
            }
        }

        assertTrue((difference.equals("\n expectedResults | results"))?"No difference":difference, Arrays.equals(results, expectedResults));
    }

    private float[][] createInputMatrix(String inputFileName,int rowSize) {

        InputStream inputStream = null;

        inputStream = this.getClass().getClassLoader().getResourceAsStream(inputFileName); //TODO change here

        BufferedReader reader = null;
        int colSize = samprate;
        float[][] channels = new float[rowSize][colSize];
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            for (int row = 0; row < rowSize; row++) {
                String[] line = reader.readLine().trim().split("\t");
                for (int col = 0;col < colSize; col++) {
                    channels[row][col] = Float.parseFloat(line[col]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return channels;
    }

    private float[] createOutputMatrix(String outputFileName,int rowSize){

        InputStream outputStream = null;

        outputStream = this.getClass().getClassLoader().getResourceAsStream(outputFileName); //TODO change here

        BufferedReader reader = null;
        float[] results = new float[rowSize];
        try {
            reader = new BufferedReader(new InputStreamReader(outputStream, "UTF-8"));
            String[] line = reader.readLine().split(" ");
            for (int i=0;i<line.length;i++){
                results[i]=Float.parseFloat(line[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return results;
    }

    @After
    public void tearDown() {
        deinitQualityChecker();
    }

}