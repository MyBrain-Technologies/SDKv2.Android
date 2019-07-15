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

@RunWith( PowerMockRunner.class )
@PrepareForTest(FileUtils.class)
public class OADEngineTest {

    private static byte[] extractedFile = new byte[OADManager.BUFFER_LENGTH];
    private static final byte byteValue = 0x01;

    private OADManager oadEngine;

    @Before
    public void setUp() throws Exception {
        Arrays.fill(extractedFile, byteValue);
        oadEngine = new OADManager();
    }


    /**
     * Check that the OAD internal state is set to {@link OADState#UPDATE_NEEDED}
     * if the firmware is not up-to-date.
     */
    @Test
    public void isFirmwareVersionUpToDate_false(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#UP_TO_DATE}
     * if the firmware is up-to-date.
     */
    @Test
    public void isFirmwareVersionUpToDate_true(){

    }

    /**
     * Check that the SDK raises an exception and the ERROR_PREPARING_UPDATE_FAILED error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not found.
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
     * Check that the OAD internal state is reset to
     * {@link OADState#IDLE} if the {@link OADState#ABORTED} state is raised.
     */
    @Test
    public void notifyOADStateChanged_Aborted(){

    }

    /**
     * Check that the SDK raises the ERROR_PREPARING_UPDATE_FAILED error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not complete.
     */
    @Test
    public void prepareOADFile_invalid_fileIncomplete(){

    }

    /**
     * Check that the SDK OAD internal state is set to {@link OADState#PREPARING_REQUEST}
     * if the binary file is found & complete.
     * Also check it returns a buffer of {@link OADManager#BUFFER_LENGTH} elements
     * of {@link OADManager#CHUNK_NB_BYTES} byte each.
     * Also check that the file length is encoded on {@link OADManager#FILE_LENGTH_NB_BYTES} bytes
     * and that the firmware version is encoded on {@link OADManager#FILE_LENGTH_NB_BYTES} bytes.
     */
    @Test
    public void prepareOADFile_valid(){

    }

    /**
     * Check that the SDK aborts the OAD process
     * and the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void prepareOADFile_disconnection(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#SENDING_VALIDATION_REQUEST}
     * once the OAD request is ready.
     * Also check that the OAD internal state is set to {@link OADState#VALIDATION_REQUEST_SENT}
     * once the {@link OADState#SENDING_VALIDATION_REQUEST} state is triggered.
     */
    @Test
    public void initiateOADRequest_request(){

    }

    /**
     * Check that the SDK raises the ERROR_REJECTED_REQUEST error
     * and that the OAD internal state is set to {@link OADState#DEVICE_REJECTED}
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_FAILED}
     * for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void initiateOADRequest_failureResponse(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#DEVICE_VALIDATED}
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_SUCCESS}
     * for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void initiateOADRequest_SuccessResponse(){

    }

    /**
     * Check that the SDK raises the ERROR_TIMEOUT_REQUEST error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void initiateOADRequest_timeout(){

    }

    /**
     * Check that the SDK aborts the OAD process
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void initiateOADRequest_disconnection(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#TRANSFER_STARTED}
     * if the SDK starts the OAD packets transfer.
     */
    @Test
    public void transferOADFile_started(){

    }

    /**
     * Check that the SDK raises a Bluetooth error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the device fail to perform a write characteristic operation.
     */
    @Test
    public void transferOADFile_failureNotStarted(){

    }

    /**
     * Check that the SDK raises the ERROR_TIMEOUT_TRANSFER error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a write characteristic operation.
     */
    @Test
    public void transferOADFile_timeout(){

    }

    /**
     * Check that the SDK raises a Bluetooth error
     * aborts the OAD process,
     * and set the OAD internal state to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void transferOADFile_disconnection(){

    }

    /**
     * Check that the SDK resets the internal packet index
     * to the value received in the headset response
     * if the headset device returns a valid packet index
     * for a {@link command.DeviceCommandEvents#MBX_OTA_IDX_RESET_EVT} event.
     */
    @Test
    public void transferOADFile_lostPacket_validIndex(){

    }

    /**
     * Check that the SDK raises an Out Of Bounds Exception and abort the OAD process
     * if the headset device notifies that a packet
     * with index beyond the size of the SDK buffer has been lost during transfer
     * for a {@link command.DeviceCommandEvents#MBX_OTA_IDX_RESET_EVT} event.
     */
    @Test
    public void transferOADFile_lostPacket_invalidIndex(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#TRANSFER_COMPLETE}
     * if all the OAD packets has been sent.
     * Also check that the OAD internal state is set to {@link OADState#WAITING_DEVICE_READBACK}
     * once the {@link OADState#TRANSFER_COMPLETE} state is triggered.
     */
    @Test
    public void transferOADFile_complete(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#READBACK_SUCCESS}
     * if the headset device returns a success readback response.
     */
    @Test
    public void transferOADFile_success(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns a failure readback response.
     */
    @Test
    public void transferOADFile_failureUncomplete(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#REBOOT}
     * once the {@link OADState#READBACK_SUCCESS} state is triggered and the headset is disconnected.
     */
    @Test
    public void reboot_success(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the {@link OADState#READBACK_SUCCESS} state is triggered
     * and the headset failed to disconnect within the allocated time.
     */
    @Test
    public void reboot_timeout(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#RECONNECTING}
     * once the reboot has been performed.
     */
    @Test
    public void reconnect_started(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#VERIFYING_FIRMWARE_VERSION}
     * once the headset device is reconnected.
     */
    @Test
    public void reconnect_success(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#UP_TO_DATE}
     * if the SDK receives a firmware version equal to the last released version.
     */
    @Test
    public void verifyingFirmwareVersion_success(){

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the SDK receives a firmware version not equal to the last released version.
     */
    @Test
    public void verifyingFirmwareVersion_failure(){

    }


}