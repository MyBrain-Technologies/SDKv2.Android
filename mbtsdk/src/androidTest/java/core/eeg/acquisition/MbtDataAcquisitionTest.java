package core.eeg.acquisition;

import android.content.Context;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import features.MbtFeatures;
import features.ScannableDevices;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.*;

//@RunWith(RobolectricTestRunner.class)
public class MbtDataAcquisitionTest {

    private MbtDataAcquisition dataAcquisition;

    @Before
    public void setUp() throws Exception {
        try {
            System.loadLibrary("mbtalgo_2.3.0");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        Context context = getContext();
        MbtManager mbtManager = new MbtManager(context);
        MbtEEGManager mbtEEGManager = new MbtEEGManager(context, mbtManager,BtProtocol.BLUETOOTH_LE);
        this.dataAcquisition = new MbtDataAcquisition(mbtEEGManager, BtProtocol.BLUETOOTH_LE);
    }

     @Test
    public void isBitSetZerosTest() {
       int i = 0;
        byte tempStatus = (byte) 0;
        //Float result = dataAcquisition.isBitSet(tempStatus, i); // private method
        //assertTrue(result.equals(0f)); //check that the tested method return 0f as expected
    }

    @Test
    public void isBitSetWithOnesTest() {
        int i = 100000000;
        byte tempStatus = (byte) 1;
        //Float result = dataAcquisition.isBitSet(tempStatus, i); // private method
        //assertTrue(result.equals(1f)); //check that the tested method return 1f as expected
    }

    @Test
    public void resetIndexTest() {
        dataAcquisition.resetIndex();
        //check that the indexes has been well initialized
        assertTrue(dataAcquisition.getPreviousIndex() == -1);
        assertTrue(dataAcquisition.getStartingIndex() == -1);
    }


    @Test
    public void handleDataMatrixSizeBLETest() { //check that the output Float matrix size is correct, according to the input array size
        MbtConfig.setScannableDevices(ScannableDevices.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] data = new byte[250];
        for (int i=0; i<10; i++){// buffer size = 1000=16*62,5 => matrix size always = 1000/2 = 500
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            dataAcquisition.handleDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0); //reset
        }
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.testGetEegMatrix();
        assertNotNull(eegResult); //problem to solve : test return null eegResult
        assertTrue("size "+eegResult.size()*eegResult.get(0).size(), eegResult.size()*eegResult.get(0).size()==124);
    }

    @Test
    public void handleDataMatrixSizeSPPTest() { //check that the output Float matrix size is correct, according to the input array size
        MbtConfig.setScannableDevices(ScannableDevices.VPRO);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.VPRO_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_SPP));

        Context context = getContext();
        MbtManager mbtManager = new MbtManager(context);
        MbtEEGManager mbtEEGManager = new MbtEEGManager(context, mbtManager,BtProtocol.BLUETOOTH_SPP);
        this.dataAcquisition = new MbtDataAcquisition(mbtEEGManager, BtProtocol.BLUETOOTH_SPP);
        byte[] data = new byte[250];
        for (int i=0; i<10; i++){ //buffer size=6750=62,5*108(Packet size) => matrix size = 6750/3 - 250 (-250 because matrix.get(0) removed for SPP)
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            dataAcquisition.handleDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0); //reset
        }
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.testGetEegMatrix();
        assertNotNull(eegResult); //problem to solve : test return null eegResult
        assertTrue("size "+eegResult.size()*eegResult.get(0).size(), eegResult.size()*eegResult.get(0).size()==124);

    }

 @Test
    public void handleDataAbnormalAmountOfDataTest() { //check that we don't have any matrix if a small amount Of EEG Data is given in parameter

        byte[] data = new byte[250];
        Arrays.fill(data, (byte) 1);

        //dataAcquisition.handleDataAcquired(data);
        //assertNull(dataAcquisition.testGetEegMatrix());


    }

}