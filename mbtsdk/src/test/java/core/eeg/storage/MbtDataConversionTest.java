package core.eeg.storage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.*;
import core.bluetooth.BtProtocol;

public class MbtDataConversionTest {

    @Test (expected = NullPointerException.class)
    public void convertRawDataToEEGNullDataTest() {
        byte[] data = null;  //check that IllegalArgumentException is raised if data is null
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 2;
        MbtDataConversion.convertRawDataToEEG(data, protocol, nbChannels);
    }

    @Test (expected = ArithmeticException.class)
    public void convertRawDataToEEGZeroChannelTest() {
        byte[] data = new byte[250];
        for(int i=0;i<250;i++){
            data[i] = (byte) 1;
        }
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 0; //if nbChannels =0, then divider =0 then we expect that length/divider will raise Arithmetic exception
        MbtDataConversion.convertRawDataToEEG(data, protocol, nbChannels);
    }

    @Test (expected = IllegalArgumentException.class)
    public void convertRawDataToEEG250RawDataTest() {
        byte[] data = new byte[251]; //check that if data.length % 250 is not equals 0, exception is raised
        Arrays.fill(data, (byte)1);
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 2;
        MbtDataConversion.convertRawDataToEEG(data, protocol, nbChannels);
    }

    @Test
    public void convertRawDataToEEGEmptyDataTest() {
        byte[] data = new byte[0]; // check that the method will return empty result if input data is empty
        BtProtocol protocol = BtProtocol.BLUETOOTH_LE;
        int nbChannels = 2;
        ArrayList<ArrayList<Float>> eegData = MbtDataConversion.convertRawDataToEEG(data, protocol, nbChannels);
        assertTrue(eegData.size()==nbChannels);
        assertTrue(eegData.get(0).isEmpty());
        assertTrue(eegData.get(1).isEmpty());
    }
}