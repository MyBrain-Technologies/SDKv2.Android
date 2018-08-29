package core.eeg.signalprocessing;

import org.junit.Test;
import static org.junit.Assert.*;

public class MBTSignalProcessingUtilsTest {

    @Test
    public void channelsToMatrixDouble() {

    }

    @Test (expected = NullPointerException.class)
    public void channelsToMatrixFloatNullChannelsTest() {
        Float[] channels = null;  //check that IllegalArgumentException is raised if channels is null
        MBTSignalProcessingUtils.channelsToMatrixFloat(channels);
    }

    @Test /*(expected = IllegalArgumentException.class)*/
    public void channelsToMatrixFloatEmptyChannelsTest() {
        Float[] channels = new Float[0];  //check that IllegalArgumentException is raised if channels is empty
        assertTrue(channels.length == 0);
        MBTSignalProcessingUtils.channelsToMatrixFloat(channels);
    }

    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixFloatTest() {
        Float[] channels1 = new Float[0];
        Float[] channels2 = new Float[1]; // size = 1 => current.length != samprate => check that illegalArgumentException is raised as expected (ERRROR : samprate not consistent in all provided channels)
        int height = 2;
        int samprate = 0;
        MBTSignalProcessingUtils.channelsToMatrixFloat(channels1,channels2);
    }

    @Test (expected = NullPointerException.class)
    public void channelsToMatrixDoubleNullChannelsTest() {
        Float[] channels = null;  //check that IllegalArgumentException is raised if channels is null
        MBTSignalProcessingUtils.channelsToMatrixDouble(channels);
    }

    @Test /*(expected = IllegalArgumentException.class)*/
    public void channelsToMatrixDoubleEmptyChannelsTest() {
        Float[] channels = new Float[0];  //check that IllegalArgumentException is raised if channels is empty
        assertTrue(channels.length == 0);
        MBTSignalProcessingUtils.channelsToMatrixDouble(channels);
    }

    @Test (expected = IllegalArgumentException.class)
    public void channelsToMatrixDoubleTest() {
        Float[] channels1 = new Float[0];
        Float[] channels2 = new Float[1]; // size = 1 => current.length != samprate => check that illegalArgumentException is raised as expected (ERRROR : samprate not consistent in all provided channels)
        int height = 2;
        int samprate = 0;
        MBTSignalProcessingUtils.channelsToMatrixDouble(channels1,channels2);
    }
}