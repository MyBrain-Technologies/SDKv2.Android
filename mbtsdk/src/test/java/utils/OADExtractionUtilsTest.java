package utils;

import android.content.Context;
import android.content.res.AssetManager;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import core.device.model.MbtVersion;

import static org.junit.Assert.*;
import static utils.OADExtractionUtils.EXPECTED_NB_BYTES_BINARY_FILE;
import static utils.OADExtractionUtils.EXPECTED_NB_PACKETS;
import static utils.OADExtractionUtils.OAD_PACKET_SIZE;

public class OADExtractionUtilsTest {

    /**
     * Check that the extractFile method return a null array
     * if the binary file is not found
     */
    @Test(expected = FileNotFoundException.class)
    public void extractFileContent_invalid_fileNotFound() throws IOException {
        OADExtractionUtils.extractFileContent(null);
    }

    /**
     * Check that the extractFile method return a non null and non-empty array
     * if the binary file is found
     */
    @Test
    public void extractFileContent_valid_readSuccess() throws IOException {
        byte[] content = OADExtractionUtils.extractFileContent(
                this.getClass().getClassLoader().getResourceAsStream("oad/indus2/mm-ota-i2-1_7_8.bin"));

        assertNotNull(content);
        assertEquals(content.length, EXPECTED_NB_BYTES_BINARY_FILE);
    }

    /**
     * Check that the extractFile method return a null array
     * if the binary file is found but the file reading fails
     */
    @Test(expected = FileNotFoundException.class)
    public void extractFileContent_valid_readFailure() throws IOException {
        assertNull(OADExtractionUtils.extractFileContent(
                this.getClass().getClassLoader().getResourceAsStream("mm-ota-1_7_7.bin")));
    }

//    /**
//     * Check that the extractFile method return a null array
//     * if the binary file is found but the file reading fails
//     */
//    @Test(expected = IllegalArgumentException.class)
//    public void extractFileContent_invalid_formatNotMatching() throws FileNotFoundException {
//        Context context = Mockito.mock(Context.class);
//        AssetManager assetManager = Mockito.mock(AssetManager.class);
//        Mockito.doReturn(assetManager).when(context).getAssets();
//        try {
//            Mockito.doReturn(new String[]{"ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        assertNull(OADExtractionUtils.extractFileContent(
//                this.getClass().getClassLoader().getResourceAsStream("oad/ota-1_6_2.bin")));
//    }

    /**
     * Check that false is returned if the format is not matching the expected format
     */
    @Test
    public void isValidFileFormat_invalid() {
        assertFalse(OADExtractionUtils.isValidFileFormat("ota-1_6_2.bin"));
        assertFalse(OADExtractionUtils.isValidFileFormat("mm-ota-1_6_2"));
    }

    /**
     * Check that true is returned if the format is matching the expected format
     */
    @Test
    public void isValidFileFormat_valid() {
        assertTrue(OADExtractionUtils.isValidFileFormat("mm-ota-2_6_2.bin"));
        assertTrue(OADExtractionUtils.isValidFileFormat("mm-ota-1_6_7.bin"));
    }

    /**
     * Check that the expected name is returned for a given firmware version
     */
    @Test
    public void getFileNameForFirmwareVersion() {
        assertEquals(OADExtractionUtils.getFilePathForFirmwareVersion("1.2.3", new MbtVersion("1.0.0")), "oad/mm-ota-1_2_3.bin");
    }

    /**
     * Check that the expected name is returned for a given firmware version
     */
    @Test
    public void getFileNameForFirmwareVersion_regex() {
        assertEquals(OADExtractionUtils.getFilePathForFirmwareVersion("1_2_3", new MbtVersion("1.0.0")), "oad/mm-ota-1_2_3.bin");
    }

