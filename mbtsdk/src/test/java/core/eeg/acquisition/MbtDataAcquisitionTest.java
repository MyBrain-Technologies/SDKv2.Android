package core.eeg.acquisition;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

import core.MbtManager;
import core.eeg.MbtEEGManager;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MbtDataAcquisitionTest {

    private MbtDataAcquisition dataAcquisition;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        MbtManager mbtManager = new MbtManager(context);
        MbtEEGManager mbtEEGManager = new MbtEEGManager(context, mbtManager);
        this.dataAcquisition = new MbtDataAcquisition(mbtEEGManager);
    }

    @Test
    public void isBitSetZerosTest() {
        int i = 0;
        byte tempStatus = (byte) 0;
        Float result = MbtDataAcquisition.isBitSet(tempStatus, i);
        assertTrue(result.equals(0f)); //check that the tested method return 0f as expected
    }

    @Test
    public void isBitSetWithOnesTest() {
        int i = 100000000;
        byte tempStatus = (byte) 1;
        Float result = MbtDataAcquisition.isBitSet(tempStatus, i);
        assertTrue(result.equals(1f)); //check that the tested method return 1f as expected
    }

    @Test
    public void reconfigureBuffersTest() {
        int samprate = 250;
        byte samplePerNotif = (byte) 1;
        int statusByteNb = 2;

        dataAcquisition.reconfigureBuffers(samprate,samplePerNotif,statusByteNb);

        //check that the buffer and indexes has been well initialized
        assertTrue(MbtDataAcquisition.getBufPos() == 0);
        assertTrue(MbtDataAcquisition.getPreviousIndex() == -1);
        assertTrue(MbtDataAcquisition.getStartingIndex() == -1);
    }

    @Test
    public void handleDataAbnormalAmountOfDataTest() { //check that we don't have an abnormal Amount Of EEG Data

        int abnormalMaxSize = 10000; //very big value randomly chosen
        byte[] data = new byte[20];
        Arrays.fill(data, (byte) 1);

        dataAcquisition.handleDataAcquired(data);

        assertTrue(data.length!=0); //check that the byte array is not empty
        assertTrue(data.length < abnormalMaxSize); //check that the byte array doesn't contain too much data (abnormal/unsual case)

        for (byte oneData:data) {
            assertTrue(oneData!= (byte) 0); //check that the byte array contains good/relevant values
        }
    }

}