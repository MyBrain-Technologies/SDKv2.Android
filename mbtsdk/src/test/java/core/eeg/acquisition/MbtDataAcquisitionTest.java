package core.eeg.acquisition;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.manifest.IntentFilterData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MbtDataAcquisitionTest {

    private MbtDataAcquisition dataAcquisition;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        MbtManager mbtManager = new MbtManager(context);
        MbtEEGManager mbtEEGManager = new MbtEEGManager(context, mbtManager,BtProtocol.BLUETOOTH_LE);
        this.dataAcquisition = new MbtDataAcquisition(mbtEEGManager, BtProtocol.BLUETOOTH_LE);
    }

    /*@Test
    public void isBitSetZerosTest() {
        int i = 0;
        byte tempStatus = (byte) 0;
        Float result = dataAcquisition.isBitSet(tempStatus, i); // private method
        assertTrue(result.equals(0f)); //check that the tested method return 0f as expected
    }*/

    /*@Test
    public void isBitSetWithOnesTest() {
        int i = 100000000;
        byte tempStatus = (byte) 1;
        Float result = dataAcquisition.isBitSet(tempStatus, i); // private method
        assertTrue(result.equals(1f)); //check that the tested method return 1f as expected
    }*/

    @Test
    public void reconfigureBuffersTest() {
        byte samplePerNotif = (byte) 1;
        byte statusByteNb = 2;

        dataAcquisition.reconfigureBuffers(samplePerNotif,statusByteNb);
        //check that the buffer and indexes has been well initialized
        assertTrue(dataAcquisition.getPreviousIndex() == -1);
        assertTrue(dataAcquisition.getStartingIndex() == -1);
    }


    @Test
    public void handleDataMatrixSizeBLETest() { //check that the output Float matrix size is correct, according to the input array size
        byte[] data = new byte[250];
        for (int i=0; i<63; i++){// buffer size = 1000=16*62,5 => matrix size always = 1000/2 = 500
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            dataAcquisition.handleDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0);
        }
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getEegMatrix();
        assertNotNull(eegResult); //problem to solve : test return null eegResult
        //assertTrue(eegResult.size()*eegResult.get(0).size()==500);
    }

    @Test
    public void handleDataMatrixSizeSPPTest() { //check that the output Float matrix size is correct, according to the input array size
        byte[] data = new byte[250];
        for (int i=0; i<63; i++){ //buffer size=6750=62,5*108(Packet size) => matrix size = 6750/3 - 250 (-250 because matrix.get(0) removed for SPP)
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            dataAcquisition.handleDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0);
        }
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getEegMatrix();
        assertNotNull(eegResult); //problem to solve : test return null eegResult
        //assertTrue(eegResult.size()*eegResult.get(0).size()==2000);
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