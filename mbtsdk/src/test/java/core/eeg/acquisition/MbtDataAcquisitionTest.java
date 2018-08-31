package core.eeg.acquisition;

import android.content.Context;
import android.support.annotation.NonNull;
import android.test.UiThreadTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.signalprocessing.ContextSP;
import features.MbtFeatures;
import features.ScannableDevices;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
import utils.AsyncUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for MbtDataAcquisition that included mock for C++ algorithms
 */
public class MbtDataAcquisitionTest {

    private MbtDataAcquisition dataAcquisition;

    private Context context = Mockito.mock(Context.class);

    @Before
    public void setUp() throws Exception {
        try {
            System.loadLibrary(ContextSP.LIBRARY_NAME + BuildConfig.USE_ALGO_VERSION);
        } catch (final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        this.dataAcquisition = new MbtDataAcquisition(new MbtEEGManager(context, new MbtManager(context),protocol),protocol);
        assertNotNull(this.dataAcquisition.getTestEegManager());

    }

    @Test
    public void handleDataTestNotEnoughData() { //check that we have no EEG matrix if the input amount of EEG data is less than the buffer size
        byte[] data = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        assertNull("Not enough data for conversion",dataAcquisition.getTestEegMatrix());
    }

    @Test
    //@UiThreadTest
    public void handleDataAcquiredTestOutput() {
        final ArrayList<ArrayList<Float>> eegConverted = new ArrayList<>();
        ArrayList<Float> channel1 = new ArrayList<>(Arrays.asList(0.005089656F,0.0043643597F,0.0020877998F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018539092F,-0.0150310155F,-0.011231219F,-0.0021661639F,0.00833976F,0.014444715F,0.017033014F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F));
        eegConverted.add(channel1);
        ArrayList<Float> channel2 = new ArrayList<>(Arrays.asList(0.0063783717F,0.0054414356F,6.7381596E-4F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.015102515F,-0.010817092F,-7.16144E-4F,0.011275835F,0.018626608F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F));
        eegConverted.add(channel2);
        byte[] data = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 84, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 85, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 86, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 87, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 88, -128, 0, -128, 0, -128, 0, -128, 0, -127, 101, -128, 0, -103, 90, -104, -35};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 89, -77, 77, -74, 33, -15, 53, -5, 28, 56, -12, 77, 1, 98, -91, 127, 52};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 90, 116, 82, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 91, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 92, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        dataAcquisition.handleDataAcquired(data);
        assertNotNull(dataAcquisition.getTestEegManager());
        assertNotNull(dataAcquisition.getTestEegMatrix());
        //assertTrue(" Result "+dataAcquisition.getTestEegMatrix().size()+" | Pre-generated "+eegConverted.size(),dataAcquisition.getTestEegMatrix().equals(eegConverted));
        //assertFalse(" Result "+dataAcquisition.getTestEegMatrix().size()+" | Pre-generated "+eegConverted.size(),dataAcquisition.getTestEegMatrix().equals(eegConverted));

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
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult); //problem to solve : test return null eegResult
        assertTrue("size "+eegResult.size()*eegResult.get(0).size(), eegResult.size()*eegResult.get(0).size()==124);
    }

    @Test
    public void handleDataMatrixSizeSPPTest() { //check that the output Float matrix size is correct, according to the input array size
        MbtConfig.setScannableDevices(ScannableDevices.VPRO);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.VPRO_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_SPP));

        MbtManager mbtManager = new MbtManager(context);
        MbtEEGManager mbtEEGManager = new MbtEEGManager(context, mbtManager,BtProtocol.BLUETOOTH_SPP);
        this.dataAcquisition = new MbtDataAcquisition(mbtEEGManager, BtProtocol.BLUETOOTH_SPP);
        byte[] data = new byte[250];
        for (int i=0; i<10; i++){ //buffer size=6750=62,5*108(Packet size) => matrix size = 6750/3 - 250 (-250 because matrix.get(0) removed for SPP)
            new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
            dataAcquisition.handleDataAcquired(data);
            Arrays.fill(data,0,0,(byte)0); //reset
        }
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult); //problem to solve : test return null eegResult
        assertTrue("size "+eegResult.size()*eegResult.get(0).size(), eegResult.size()*eegResult.get(0).size()==124);

    }
    /*

    @Test
    public void handleDataAbnormalAmountOfDataTest() { //check that we don't have any matrix if a small amount Of EEG Data is given in parameter

        //byte[] data = new byte[250];
        //Arrays.fill(data, (byte) 1);

        //dataAcquisition.handleDataAcquired(data);
        //assertNull(dataAcquisition.testGetEegMatrix());
    }*/


    /*@Test
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
    }*/

    @Test
    public void resetIndexTest() {
        dataAcquisition.resetIndex();
        //check that the indexes has been well initialized
        assertTrue(dataAcquisition.getPreviousIndex() == -1);
        assertTrue(dataAcquisition.getStartingIndex() == -1);
    }

}