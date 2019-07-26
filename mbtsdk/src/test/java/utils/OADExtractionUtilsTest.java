package utils;

import android.content.Context;
import android.content.res.AssetManager;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import core.device.oad.OADManager;

import static org.junit.Assert.*;
import static utils.OADExtractionUtils.EXPECTED_NB_BYTES_BINARY_FILE;
import static utils.OADExtractionUtils.OAD_PACKET_SIZE;
import static utils.OADExtractionUtils.OAD_PAYLOAD_PACKET_SIZE;

public class OADExtractionUtilsTest {

    /**
     * Check that the extractFile method return a null array
     * if the binary file is not found
     */
    @Test(expected = FileNotFoundException.class)
    public void extractFileContent_invalid_fileNotFound() throws FileNotFoundException {
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
        } catch (IOException e) {
            e.printStackTrace();
        }

        OADExtractionUtils.extractFileContent(assetManager,"mm-ota-1_6_9.bin");
    }

    /**
     * Check that the extractFile method return a non null and non-empty array
     * if the binary file is found
     */
    @Test
    public void extractFileContent_valid_readSuccess() throws FileNotFoundException {
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        InputStream inputStream = Mockito.mock(InputStream.class);

        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
            Mockito.doReturn(inputStream).when(assetManager).open(Mockito.anyString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] content = OADExtractionUtils.extractFileContent(assetManager,"mm-ota-1_6_2.bin");

        assertNotNull(content);
        assertEquals(content.length, EXPECTED_NB_BYTES_BINARY_FILE);
    }

    /**
     * Check that the extractFile method return a null array
     * if the binary file is found but the file reading fails
     */
    @Test
    public void extractFileContent_valid_readFailure() throws FileNotFoundException {
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNull(OADExtractionUtils.extractFileContent(assetManager,"mm-ota-1_6_2.bin"));
    }

    /**
     * Check that the extractFile method return a null array
     * if the binary file is found but the file reading fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void extractFileContent_invalid_formatNotMatching() throws FileNotFoundException {
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{"ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNull(OADExtractionUtils.extractFileContent(assetManager,"ota-1_6_2.bin"));
    }

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
        assertEquals(OADExtractionUtils.getFileNameForFirmwareVersion("1.2.3"), "mm-ota-1_2_3.bin");
    }

    /**
     * Check that the expected name is returned for a given firmware version
     */
    @Test
    public void getFileNameForFirmwareVersion_regex() {
        assertEquals(OADExtractionUtils.getFileNameForFirmwareVersion("1_2_3"), "mm-ota-1_2_3.bin");
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
    }

    /**
     * Check that a non null firmware version is returned for an non null content
     */
    @Test
    public void extractFirmwareVersionFromContent_valid(){
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        InputStream inputStream = Mockito.mock(InputStream.class);

        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
            Mockito.doReturn(inputStream).when(assetManager).open(Mockito.anyString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] content = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
        for(int i = 0; i< EXPECTED_NB_BYTES_BINARY_FILE; i++){
            content[i] = (i == OADExtractionUtils.FIRMWARE_VERSION_OFFSET+1) ?
                    (byte)7 : (byte)1;
        }

        assertEquals(OADExtractionUtils.extractFirmwareVersionFromContent(content),"1.7.1");

    }

    /**
     * Check that a non null list of packets is returned for an non null content
     */
    @Test
    public void extractOADPackets_valid(){

        byte[] content = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
        Arrays.fill(content, (byte)1);

        ArrayList<byte[]> computedOADPackets = OADExtractionUtils.extractOADPackets(content);
        assertEquals("Expected size "+OADManager.EXPECTED_NB_PACKETS_BINARY_FILE+ " | Computed size "+computedOADPackets.size() ,computedOADPackets.size(), OADManager.EXPECTED_NB_PACKETS_BINARY_FILE);

    }
    /**
     * Check that a null firmware version is returned for a invalid length content
     */
    @Test
    public void extractFirmwareVersionFromContent_invalid_length(){
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        InputStream inputStream = Mockito.mock(InputStream.class);

        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
            Mockito.doReturn(inputStream).when(assetManager).open(Mockito.anyString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] content = new byte[]{1};
        assertNull(OADExtractionUtils.extractFirmwareVersionFromContent(content));

    }

    /**
     * Check that a null firmware version is returned for a null content
     */
    @Test
    public void extractFirmwareVersionFromContent_invalid_null(){
        Context context = Mockito.mock(Context.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        InputStream inputStream = Mockito.mock(InputStream.class);

        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
            Mockito.doReturn(inputStream).when(assetManager).open(Mockito.anyString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNull(OADExtractionUtils.extractFirmwareVersionFromContent(null));

    }
}