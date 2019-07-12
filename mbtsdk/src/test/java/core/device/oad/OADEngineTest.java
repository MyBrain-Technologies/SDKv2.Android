package core.device.oad;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;

import utils.FileUtils;

import static org.junit.Assert.*;

@RunWith( PowerMockRunner.class )
@PrepareForTest(FileUtils.class)
public class OADEngineTest {

    private static byte[] extractedFile = new byte[OADEngine.BUFFER_LENGTH];
    private static final byte byteValue = 0x01;

    private OADEngine oadEngine;

    @Before
    public void setUp() throws Exception {
        Arrays.fill(extractedFile, byteValue);
        oadEngine = new OADEngine();
    }


    /**
     * Check that the SDK raises an exception and the ERROR_PREPARING_UPDATE_FAILED error
     * if the binary file is not found
     */
    @Test
    public void prepareOADFile_invalid_fileNotFound(){
        PowerMockito.spy(FileUtils.class);

        try {
            PowerMockito.doReturn(extractedFile)
                    .when(FileUtils.class, "extractFile", Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Check that the SDK raises the ERROR_PREPARING_UPDATE_FAILED error
     * if the binary file is not complete
     */
    @Test
    public void prepareOADFile_invalid_fileIncomplete(){

    }

    /**
     * Check that the SDK does not raise any error
     * if the binary file is found & complete
     * Also check it returns a buffer of {@link core.device.oad.OADEngine#BUFFER_LENGTH} elements
     * of {@link OADEngine#CHUNK_NB_BYTES} byte each.
     * Also check that the file length is encoded on {@link OADEngine#FILE_LENGTH_NB_BYTES} bytes
     * and that the firmware version is encoded on {@link OADEngine#FILE_LENGTH_NB_BYTES} bytes
     */
    @Test
    public void prepareOADFile_valid(){

    }

    /**
     * Check that the SDK raises the ERROR_REJECTED_REQUEST error
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_FAILED} for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event
     */
    @Test
    public void initiateOADRequest_invalid(){

    }

    /**
     * Check that the SDK does not raise any error
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_SUCCESS} for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event
     */
    @Test
    public void initiateOADRequest_valid(){

    }

    /**
     * Check that the SDK raises the ERROR_TIMEOUT_REQUEST error
     * if the headset device does not return any response within the allocated time for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event
     */
    @Test
    public void initiateOADRequest_timeout(){

    }

    /**
     * Check that the SDK raises the ERROR_TIMEOUT_TRANSFER error
     * if the headset device does not return any response within the allocated time for a write characteristic operation
     */
    @Test
    public void transferOADFile_timeout(){

    }
}