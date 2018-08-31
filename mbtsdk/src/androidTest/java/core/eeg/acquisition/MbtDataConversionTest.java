package core.eeg.acquisition;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.storage.RawEEGSample;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MbtDataConversionTest {

    private Context context = Mockito.mock(Context.class);

    @Before
    public void setUp(){
        try {
            System.loadLibrary(ContextSP.LIBRARY_NAME + BuildConfig.USE_ALGO_VERSION);
        } catch (final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
    @Test
    public void convertRawDataToEEGTest() { //Check that matrix returned has 2 channels as expected
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        MbtDataAcquisition dataAcquisition = new MbtDataAcquisition(new MbtEEGManager(context, new MbtManager(context),protocol),protocol);
        int nbData = 18;
        int nbChannels = 2;
        int nbBytesData = 2;
        int nbIndexBytes = 2;
        byte[] rawData = new byte[nbData];
        new Random().nextBytes(rawData);

        final CompletableFuture<Void> future= CompletableFuture.runAsync(()->{
            for (int i=0;i<10;i++){
                dataAcquisition.handleDataAcquired(rawData);
            }

        }, Executors.newCachedThreadPool());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(future.isDone());// if the async task has been completed
        ArrayList<RawEEGSample> rawEEGSample = dataAcquisition.getTestRawEEGSample();
        rawEEGSample.set(1,new RawEEGSample(null, Float.NaN));
        ArrayList<ArrayList<Float>> eegMatrix = MbtDataConversion.convertRawDataToEEG(rawEEGSample,protocol);
        assertNotNull(eegMatrix);
        assertTrue("EEG matrix size "+eegMatrix.size() ,eegMatrix.size()==(nbData-nbIndexBytes)/(nbBytesData*nbChannels));
        assertTrue(eegMatrix.get(0).size()==nbChannels);
        for (ArrayList<Float> eegData: eegMatrix) {
            assertTrue("  Raw data "+ Arrays.toString(rawData)+"  Converted data "+ eegData.toString(),eegData.size()==2);
        }

    }
    /*@Test (expected = NullPointerException.class)
    public void convertRawDataToEEGNullDataTest() {
        byte[] data = null;  //check that IllegalArgumentException is raised if data is null
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 2;
        MbtDataConversion.convertRawDataToEEG(data, protocol);
    }

    @Test (expected = ArithmeticException.class)
    public void convertRawDataToEEGZeroChannelTest() {
        byte[] data = new byte[250];
        for(int i=0;i<250;i++){
            data[i] = (byte) 1;
        }
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 0; //if nbChannels =0, then divider =0 then we expect that length/divider will raise Arithmetic exception
        MbtDataConversion.convertRawDataToEEG(data, protocol);
    }

    @Test (expected = IllegalArgumentException.class)
    public void convertRawDataToEEG250RawDataTest() {
        byte[] data = new byte[251]; //check that if data.length % 250 is not equals 0, exception is raised
        Arrays.fill(data, (byte)1);
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        MbtDataConversion.convertRawDataToEEG(data, protocol);
    }

    @Test
    public void convertRawDataToEEGEmptyDataTest() {
        byte[] data = new byte[0]; // check that the method will return empty result if input data is empty
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 2;
        ArrayList<ArrayList<Float>> eegData = MbtDataConversion.convertRawDataToEEG(data, protocol);
        assertTrue(eegData.size()==nbChannels);
        assertTrue(eegData.get(0).isEmpty());
        assertTrue(eegData.get(1).isEmpty());
    }*/

    @Test
    public void convertRawDCOffsetBLEResultConversionTest() { //check that the computed result of raw data conversion is equal to a pre-generated expected result

        float expectedOffset = 0.01857856f;
        byte[] offset = new byte[]{23, -18};
        float computedDcOffset = MbtDataConversion.convertRawDCOffsetBLE(offset);

        assertTrue(computedDcOffset == expectedOffset);
    }

    @Test
    public void convertRawDCOffsetBLENegativeTest(){ //check that the computed result of raw data conversion is equal to a pre-generated expected result
        byte[] offset = new byte[]{-1, -18};
        float dcoffset = MbtDataConversion.convertRawDCOffsetBLE(offset);
        assertTrue("result "+dcoffset,dcoffset==-1.6473599E-4f);
    }
    @Test
    public void convertRawDCOffsetBLEPositiveTest(){ //check that the computed result of raw data conversion is equal to a pre-generated expected result
        byte[] offset = new byte[]{1, 18};
        float dcoffset = MbtDataConversion.convertRawDCOffsetBLE(offset);
        assertTrue("result "+dcoffset,dcoffset==0.002507648f);
    }

    @Test
    public void convertRawDCOffsetBLETestRandom() { // check that the computed result of conversion of a randomly generated array of raw data is included between -0,03 and +0,3.
        float dcOffset;
        byte[] offset = new byte[2];
        new Random().nextBytes(offset);
        dcOffset = MbtDataConversion.convertRawDCOffsetBLE(offset);

        assertTrue("input "+Arrays.toString(offset)+" | dc offset "+dcOffset,dcOffset>-0.03);
        assertTrue("input "+Arrays.toString(offset)+" | dc offset "+dcOffset,dcOffset<0.03);
    }


}