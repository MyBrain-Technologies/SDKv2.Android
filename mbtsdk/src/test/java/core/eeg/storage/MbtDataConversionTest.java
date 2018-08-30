package core.eeg.storage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.*;
import core.bluetooth.BtProtocol;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.acquisition.MbtDataConversion;

public class MbtDataConversionTest {

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
    public void checkNoSingleNaNValuesTest() { //Check that matrix returned is the same matrix as the input matrix because it has no single channels
        int nbChannels = 2;
        int nbData = 10;
        ArrayList<ArrayList<Float>> eegMatrix = new ArrayList<>();
        ArrayList<Float> data = new ArrayList<>();
        for (int i=0; i<nbChannels; i++){
            data.add(i*1f);
        }
        for (int i=0 ; i<nbData ; i++){
            eegMatrix.add(data);
        }
        boolean containsNan = false;
        for (int i=0; i<nbData; i++){ //check before conversion : contains NaN must be true
            if(eegMatrix.get(i).size()==1)
                containsNan = true;
            assertFalse(containsNan);
        }
        containsNan = false;
        eegMatrix = MbtDataConversion.checkSingleNaNValues(eegMatrix);
        for (int i=0; i<nbData; i++){ //check after conversion : contains NaN must be true
            if(eegMatrix.get(i).size()==1)
                containsNan = true;
            assertFalse(containsNan);
        }
    }

    @Test
    public void checkSingleNaNValuesTest() { //Check that matrix with missing channels are filled by the checkSingleNanValues method
        int nbChannels = 2;
        int nbData = 10;
        ArrayList<ArrayList<Float>> eegMatrix = new ArrayList<>();
        ArrayList<Float> data = new ArrayList<>();
        for (int i=0; i<nbChannels; i++){
            if(i==0)
                data.add(i*1f);
        }
        for (int i=0 ; i<nbData ; i++){
            eegMatrix.add(data);
        }
        boolean containsNan = false;
        for (int i=0; i<nbData; i++){ //check before conversion : contains NaN must be true
            if(eegMatrix.get(i).size()==1)
                containsNan = true;

            assertTrue(containsNan);
        }
        containsNan = false;


        eegMatrix = MbtDataConversion.checkSingleNaNValues(eegMatrix);
        for (int i=0; i<nbData; i++){ //check after conversion : contains NaN must be true
            if(eegMatrix.get(i).size()==1)
                containsNan = true;

            assertFalse(containsNan);
        }
    }
}