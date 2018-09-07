package core.eeg.storage;

import android.content.Context;

import com.ibm.icu.text.ArabicShaping;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;

import static features.MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MbtDataBufferingTest {

    MbtDataBuffering buffering;
    MbtEEGManager eegManager;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        //default value for protocol is BLE
        eegManager = new MbtEEGManager(context,new MbtManager(context), BtProtocol.BLUETOOTH_LE);
        this.buffering = new MbtDataBuffering(eegManager);
    }

    /**
     * Check that the pending raw data size after adding the new values is coherent/consistent according to the input size
     * in case the buffer contains less than MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE elements (the buffer is not full) after storage in the buffer
     */
    @Test
    public void storePendingBufferNotFullTest() {
        ArrayList<byte[]> bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{-13, -29});
        bytesArrayList.add(new byte[]{68, 59});
        Float status = Float.NaN;

        assertNotNull(buffering.getPendingRawData());
        assertTrue(buffering.getPendingRawData().size()==0);

        buffering.getPendingRawData().add(new RawEEGSample(bytesArrayList,status));
        int sizeBefore = buffering.getPendingRawData().size();

        ArrayList< RawEEGSample> list = new ArrayList<>();
        list.add(new RawEEGSample(bytesArrayList,status));
        list.add(new RawEEGSample(null,Float.NaN));

        buffering.storePendingDataInBuffer(list);
        assertTrue(buffering.getPendingRawData().size()==list.size()+sizeBefore);
        for (RawEEGSample rawEEGSample : list){
            assertTrue(buffering.getPendingRawData().contains(rawEEGSample));
        }

        buffering.storePendingDataInBuffer(list);
        assertFalse(buffering.getPendingRawData().isEmpty());
        assertTrue("size pending raw data: "+buffering.getPendingRawData().size(),buffering.getPendingRawData().size() == 2*list.size()+sizeBefore);

    }

    /**
     * Check that the pending raw data has been cleared in case the buffer is full after storage of the new data in the buffer
     */
    @Test
    public void storePendingBufferFullTest() {

        ArrayList<byte[]> bytesArrayList = new ArrayList<>();
        bytesArrayList.add(new byte[]{-13, -29});
        bytesArrayList.add(new byte[]{68, 59});
        Float status = Float.NaN;

        ArrayList< RawEEGSample> list = new ArrayList<>();
        list.add(new RawEEGSample(bytesArrayList,status));
        list.add(new RawEEGSample(bytesArrayList,status));
        list.add(new RawEEGSample(bytesArrayList,status));
        list.add(new RawEEGSample(bytesArrayList,status));
        list.add(new RawEEGSample(bytesArrayList,status));

        for (int i = 0 ; i< DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE/list.size() ; i++){
            buffering.storePendingDataInBuffer(list);
        }

        assertTrue(" pending raw data size"+buffering.getPendingRawData().size(),buffering.getPendingRawData().isEmpty());
    }

    /**
     * Check that the whole matrix of converted EEG data is well stored in the packet buffer
     * in this case the buffer contains less than 250 values (buffer not full)
     */
    @Test
    public void storeConsolidatedEegInPacketBufferTestNotFull() {
        int size = 249;
        ArrayList<ArrayList<Float>> consolidatedEEG = new ArrayList<>();
        ArrayList<Float> status = new ArrayList<>();
        for (int i = 0 ; i < size ; i++){
            consolidatedEEG.add(new ArrayList<>(Arrays.asList(1f,2f)));
            status.add(Float.NaN);
        }
        buffering.storeConsolidatedEegInPacketBuffer(consolidatedEEG,status);
        assertTrue(buffering.getTestMbtEEGPacketsBuffer().getChannelsData().size()==size);
    }

    /**
     * Check that the whole matrix of converted EEG data is well stored in the packet buffer
     * in this case the buffer contains exactly 250 values, so there is no overflow
     */
    @Test
    public void storeConsolidatedEegInPacketBufferTestFullNoOverflow() {
        int size = 250;
        ArrayList<ArrayList<Float>> consolidatedEEG = new ArrayList<>();
        ArrayList<Float> status = new ArrayList<>();
        for (int i = 0 ; i < size ; i++){
            consolidatedEEG.add(new ArrayList<>(Arrays.asList(1f,2f)));
            status.add(Float.NaN);
        }
        buffering.storeConsolidatedEegInPacketBuffer(consolidatedEEG,status);
        assertTrue("size "+buffering.getTestMbtEEGPacketsBuffer().getChannelsData().size(),buffering.getTestMbtEEGPacketsBuffer().getChannelsData().size()==0);
    }
    /**
     * Check that the whole matrix of converted EEG data is well stored in the packet buffer
     * in this case the buffer contains more than 250 values, so there is overflow
     * the buffer is reset and the overflow is stored in this reset buffer.
     */
    @Test
    public void storeConsolidatedEegInPacketBufferTestFullWithOverflow() {
        int overflow = 2;
        int size = 250+overflow;
        ArrayList<ArrayList<Float>> consolidatedEEG = new ArrayList<>();
        ArrayList<Float> status = new ArrayList<>();
        for (int i = 0 ; i < size ; i++){
            consolidatedEEG.add(new ArrayList<>(Arrays.asList(1f,2f)));
            status.add(Float.NaN);
        }
        buffering.storeConsolidatedEegInPacketBuffer(consolidatedEEG,status);
        assertTrue("size "+buffering.getTestMbtEEGPacketsBuffer().getChannelsData().size(),buffering.getTestMbtEEGPacketsBuffer().getChannelsData().size()==overflow);
    }

    /**
     * Check that all the buffers have been cleared and that the EEG packet buffer have been recreated by checking the timestamp
     */
    @Test
    public void reinitBuffersTest(){
        ArrayList<ArrayList<Float>> matrix = new ArrayList<>();
        matrix.add(new ArrayList<>(Arrays.asList(-0.001773772f, 0.009991123f)));
        matrix.add(new ArrayList<>(Arrays.asList(0.013030159f, -1.30416E-4f)));
        matrix.add(new ArrayList<>(Arrays.asList(-0.0042831358f, -0.0032558239f)));

        buffering.setTestMbtEEGPacketsBuffer(new MbtEEGPacket(matrix));
        buffering.setTestPendingRawData(new ArrayList<>(Arrays.asList(new RawEEGSample(null,Float.NaN))));
        assertFalse(buffering.getTestMbtEEGPacketsBuffer().isEmpty());
        assertFalse(buffering.getPendingRawData().isEmpty());

        long timestamp = System.currentTimeMillis();
        buffering.reinitBuffers();
        assertTrue(buffering.getPendingRawData().isEmpty());
        assertTrue(buffering.getTestMbtEEGPacketsBuffer().isEmpty());
        assertTrue(buffering.getTestMbtEEGPacketsBuffer().getTimeStamp() == timestamp);
    }

}