    /**
     * Check that the expected name is returned for a given firmware & hardware version
     */
    @Test
    public void getFileNameForFirmwareVersion_firmwareBasedOnHardware() {
        assertEquals("oad/mm-ota-i2-1_7_8.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1_7_8", new MbtVersion("1.0.0")));
        assertEquals("oad/mm-ota-i3-1_7_8.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1_7_8", new MbtVersion("1.1.0")));
        assertEquals("oad/mm-ota-i2-1_7_8.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1.7.8", new MbtVersion("1.0.0")));
        assertEquals("oad/mm-ota-i3-1_7_8.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1.7.8", new MbtVersion("1.1.0")));

        assertEquals("oad/mm-ota-1_7_4.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1_7_4", new MbtVersion("1.0.0")));
        assertEquals("oad/mm-ota-1_7_4.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1_7_4", new MbtVersion("1.1.0")));
        assertEquals("oad/mm-ota-1_7_4.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1.7.4", new MbtVersion("1.0.0")));
        assertEquals("oad/mm-ota-1_7_4.bin",OADExtractionUtils.getFilePathForFirmwareVersion("1.7.4", new MbtVersion("1.1.0")));
    }

    /**
     * Check that a null firmware version is returned for a null name
     */
    @Test
    public void extractFirmwareVersionFromFileName_nullName() {
        assertNull(OADExtractionUtils.extractFirmwareVersionFromFileName(null));
    }

    /**
     * Check that a null firmware version is returned for an empty name
     */
    @Test
    public void extractFirmwareVersionFromFileName_emptyName() {
        assertNull(OADExtractionUtils.extractFirmwareVersionFromFileName(""));
    }

    /**
     * Check that a null firmware version is returned for an invalid name
     */
    @Test
    public void extractFirmwareVersionFromFileName_invalidName() {
        assertNull(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-1_2_3"));
        assertNull(OADExtractionUtils.extractFirmwareVersionFromFileName("ota-1_2_3.bin"));
    }

    /**
     * Check that a non null firmware version is returned for an valid name
     */
    @Test
    public void extractFirmwareVersionFromFileName_validName() {
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-1_2_3.bin"),"1.2.3");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-1_7_4.bin"),"1.7.4");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-i2-1_7_4.bin"),"1.7.4");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-i3-1_7_4.bin"),"1.7.4");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-i2-1_7_8.bin"),"1.7.8");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-i3-1_7_8.bin"),"1.7.8");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-i2-1_7_9.bin"),"1.7.9");
        assertEquals(OADExtractionUtils.extractFirmwareVersionFromFileName("mm-ota-i3-1_7_9.bin"),"1.7.9");
    }

    /**
     * Check that a non null firmware version is returned for an non null content
     */
    @Test
    public void extractFirmwareVersionFromContent_valid(){
        byte[] content = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
        //Arrays.fill(content, (byte)1);
        for(int i = 0; i< EXPECTED_NB_BYTES_BINARY_FILE; i++){
            content[i] = (i == OADExtractionUtils.FIRMWARE_VERSION_OFFSET+1) ?
                    (byte)0 : (byte)1;
        }

        assertTrue(Arrays.toString(OADExtractionUtils.extractFirmwareVersionFromContent(content)),ArrayUtils.isEquals(OADExtractionUtils.extractFirmwareVersionFromContent(content),new byte[]{1,0}));
    }

    /**
     * Check that a null firmware version is returned for a invalid length content
     */
    @Test
    public void extractFirmwareVersionFromContent_invalid_length(){

        byte[] content = new byte[]{1};
        assertNull(OADExtractionUtils.extractFirmwareVersionFromContent(content));

    }

    /**
     * Check that a null firmware version is returned for a null content
     */
    @Test
    public void extractFirmwareVersionFromContent_invalid_null(){
        assertNull(OADExtractionUtils.extractFirmwareVersionFromContent(null));
    }

    /**
     * Check that a non null list of packets is returned for an non null content
     */
    @Test
    public void extractOADPackets_valid(){
        //content 256 000 bytes long (not multiple of 18)
        byte[] content = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
        Arrays.fill(content, (byte)1);

        ArrayList<byte[]> computedOADPackets = OADExtractionUtils.extractOADPackets(content);

        assertEquals("Expected size "+ EXPECTED_NB_PACKETS + " | Computed size "+computedOADPackets.size() ,computedOADPackets.size(), EXPECTED_NB_PACKETS);
        assertEquals(computedOADPackets.get(0)[0], 0);
        assertEquals(computedOADPackets.get(0)[1], 0);
        assertEquals(computedOADPackets.get(EXPECTED_NB_PACKETS -1)[0], (byte)142);
        assertEquals(computedOADPackets.get(EXPECTED_NB_PACKETS -1)[1], (byte)55);
        for (byte[] packet : computedOADPackets){
            assertEquals(packet.length, OAD_PACKET_SIZE);
        }
        assertEquals(computedOADPackets.get(0)[2],1);
        assertEquals(computedOADPackets.get(EXPECTED_NB_PACKETS -1)[OAD_PACKET_SIZE-1],(byte)0xFF); //remainer of Euclidian division is not 0 so the last byte is 0xFF

        //content 250 014 bytes long (multiple of 18)
        content = new byte[EXPECTED_NB_BYTES_BINARY_FILE+14];
        Arrays.fill(content, (byte)1);

        computedOADPackets = OADExtractionUtils.extractOADPackets(content);

        assertEquals("Expected size "+ EXPECTED_NB_PACKETS + " | Computed size "+computedOADPackets.size() ,computedOADPackets.size(), EXPECTED_NB_PACKETS);
        assertEquals(computedOADPackets.get(0)[0], 0);
        assertEquals(computedOADPackets.get(0)[1], 0);
        assertEquals(computedOADPackets.get(EXPECTED_NB_PACKETS -1)[0], (byte)142);
        assertEquals(computedOADPackets.get(EXPECTED_NB_PACKETS -1)[1], (byte)55);
        for (byte[] packet : computedOADPackets){
            assertEquals(packet.length, OAD_PACKET_SIZE);
        }
        assertEquals(computedOADPackets.get(0)[2],1);
        assertEquals(computedOADPackets.get(EXPECTED_NB_PACKETS -1)[OAD_PACKET_SIZE-1],1); //remainer of Euclidian division is not 0 so the last byte is 0xFF
    }

    /**
     * Check that a null list of packets is returned for a null content
     */
    @Test
    public void extractOADPackets_invalid_null(){
        assertNull(OADExtractionUtils.extractOADPackets(null));
    }

    /**
     * Check that an list of packets is returned for an empty content
     */
    @Test
    public void extractOADPackets_invalid_empty(){
        assertTrue(OADExtractionUtils.extractOADPackets(new byte[]{}).size() == 1);
    }

    /**
     * Check that a null array is returned if the assets are null
     */
    @Test
    public void getAvailableFirmwareVersions_invalid_null(){
        assertNull(OADExtractionUtils.getAvailableFirmwareVersions(null, new MbtVersion("1.0.0")));
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Context context = Mockito.mock(Context.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        assertNull(OADExtractionUtils.getAvailableFirmwareVersions(assetManager, null));
    }

    /**
     * Check that an empty array is returned if the assets does not contains any file
     */
    @Test
    public void getAvailableFirmwareVersions_empty(){
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{}).when(assetManager).list("oad/indus2");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(0, OADExtractionUtils.getAvailableFirmwareVersions(assetManager, new MbtVersion("1.0.0")).length);
    }

    /**
     * Check that a non empty array is returned if the assets does contains OAD files
     */
    @Test
    public void getAvailableFirmwareVersions_foundFiles(){
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{"mm-ota-1_7_4.bin"}).when(assetManager).list("oad/indus2");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(1,OADExtractionUtils.getAvailableFirmwareVersions(assetManager,new MbtVersion("1.0.0")).length);
    }

        /**
         * Check that the right directory is returned
         * if the hardware version is indus2
         */
        @Test
        public void getBinaryDirectory_indus2(){
            final String expectedDirectory = "oad/indus2";

            MbtVersion version = new MbtVersion("1.0.0");
            assertEquals(expectedDirectory, OADExtractionUtils.getBinaryDirectory(version));

            version = new MbtVersion("1.0.1");
            assertEquals(expectedDirectory, OADExtractionUtils.getBinaryDirectory(version));
        }

        /**
         * Check that the right directory is returned
         * if the hardware version is indus3
         */
        @Test
        public void getBinaryDirectory_indus3(){
            final String expectedDirectory = "oad/indus3";

            MbtVersion version = new MbtVersion("1.1.0");
            assertEquals(expectedDirectory, OADExtractionUtils.getBinaryDirectory(version));

            version = new MbtVersion("1.1.1");
            assertEquals(expectedDirectory, OADExtractionUtils.getBinaryDirectory(version));
        }

        /**
         * Check that the default directory is returned
         * if the hardware version is unknown
         */
        @Test
        public void getBinaryDirectory_default(){
            final String expectedDirectory = "oad/indus2";

            MbtVersion version = new MbtVersion("0.0.0");
            assertEquals(expectedDirectory, OADExtractionUtils.getBinaryDirectory(version));

            version = new MbtVersion("0.0.1");
            assertEquals(expectedDirectory, OADExtractionUtils.getBinaryDirectory(version));
        }
}