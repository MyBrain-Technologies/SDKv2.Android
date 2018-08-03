package core.eeg.signalprocessing;
import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import core.eeg.storage.MbtEEGPacket;

import static org.junit.Assert.*;

public class MBTCalibratorTest {

    @Before
    public void setUp() throws Exception {
        try {
            System.loadLibrary("mbtalgo_2.3.1");
        } catch (@NonNull final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    @Test
    public void calibrateNewTest() {
        final int sampRate = 250;
        final int packetLength = 250;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());//channels data size must equals 2
        for (int i=0; i<channelsData.size(); i++){
            for (int j=0; j<sampRate; j++) {
                channelsData.get(i).add(new Random().nextFloat());
            }
        }
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData);
        HashMap<String, float[]> map = MBTCalibrator.calibrateNew(sampRate, packetLength, ContextSP.smoothingDuration, packets);
        assertNotNull("MAP "+map.toString(), map);
    }

    /*@Test (expected = IndexOutOfBoundsException.class)
    public void calibrateNewBadQualitiesSizeTest() {
        final int sampRate = 250;
        final int packetLength = 1;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());//channels data size must equals 2
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F); //check that IndexOutOfBoundsException is raised if qualities.size<2
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        HashMap<String, float[]> map = MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
        assertNull(map);
    }

    @Test
    public void calibrateNewZeroSampRateTest() {
        final int sampRate = 0;
        final int packetLength = 1;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());//channels data size must equals 2
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        HashMap<String, float[]> map = MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
        assertNotNull(map);
    }

    @Test (expected = IllegalArgumentException.class)
    public void calibrateNewNegativeSampRateTest() {
        final int sampRate = -1; //check that IllegalArgumentException is raised if samprate is negative
        final int packetLength = 0;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());//channels data size must equals 2
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void calibrateNewNegativePacketLengthTest() {
        final int sampRate = 0;
        final int packetLength = -1; //check that IllegalArgumentException is raised if packetsLength is negative
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());//channels data size must equals 2
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
    }

    @Test (expected = NullPointerException.class)
    public void calibrateNewPacketsNullTest() {
        final int sampRate = 0;
        final int packetLength = 0;
        final MbtEEGPacket packets = null; //check that IllegalArgumentException is raised if packets is null
        MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
    }

    @Test (expected = IllegalArgumentException.class)
    public void calibrateNewPacketsEmptyTest() {
        final int sampRate = 0;
        final int packetLength = 0;
        final MbtEEGPacket[] packets = new MbtEEGPacket[0]; //check that IllegalArgumentException is raised if packets.size = 0
        MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
    }

    @Test
    public void calibrateNewPacketsQualitiesTest() {
        final int sampRate = 0;
        final int packetLength = 0;
        final ArrayList<ArrayList<Float>> channelsData = new ArrayList<ArrayList<Float>>();
        channelsData.add(new ArrayList<Float>());
        channelsData.add(new ArrayList<Float>());//channels data size must equals 2
        ArrayList<Float> qualities = new ArrayList<Float>();
        qualities.add(1F);
        qualities.add(1F);
        long timestamp = 1L;
        final MbtEEGPacket packets = new MbtEEGPacket(channelsData, qualities, null, timestamp);
        HashMap<String, float[]> map = MBTCalibrator.calibrateNew(sampRate, packetLength, packets);
        if(packets.getQualities()!=null)
            assertFalse(packets.getQualities().isEmpty()); //check that packets qualities is not empty

    }*/
}