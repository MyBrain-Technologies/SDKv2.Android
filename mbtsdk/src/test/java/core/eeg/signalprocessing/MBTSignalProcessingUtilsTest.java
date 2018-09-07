package core.eeg.signalprocessing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MBTSignalProcessingUtilsTest {


    /**
     * check that IllegalArgumentException is raised if input is null
     */
    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixDoubleNullChannelsTest() {
        MBTSignalProcessingUtils.channelsToMatrixDouble(null);
    }

    /**
     * check that IllegalArgumentException is raised if input is empty
     */
    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixDoubleEmptyChannelsTest() {
        MBTSignalProcessingUtils.channelsToMatrixDouble();
    }

    /**
     * Check that illegalArgumentException is raised because the size of the 2 channels are not equals.
     * (ERRROR : samprate not consistent in all provided channels)
     */
    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixDoubleSamprateTest() {
        Float[][] channels = new Float[][]{{1F,2F,3F},{5F,6F}};
        double[][] expectedOutput = null;
        double[][] computedOutput = MBTSignalProcessingUtils.channelsToMatrixDouble(channels);
        assertTrue(""+Arrays.deepToString(computedOutput), Arrays.deepEquals(computedOutput, expectedOutput));
    }

    /**
     * Check that the output double matrix size (number of line and number of column) is equal to the input matrix size
     * and that the output content is equal to the input content (every lines and every columns must contains the same values).
     */
    @Test
    public void channelsToMatrixDoubleOutputCompareExpectedTest() {
        Float[][] channels = new Float[][]{{1F,2F,3F},{4F,5F,6F}};
        double[][] expectedOutput = new double[][]{{1,2,3},{4,5,6}};
        double[][] doubleMatrixComputed = MBTSignalProcessingUtils.channelsToMatrixDouble(channels);
        assertTrue("matrix.length (lines output = colums input) "+doubleMatrixComputed.length,doubleMatrixComputed.length == channels.length);
        assertTrue("matrix[0].length (lines output = colums input) "+doubleMatrixComputed[0].length,doubleMatrixComputed[0].length == channels[0].length);
        assertTrue("Computed : "+ Arrays.deepToString(doubleMatrixComputed)+" \n Expected : "+ Arrays.deepToString(expectedOutput),Arrays.deepToString(doubleMatrixComputed).equals(Arrays.deepToString(expectedOutput)));
    }


    /**
     * check that IllegalArgumentException is raised if input is null
     */
    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixFloatNullChannelsTest() {
        float[][] computedOutput = MBTSignalProcessingUtils.channelsToMatrixFloat((ArrayList<ArrayList<Float>>) null);
        assertNull(computedOutput);
    }

    /**
     * check that IllegalArgumentException is raised if input is empty
     */
    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixFloatEmptyChannelsTest() {
        float[][] computedOutput = MBTSignalProcessingUtils.channelsToMatrixFloat();
        assertNull(computedOutput);
    }

    /**
     * Check that IllegalArgumentException is raised if the size of the 2 channels are different
     */
    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixFloatTest() {
        Float[] channels1 = new Float[0];
        Float[] channels2 = new Float[1];
        float[][] computedOutput = MBTSignalProcessingUtils.channelsToMatrixFloat(channels1,channels2);
        assertNull(computedOutput);
    }

    /**
     * Check that the output float matrix size (number of line and number of column) is equal to the input matrix size
     * and that the output content is equal to the input content (every lines and every columns must contains the same values).
     */
    @Test
    public void channelsToMatrixFloatOutputCompareExpectedTest() {
        Float[][] channels = new Float[][]{{1F,2F,3F},{4F,5F,6F}};
        float[][] expectedOutput = new float[][]{{1f,2f,3f},{4f,5f,6f}};
        float[][] floatMatrixComputed = MBTSignalProcessingUtils.channelsToMatrixFloat(channels);
        assertTrue("matrix.length (lines output = colums input) "+floatMatrixComputed.length,floatMatrixComputed.length == channels.length);
        assertTrue("matrix[0].length (lines output = colums input) "+floatMatrixComputed[0].length,floatMatrixComputed[0].length == channels[0].length);
        assertTrue("Computed : "+ Arrays.deepToString(floatMatrixComputed)+" \n Expected : "+ Arrays.deepToString(expectedOutput),Arrays.deepToString(floatMatrixComputed).equals(Arrays.deepToString(expectedOutput)));
    }

}