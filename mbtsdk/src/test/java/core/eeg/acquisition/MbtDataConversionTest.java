package core.eeg.acquisition;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import core.bluetooth.BtProtocol;
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

    /**
     * Check that matrix returned has 2 channels as expected even if a EEG packet loss occured
     */
    @Test
    public void convertRawDataToEEGTestSizeReturnedMatrix() {
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbData = 18;
        int nbChannels = 2;
        int nbBytesData = 2;
        int nbIndexBytes = 2;

        ArrayList<RawEEGSample> rawEEGSampleList = new ArrayList<>();
        ArrayList<byte[]> bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{-13, -29});
        bytesArrayList.add(new byte[]{68, 59});
        Float status = Float.NaN;
        rawEEGSampleList.add(new RawEEGSample(bytesArrayList,status));
        rawEEGSampleList.add(new RawEEGSample(null,status));
        bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{88, -4});
        bytesArrayList.add(new byte[]{-1, 28});
        rawEEGSampleList.add(new RawEEGSample(bytesArrayList,status));
        bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{-30, -64});
        bytesArrayList.add(new byte[]{-23, -60});
        rawEEGSampleList.add(new RawEEGSample(bytesArrayList,status));

        ArrayList<ArrayList<Float>> eegMatrix = MbtDataConversion.convertRawDataToEEG(rawEEGSampleList,protocol);
        assertNotNull(eegMatrix);
        assertTrue("EEG matrix size "+eegMatrix.size()+" == "+(nbData-nbIndexBytes)/(nbBytesData*nbChannels) ,eegMatrix.size()==(nbData-nbIndexBytes)/(nbBytesData*nbChannels));
        assertTrue(eegMatrix.get(0).size()==nbChannels);
        for (ArrayList<Float> eegData: eegMatrix) {
            assertTrue("  Raw data "+ rawEEGSampleList.toString() +"  Converted data "+ eegData.toString(),eegData.size()==2);
        }
    }

    /**
     * Check that matrix returned the good values according to a known input and a defined expected output
     */
    @Test
    public void convertRawDataToEEGTestContentReturnedMatrix() {
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbData = 18;
        int nbChannels = 2;
        int nbBytesData = 2;
        int nbIndexBytes = 2;
        ArrayList<ArrayList<Float>> expectedMatrix = new ArrayList<>();
        expectedMatrix.add(new ArrayList<Float>(Arrays.asList(-0.001773772f, 0.009991123f)));
        expectedMatrix.add(new ArrayList<>(Arrays.asList(Float.NaN, Float.NaN)));
        expectedMatrix.add(new ArrayList<>(Arrays.asList(0.013030159f, -1.30416E-4f)));
        expectedMatrix.add(new ArrayList<>(Arrays.asList(-0.0042831358f, -0.0032558239f)));
        ArrayList<RawEEGSample> rawEEGSampleList = new ArrayList<>();
        ArrayList<byte[]> bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{-13, -29});
        bytesArrayList.add(new byte[]{68, 59});
        Float status = Float.NaN;
        rawEEGSampleList.add(new RawEEGSample(bytesArrayList,status));
        rawEEGSampleList.add(new RawEEGSample(null,status));
        bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{88, -4});
        bytesArrayList.add(new byte[]{-1, 28});
        
        rawEEGSampleList.add(new RawEEGSample(bytesArrayList,status));
        bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{-30, -64});
        bytesArrayList.add(new byte[]{-23, -60});
        rawEEGSampleList.add(new RawEEGSample(bytesArrayList,status));

        ArrayList<ArrayList<Float>> eegMatrix = MbtDataConversion.convertRawDataToEEG(rawEEGSampleList,protocol);
        assertNotNull(eegMatrix);
        assertTrue("EEG matrix size "+eegMatrix.size()+" == "+(nbData-nbIndexBytes)/(nbBytesData*nbChannels) ,eegMatrix.size()==(nbData-nbIndexBytes)/(nbBytesData*nbChannels));
        assertTrue(eegMatrix.get(0).size()==nbChannels);
        for (ArrayList<Float> eegData: eegMatrix) {
            assertTrue("  Raw data "+ rawEEGSampleList.toString() +"  Converted data "+ eegData.toString(),eegData.size()==2);
        }
        assertTrue(" expected "+expectedMatrix+" but was "+eegMatrix, expectedMatrix.equals(eegMatrix));

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

    /**
     * check that the computed result of raw data conversion is equal to a pre-generated expected result
     */
    @Test
    public void convertRawDataToDcOffsetResultConversionTest() {

        float expectedOffset = 0.01857856f;
        byte[] offset = new byte[]{23, -18};
        float computedDcOffset = MbtDataConversion.convertRawDataToDcOffset(offset);

        assertTrue(computedDcOffset == expectedOffset);
    }

    /**
     * check that the computed result of raw data conversion is equal to a pre-generated expected result
     */
    @Test
    public void convertRawDataToDcOffsetNegativeTest(){
        byte[] offset = new byte[]{-1, -18};
        float dcoffset = MbtDataConversion.convertRawDataToDcOffset(offset);
        assertTrue("result "+dcoffset,dcoffset==-1.6473599E-4f);
    }

    /**
     * check that the computed result of raw data conversion is equal to a pre-generated expected result
     */
    @Test
    public void convertRawDataToDcOffsetPositiveTest(){
        byte[] offset = new byte[]{1, 18};
        float dcoffset = MbtDataConversion.convertRawDataToDcOffset(offset);
        assertTrue("result "+dcoffset,dcoffset==0.002507648f);
    }

    /**
     * check that the computed result of conversion of a randomly generated array of raw data is included in a coherent/consistent range of values
     */
    @Test
    public void convertRawDataToDcOffsetTestRandom() {
        float dcOffset;
        byte[] offset = new byte[2];
        new Random().nextBytes(offset);
        dcOffset = MbtDataConversion.convertRawDataToDcOffset(offset);

        assertTrue("input "+Arrays.toString(offset)+" | dc offset "+dcOffset,dcOffset>-0.03);
        assertTrue("input "+Arrays.toString(offset)+" | dc offset "+dcOffset,dcOffset<0.03);
    }

    /**
     * Check that the IllegalArgumentException is raised if the input array size is lower than 2
     */
    @Test (expected = IllegalArgumentException.class)
    public void convertRawDCOffsetTestExceptionEmpty(){
        byte[] offset = new byte[1];
        MbtDataConversion.convertRawDataToDcOffset(offset);
    }

    /**
     * Check that the IllegalArgumentException is raised if the input array is null
     */
    @Test (expected = IllegalArgumentException.class)
    public void convertRawDCOffsetTestExceptionNull(){
        byte[] offset = null;
        MbtDataConversion.convertRawDataToDcOffset(offset);
    }


}