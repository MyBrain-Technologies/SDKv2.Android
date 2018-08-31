package core.eeg.acquisition;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import config.MbtConfig;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.signalprocessing.ContextSP;
import features.MbtFeatures;
import features.ScannableDevices;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
import utils.MatrixUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for MbtDataAcquisition where C++ algorithms are called
 */
public class MbtDataAcquisitionTest {

    private MbtDataAcquisition dataAcquisition;

    private Context context = Mockito.mock(Context.class);

    @Before
    public void setUp(){
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
    public void handleDataAcquiredTestDefinedOutput() { // check that the result of EEG matrix computation is equal to the pre-generated matrix with a given input of eeg raw array
        int nbData = 10;
        int nbChannels = 2;
        int sizeArray = 18;
        final ArrayList<ArrayList<Float>> eegConverted = new ArrayList<>();
        eegConverted.add(new ArrayList<>(Arrays.asList(0.005089656F,0.0043643597F,0.0020877998F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018539092F,-0.0150310155F,-0.011231219F,-0.0021661639F,0.00833976F,0.014444715F,0.017033014F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F)));
        eegConverted.add(new ArrayList<>(Arrays.asList(0.0063783717F,0.0054414356F,6.7381596E-4F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.015102515F,-0.010817092F,-7.16144E-4F,0.011275835F,0.018626608F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F)));

        byte[][] data = new byte[10][sizeArray];
        data [0] = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        data [1] = new byte[]{14, 84, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        data [2] = new byte[]{14, 85, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        data [3] = new byte[]{14, 86, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        data [4] = new byte[]{14, 87, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0, -128, 0};
        data [5] = new byte[]{14, 88, -128, 0, -128, 0, -128, 0, -128, 0, -127, 101, -128, 0, -103, 90, -104, -35};
        data [6] = new byte[]{14, 89, -77, 77, -74, 33, -15, 53, -5, 28, 56, -12, 77, 1, 98, -91, 127, 52};
        data [7] = new byte[]{14, 90, 116, 82, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        data [8] = new byte[]{14, 91, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        data [9] = new byte[]{14, 92, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};

        final CompletableFuture<Void> future= CompletableFuture.runAsync(()->{
            for (int i=0; i< nbData; i++){
                dataAcquisition.handleDataAcquired(data[i]);
            }

        },Executors.newCachedThreadPool());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArrayList<ArrayList<Float>> eegResultMatrix = MatrixUtils.invertFloatMatrix(dataAcquisition.getTestEegMatrix());
        assertTrue(future.isDone());// if the async task has been completed
        assertNotNull(dataAcquisition.getTestEegManager());
        assertNotNull(eegResultMatrix);
        assertTrue(eegResultMatrix.size()==nbChannels);
        assertTrue(eegResultMatrix.get(0).size()==4*nbData);
        assertTrue(" Result size "+eegResultMatrix.size()+" | Pre-generated size"+eegConverted.size(),eegResultMatrix.size()==eegConverted.size());
        assertTrue(eegResultMatrix.get(0).size()==eegConverted.get(0).size());
        assertTrue(" Result "+eegResultMatrix.size()+" | Pre-generated "+eegConverted.size(),eegResultMatrix.equals(eegConverted));
    }

    @Test
    public void handleDataMatrixSizeBLETest() { //check that the output Float matrix size is correct, according to the input array size
        int nbData = 10;
        int nbChannels = 2;
        int sizeArray = 18;
        MbtConfig.setScannableDevices(ScannableDevices.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] rawData = new byte[]{14, 92, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        final CompletableFuture<Void> future= CompletableFuture.runAsync(()->{ //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done

            for (int i=0; i<nbData; i++) {
                dataAcquisition.handleDataAcquired(rawData);
                assertTrue(rawData.length == 18);
            }
        },Executors.newCachedThreadPool());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult);
        assertTrue("channel size "+eegResult.size(),eegResult.size()>0);
        assertTrue("nb channel  "+eegResult.size(),eegResult.size()==4*nbData);
        assertTrue("eeg.get(0).size "+eegResult.get(0).size(),eegResult.get(0).size()==nbChannels);
        assertTrue(eegResult.get(0).size()*eegResult.size()==nbChannels*4*nbData);
    }

    @Test
    public void handleDataMatrixSizeBLETestRandom() { //check that the output Float matrix size is correct, according to the input array size
        int nbData = 10;
        int nbChannels = 2;
        MbtConfig.setScannableDevices(ScannableDevices.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] data = new byte[18];
        new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> { //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done

            for (int i = 0; i < nbData; i++) {// buffer size = 1000=16*62,5 => matrix size always = 1000/2 = 500
                dataAcquisition.handleDataAcquired(data);
            }
        }, Executors.newCachedThreadPool());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult);
        assertTrue("channel size "+eegResult.size(),eegResult.size()>0);
        assertTrue("nb channel  "+eegResult.size(),eegResult.size()==4*nbData);
        assertTrue(eegResult.get(0).size() >0);
        assertTrue(eegResult.get(0).size()==nbChannels);
        assertTrue(" matrix size "+eegResult.get(0).size()*eegResult.size(),eegResult.get(0).size()*eegResult.size()==nbChannels*4*nbData);

    }

    @Test
    public void handleDataMatrixSizeBLETestRandom22bytes() { //check that the output Float matrix size is correct, according to the input array size with a 22 bytes array, the output size should be 80 (same size as the previous test)
        int nbData = 10;
        int nbChannels = 2;
        int sizeArray = 250;
        MbtConfig.setScannableDevices(ScannableDevices.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] data = new byte[sizeArray];
        new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array
        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> { //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done

            for (int i = 0; i < nbData; i++) {// buffer size = 1000=16*62,5 => matrix size always = 1000/2 (in case nbChannel is 2) = 500
                dataAcquisition.handleDataAcquired(data);
            }
        }, Executors.newCachedThreadPool());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult);
        assertTrue("channel size "+eegResult.size(),eegResult.size()>0);
        assertTrue("nb channel  "+eegResult.size(),eegResult.size()==sizeArray/4);
        assertTrue(eegResult.get(0).size() >0);
        assertTrue(eegResult.get(0).size()==nbChannels);
        assertTrue("matrix size "+eegResult.get(0).size()*eegResult.size(),eegResult.get(0).size()*eegResult.size()==(sizeArray/4)*nbChannels);

    }

    @Test
    public void resetIndexTest() {
        dataAcquisition.resetIndex();
        //check that the indexes has been well initialized
        assertTrue(dataAcquisition.getPreviousIndex() == -1);
        assertTrue(dataAcquisition.getStartingIndex() == -1);
    }


}