package core.eeg.storage;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;

import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MbtDataBufferingTest {

    MbtDataBuffering buffering;
    MbtEEGManager eegManager;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        MbtManager manager = new MbtManager(context);
        //default value for protocol is BLE
        eegManager = new MbtEEGManager(context,manager, BtProtocol.BLUETOOTH_LE);
        this.buffering = new MbtDataBuffering(eegManager);
    }

    @Test
    public void storePendingBufferNullTest() {
        ArrayList< RawEEGSample> list = new ArrayList<>();
        RawEEGSample rawEEGSample = new RawEEGSample(null,null);
        list.add(rawEEGSample);
        buffering.storePendingDataInBuffer(list);
        assertTrue(buffering.getPendingRawData().size()>list.size());
    }

    @Test (expected = IllegalArgumentException.class)
    public void storePendingBufferZeroDataTest() {
        ArrayList< RawEEGSample> list = new ArrayList<>();
        ArrayList<byte[]> bytes = new ArrayList<>();
        byte[] array = new byte[10];
        Arrays.fill(array, (byte) 1 );
        bytes.add(array);
        RawEEGSample rawEEGSample = new RawEEGSample(bytes,null);
        list.add(rawEEGSample);
        buffering.storePendingDataInBuffer(list);
    }
/*
    @Test (expected = IndexOutOfBoundsException.class)
    public void storePendingBufferOutOfBondsBadLengthTest() {
        int srcPos = 0;
        int bufPos = 0;
        int length = 11; // check index out of bounds exception is raised with a wrong length
        byte[] data =  new byte[10];
        Arrays.fill(data, (byte) 1 );
        assertTrue(srcPos+length > data.length);

        buffering.storePendingDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void storePendingBufferOutOfBondsBadSrcPosTest() {
        int srcPos = 2; //check that index out of bounds exception is raised with a wrong value of srcPos
        int bufPos = 0;
        int length = 10;
        byte[] data =  new byte[10];
        Arrays.fill(data, (byte) 1 );
        assertTrue(srcPos+length > data.length);

        buffering.storePendingDataInBuffer(data,srcPos,bufPos,length);
    }


    @Test (expected = IndexOutOfBoundsException.class)
    public void storePendingBufferOutOfBondsBadBufPosTest() {
        int srcPos = 0;
        int bufPos = 1;// check that index out of bounds exception is raised with a wrong value of bufPos
        int length = 1000;
        byte[] data =  new byte[1000];
        Arrays.fill(data, (byte) 1 );
        assertTrue(bufPos+length > eegManager.getRawDataBufferSize()); // BLE buffer size = 1000

        buffering.storePendingDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IllegalArgumentException.class)
    public void storePendingBufferNullDataTest() {
        int srcPos = 0;
        int bufPos = 0;
        int length = 1;
        byte[] data =  null; //check that IllegalArgumentException is raised if data is null

        buffering.storePendingDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IllegalArgumentException.class)
    public void storePendingBufferEmptyDataTest() {
        int srcPos = 0;
        int bufPos = 0;
        int length = 1;
        byte[] data =  new byte[0]; //check that IllegalArgumentException is raised if data is empty

        buffering.storePendingDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IllegalArgumentException.class)
    public void storeOverflowBufferEmptyDataTest() {
        int srcPos = 0;
        int bufPos = 0;
        int length = 1;
        byte[] data =  new byte[0]; //check that IllegalArgumentException is raised if data is empty

        buffering.storeOverflowDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IllegalArgumentException.class)
    public void storeOverflowBufferNullDataTest() {
        int srcPos = 0;
        int bufPos = 0;
        int length = 1;
        byte[] data =  null; //check that IllegalArgumentException is raised if data is null

        buffering.storeOverflowDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void storeOverflowBufferBadLengthTest() {
        int srcPos = 0;
        int bufPos = 0;
        int length = eegManager.getRawDataPacketSize() - (eegManager.getRawDataBufferSize() - bufPos); // check that index out of bounds exception is raised with a wrong value of length
        //default value : packets size = 16 & buffer size = 1000
        byte[] data =  new byte[1000];
        Arrays.fill(data, (byte) 1 );
        assertTrue(length < 0);

        buffering.storeOverflowDataInBuffer(data,srcPos,bufPos,length);
    }

    @Test (expected = IllegalArgumentException.class)
    public void storeOverflowInPendingBufferNullOverflowTest() {
        int length = 1;
        MbtDataBuffering.setOveflowBytes(null); // check that index IllegalArgumentException is raised with a null oveflowBytes
        buffering.storeOverflowDataInPendingBuffer(length);

    }

    @Test (expected = IllegalArgumentException.class)
    public void storeOverflowInPendingBufferEmptyBufferTest() {
        int length = 1;
        byte[] overflowbytesEmpty = new byte[0]; //default value is eegManager.getRawDataPacketSize() = 16
        MbtDataBuffering.setOveflowBytes(overflowbytesEmpty); // check that index IllegalArgumentException is raised with an empty oveflowBytes
        buffering.storeOverflowDataInPendingBuffer(length);

    }

    @Test (expected = IllegalArgumentException.class)
    public void storeOverflowInPendingBufferBadLengthTest() {
        int length = 0;
        buffering.storeOverflowDataInPendingBuffer(length); // check that index IllegalArgumentException is raised with a wrong length
    }

    @Test
    public void reconfigureBuffersStatusDataNullTest() {
        int samprate = 250;
        byte samplePerNotif = (byte) 1;
        int statusByteNb = 0; // check that if statusByteNb<0 => statusData is reset to null
        buffering.reconfigureBuffers(samprate, samplePerNotif, statusByteNb);
        assertNull("NbstatusBytes: "+eegManager.getNbStatusBytes(),MbtDataBuffering.getStatusData()); // check that statusData is null as expected
    }

    @Test
    public void reconfigureBuffersStatusDataEmptyTest() {
        int samprate = 250;
        byte samplePerNotif = (byte) 1;
        int statusByteNb = 2;
        eegManager.setNbStatusBytes(3); //value is 3 for SPP

        buffering.reconfigureBuffers(samprate,samplePerNotif,statusByteNb);
        assertNotNull(MbtDataBuffering.getStatusData()); // check that statusData is not null as expected
        assertTrue(MbtDataBuffering.getStatusData().isEmpty()); //check that statusData is empty
    }

    @Test
    public void reconfigureBuffersTest() {
        int samprate = 250;
        byte samplePerNotif = (byte) 1;
        int statusByteNb = 2;
        eegManager.setRawDataPacketSize(1);

        buffering.reconfigureBuffers(samprate,samplePerNotif,statusByteNb);
        assertTrue(samplePerNotif>0);
        assertTrue(eegManager.getRawDataPacketSize()>0);
        assertFalse(Arrays.asList(MbtDataBuffering.getLostPacketInterpolator()).isEmpty()); //check that the method has well initialized the byte array LostPacketInterpolator with 0xFF value
        assertFalse(Arrays.asList(MbtDataBuffering.getLostPacketInterpolator()).isEmpty());  //check that the method has well initialized the lost packets buffer
        //assertTrue(Arrays.asList(MbtDataBuffering.getLostPacketInterpolator()).size()==3);
        assertTrue(MbtDataBuffering.getLostPacketInterpolator()[0] ==((byte)0xFF));
        assertTrue(MbtDataBuffering.getLostPacketInterpolator()[1] ==((byte)0xFF));
        assertTrue(MbtDataBuffering.getLostPacketInterpolator()[2] ==((byte)0xFF));
        assertNotNull(Arrays.asList(MbtDataBuffering.getPendingRawData()));  //check that the method has well initialized the pending buffer
        assertNotNull(Arrays.asList(MbtDataBuffering.getOveflowBytes())); //check that the method has well initialized the overflow buffer
    }

    @Test
    public void handleOverflowBufferTest() {
        buffering.handleOverflowDataBuffer(); //this method is supposed to refill the pending buffer
        assertFalse(buffering.hasOverflow()); //check that the overflow buffer has been handled
        assertTrue(MbtDataBuffering.getPendingRawData().length != 0); //check that the overflow buffer has been passed to the pending buffer
    }

    @Test (expected = IllegalArgumentException.class)
    public void handleOverflowBufferWithNullOverflowBufferTest() {
        MbtDataBuffering.setOveflowBytes(null);  //check that IllegalArgumentException is raised with a null overflow buffer
        buffering.handleOverflowDataBuffer();
    }*/
}