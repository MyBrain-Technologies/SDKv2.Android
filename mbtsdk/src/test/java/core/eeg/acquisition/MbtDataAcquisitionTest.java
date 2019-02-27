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
import core.eeg.storage.RawEEGSample;
import features.MbtFeatures;
import features.MbtDeviceType;
import utils.MatrixUtils;

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
    public void setUp() {
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        this.dataAcquisition = new MbtDataAcquisition(new MbtEEGManager(context, new MbtManager(context),protocol),protocol);
    }

    /**
     * check that we have no EEG matrix if the input amount of EEG data is less than the buffer size
     */
    @Test
    public void handleDataTestNotEnoughData() {
        byte[] data = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        data = new byte[]{14, 84, 29, -50, 37, 41, 34, -62, 43, -113,  -128, 0, -128, 0, 14, 66, 4, -102};
        dataAcquisition.handleDataAcquired(data);
        assertNull("Not enough data for conversion",dataAcquisition.getTestEegMatrix());
    }


    /**
     * check that we have no EEG matrix if the input amount of EEG data is less than the buffer size
     */
    @Test
    public void handleDataTestNullStatus() {
        byte[] data = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        assertNull(dataAcquisition.getTestStatusDataBytes());
    }

    /**
     * check that status have been well generated in case of SPP protocol (BLE has no status)
     */
    /*@Test
    public void handleDataTestNonNullStatusSPP() {
        BtProtocol protocol = BtProtocol.BLUETOOTH_SPP;
        MbtConfig.setScannableDevices(MbtDeviceType.VPRO);
        assertTrue(" vpro ? "+getScannableDevices(),getScannableDevices() == MbtDeviceType.VPRO);
        assertTrue(" nb status bytes"+MbtFeatures.getNbStatusBytes(),MbtFeatures.getNbStatusBytes() == DEFAULT_SPP_NB_STATUS_BYTES);

        this.dataAcquisition = new MbtDataAcquisition(new MbtEEGManager(context, new MbtManager(context),protocol),protocol);

        byte[] data = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        assertNotNull(" "+Arrays.toString(dataAcquisition.getTestStatusDataBytes()),dataAcquisition.getTestStatusDataBytes());
        assertTrue(dataAcquisition.getTestStatusDataBytes().length == 2);
    }*/


    /**
     * check that the indexes has been well reset
     */
    @Test
    public void resetIndexTest() {
        dataAcquisition.resetIndex();
        assertTrue(dataAcquisition.getPreviousIndex() == -1);
        assertTrue(dataAcquisition.getStartingIndex() == -1);
    }

    /**
     * Check that some values have been well modified / the output values corresponds to the expected result for a defined/known input
     * after the call of the handleDataAcquired method.
     */
    @Test
    public void handleDataAcquiredTestInput(){
        int nbChannels = MbtFeatures.getNbChannels();
        int nbBytes = MbtFeatures.getEEGByteSize();
        int nbIndexBytes = MbtFeatures.getRawDataIndexSize();
        dataAcquisition.setTestPreviousIndex(-1);
        dataAcquisition.setTestSingleRawEEGList(null);
        byte[] data = new byte[]{14, 83, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        int sizeDataArray = data.length;

        dataAcquisition.handleDataAcquired(data);
        assertTrue(" previous index= "+dataAcquisition.getPreviousIndex(),dataAcquisition.getPreviousIndex()==3667);
        assertTrue(dataAcquisition.getTestSingleRawEEGList()!=null);
        assertTrue("single raw eeg size "+dataAcquisition.getTestSingleRawEEGList().size(),dataAcquisition.getTestSingleRawEEGList().size() == (sizeDataArray-nbIndexBytes)/(nbBytes*nbChannels));
    }

    /**
     * Check that the list of raw EEG data contains null bytes array of EEG data in case of EEG packets loss
     */
    @Test
    public void handleDataAcquiredTestPacketLoss(){
        int indexDifference = 2; //the goal of the test is to have a indexDiferrence >= 2
        int nbBytes = 2;
        int nbChannels = 2;
        dataAcquisition.setTestSingleRawEEGList(new ArrayList<>()); //reset the list
        byte[] data = new byte[]{0, 0, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        byte[] data2 = new byte[]{0, (byte)indexDifference, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        dataAcquisition.handleDataAcquired(data);
        dataAcquisition.handleDataAcquired(data2);
        ArrayList<RawEEGSample> rawEEGSamples = dataAcquisition.getTestRawEEGSample();

        assertTrue(rawEEGSamples.contains(RawEEGSample.LOST_PACKET_INTERPOLATOR));
        int lostPacketCounter=0;
        for (RawEEGSample rawEEGSample : rawEEGSamples){
            assertTrue("status: "+rawEEGSample.getStatus(),rawEEGSample.getStatus().equals(Float.NaN));
            if(rawEEGSample.equals(RawEEGSample.LOST_PACKET_INTERPOLATOR))
                lostPacketCounter++;
        }

        assertTrue(" lost packet count: "+lostPacketCounter,lostPacketCounter == nbBytes*nbChannels*indexDifference);
    }

    /**
     * Check that the list of raw EEG data does not contains null bytes array of EEG data in case of NO EEG packets loss
     */
    @Test
    public void handleDataAcquiredTestNoPacketLoss(){
        int nbBytes = 2;
        int nbChannels = 2;
        int nbBytesIndex =2;
        byte[] data = new byte[]{0, 0, 34, -62, 43, -113, 29, -50, 37, 41, 14, 66, 4, -102, -128, 0, -128, 0};
        int sizeArraydata = data.length;
        dataAcquisition.handleDataAcquired(data);
        ArrayList<RawEEGSample> rawEEGSamples = dataAcquisition.getTestRawEEGSample();

        assertTrue(!rawEEGSamples.contains(RawEEGSample.LOST_PACKET_INTERPOLATOR));
        assertTrue(" raw eeg sample size: "+rawEEGSamples.size()+" rawEEGSamples "+rawEEGSamples.toString(),rawEEGSamples.size() == ((sizeArraydata-nbBytesIndex)/(nbBytes*nbChannels)));
        for (RawEEGSample rawEEGSample : rawEEGSamples){
            assertTrue(rawEEGSample.getBytesEEG()!=null);
            assertTrue(rawEEGSample.getBytesEEG().size()== nbChannels);
            for (byte[] bytes : rawEEGSample.getBytesEEG()){
                assertTrue(bytes.length == nbBytes);
            }
            assertTrue("status "+rawEEGSample.getStatus(),rawEEGSample.getStatus().equals(Float.NaN));
        }
    }

    /**
     * Check that the method does'nt change anything in case that the input data array size is lower than 2.
     */
    @Test
    public void handleDataAcquiredTestDataInputSize(){
        dataAcquisition.setTestPreviousIndex(-1);
        byte[] data = new byte[]{14};
        dataAcquisition.handleDataAcquired(data);
        assertTrue(" previous index= "+dataAcquisition.getPreviousIndex(),dataAcquisition.getPreviousIndex()==-1);
    }

    @Test
    public void handleDataAcquiredTestDefinedOutput() { // check that the result of EEG matrix computation is equal to the pre-generated matrix with a given input of eeg raw array
        int nbAcquisition = 10;
        int nbChannels = 2;
        int nbBytes = 2;
        int sizeArray = 18;
        int nbBytesIndex = 2;
        final ArrayList<ArrayList<Float>> eegConverted = new ArrayList<>();
        eegConverted.add(new ArrayList<>(Arrays.asList(0.005089656F,0.0043643597F,0.0020877998F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018539092F,-0.0150310155F,-0.011231219F,-0.0021661639F,0.00833976F,0.014444715F,0.017033014F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F)));
        eegConverted.add(new ArrayList<>(Arrays.asList(0.0063783717F,0.0054414356F,6.7381596E-4F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.018743295F,-0.015102515F,-0.010817092F,-7.16144E-4F,0.011275835F,0.018626608F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F,0.018742723F)));

        byte[][] data = new byte[nbAcquisition][sizeArray];
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


        final CompletableFuture<Void> future = CompletableFuture.runAsync(()->{ //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done

            for (int i=0; i<nbAcquisition; i++) {
                dataAcquisition.handleDataAcquired(data[i]);
                assertTrue(data[i].length == 18);
            }
        },Executors.newCachedThreadPool());

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ArrayList<ArrayList<Float>> eegResultMatrix = MatrixUtils.invertFloatMatrix(dataAcquisition.getTestEegMatrix());
        assertNotNull(eegResultMatrix);
        assertTrue(future.isDone());// if the async task has been completed
        assertNotNull(dataAcquisition.getTestEegManager());
        assertTrue(eegResultMatrix.size()== nbChannels);
        assertTrue(eegResultMatrix.get(0).size() == ((sizeArray-nbBytesIndex)/(nbBytesIndex*nbBytes))*nbAcquisition);
        assertTrue(" Result size "+eegResultMatrix.size()+" | Pre-generated size"+eegConverted.size(),eegResultMatrix.size() == eegConverted.size());
        assertTrue(eegResultMatrix.get(0).size() == eegConverted.get(0).size());
        assertTrue(" Result "+eegResultMatrix.size()+" | Pre-generated "+eegConverted.size(),eegResultMatrix.equals(eegConverted));
    }

    /**
     * check that the output Float matrix size is correct, according to the input array size
     */
    @Test
    public void handleDataMatrixSizeBLETest() {
        int nbAcquisition = 10;
        int nbChannels = 2;
        int nbBytes= 2;
        int nbBytesIndex = 2;
        MbtConfig.setScannableDevices(MbtDeviceType.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] rawData = new byte[]{14, 92, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1};
        int sizeArray = rawData.length;
        final CompletableFuture<Void> future= CompletableFuture.runAsync(()->{ //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done

            for (int i=0; i<nbAcquisition; i++) {
                dataAcquisition.handleDataAcquired(rawData);
                assertTrue(rawData.length == 18);
            }
        },Executors.newCachedThreadPool());

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult);
        assertTrue("channel size "+eegResult.size(),eegResult.size()>0);
        assertTrue("eeg.size()  "+eegResult.size(),eegResult.size()==((sizeArray-nbBytesIndex)/(nbChannels*nbBytes))*nbAcquisition);
        assertTrue("eeg.get(0).size "+eegResult.get(0).size(),eegResult.get(0).size()==nbChannels);
        assertTrue(eegResult.get(0).size()*eegResult.size()==nbChannels*((sizeArray-nbBytesIndex)/(nbChannels*nbBytes))*nbAcquisition);
    }

    /**
     * check that the output Float matrix size is correct, according to the input array size
     */
    @Test
    public void handleDataMatrixSizeBLETestRandom() {
        int nbAcquisition = 10;
        int nbChannels = 2;
        int nbBytesIndex = 2;
        int nbBytes = 2;
        int sizeArray = 18;
        MbtConfig.setScannableDevices(MbtDeviceType.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] data = new byte[sizeArray];
        new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array

        final CompletableFuture<Void> future= CompletableFuture.runAsync(()->{ //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done

            for (int i=0; i<nbAcquisition; i++) {
                dataAcquisition.handleDataAcquired(data);
                assertTrue(data.length == 18);
            }
        },Executors.newCachedThreadPool());

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult);
        assertTrue("channel size "+eegResult.size(),eegResult.size()>0);
        assertTrue("nb channel  "+eegResult.size(),eegResult.size()==((sizeArray-nbBytesIndex)/(nbChannels*nbBytes))*nbAcquisition);
        assertTrue(eegResult.get(0).size() >0);
        assertTrue(eegResult.get(0).size()==nbChannels);
        assertTrue(" matrix size "+eegResult.get(0).size()*eegResult.size(),eegResult.get(0).size()*eegResult.size()==nbChannels*((sizeArray-nbBytesIndex)/(nbChannels*nbBytes))*nbAcquisition);
    }

    @Test
    public void handleDataMatrixSizeBLETestRandom22bytes() { //check that the output Float matrix size is correct, according to the input array size with a 22 bytes array, the output size should be 80 (same size as the previous test)
        int nbAcquisition = 10;
        int nbChannels = 2;
        int nbBytesIndex = 2;
        int nbBytes = 2;
        int sizeArray = 250;
        MbtConfig.setScannableDevices(MbtDeviceType.MELOMIND);
        assertTrue(MbtFeatures.getNbChannels()==MbtFeatures.MELOMIND_NB_CHANNELS);
        assertTrue(MbtFeatures.getBluetoothProtocol().equals(BtProtocol.BLUETOOTH_LE));

        byte[] data = new byte[sizeArray];
        new Random().nextBytes(data); //Generates random bytes and places them into a user-supplied byte array

        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> { //as handledataacquired calls methods that contain an async task where the eeg result matrix is returned, we must wait that the eeg matrix computation is done
            for (int i = 0; i < nbAcquisition; i++) {// buffer size = 1000=16*62,5 => matrix size always = 1000/2 (in case nbChannel is 2) = 500
                dataAcquisition.handleDataAcquired(data);
            }
        }, Executors.newCachedThreadPool());

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<ArrayList<Float>> eegResult = dataAcquisition.getTestEegMatrix();
        assertNotNull(eegResult);
        assertTrue("channel size "+eegResult.size(),eegResult.size()>0);
        assertTrue("expected "+sizeArray/4+" nb channel  "+eegResult.size(),eegResult.size()==sizeArray/4);
        assertTrue(eegResult.get(0).size() >0);
        assertTrue(eegResult.get(0).size()==nbChannels);
        assertTrue("matrix size "+eegResult.get(0).size()*eegResult.size(),eegResult.get(0).size()*eegResult.size()==(sizeArray/4*nbChannels));

    }
}