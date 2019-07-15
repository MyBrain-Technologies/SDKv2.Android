package core.device.oad;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileNotFoundException;
import java.util.Arrays;

import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import engine.clientevents.OADStateListener;
import utils.FileUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith( PowerMockRunner.class )
@PrepareForTest(FileUtils.class)
public class OADManagerTest {

    private final String NOT_FOUND_FILE_PATH = "anyString";
    private static byte[] completeExtractedFile = new byte[OADManager.BUFFER_LENGTH];
    private static byte[] uncompleteExtractedFile = new byte[OADManager.BUFFER_LENGTH-1];
    private static final byte byteValue = 0x01;

    private OADManager oadManager;

    @Before
    public void setUp() throws Exception {
        Arrays.fill(completeExtractedFile, byteValue);
        Arrays.fill(uncompleteExtractedFile, byteValue);
        oadManager = new OADManager(Mockito.mock(Context.class));
    }


    /**
     * Check that the isFirmwareVersionUpToDate method returns false
     * if the firmware is not up-to-date.
     */
    @Test
    public void isFirmwareVersionUpToDate_false(){
        String firmwareVersion = "1.0.0";
        assertFalse(oadManager.isFirmwareVersionUpToDate(firmwareVersion));
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#COMPLETE}
     * if the firmware is up-to-date.
     */
    @Test
    public void isFirmwareVersionUpToDate_true(){
        String firmwareVersion = "1.7.4";
        assertTrue(oadManager.isFirmwareVersionUpToDate(firmwareVersion));
    }

    /**
     * Check that the SDK raises an exception and the ERROR_PREPARING_UPDATE_FAILED error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not found.
     */
    @Test(expected = FileNotFoundException.class)
    public void prepareOADFile_invalid_fileNotFound(){
        PowerMockito.spy(FileUtils.class);

        try {
            PowerMockito.doReturn(null)
                    .when(FileUtils.class, "extractFile", Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertFalse(oadManager.prepareOADFile(NOT_FOUND_FILE_PATH));

    }

    /**
     * Check that the OAD internal state is reset 
     * if the {@link OADState#ABORTED} state is raised.
     */
    @Test
    public void notifyOADStateChanged_Aborted(){
        oadManager.abort(null);
        assertNull(oadManager.getCurrentState());
    }

    /**
     * Check that the SDK raises the ERROR_PREPARING_UPDATE_FAILED error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not complete.
     */
    @Test
    public void prepareOADFile_invalid_fileIncomplete(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_PREPARING_REQUEST_FAILED);
            }
        });

        PowerMockito.spy(FileUtils.class);

        try {
            PowerMockito.doReturn(uncompleteExtractedFile)
                    .when(FileUtils.class, "extractFile", Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertFalse(oadManager.prepareOADFile(NOT_FOUND_FILE_PATH));
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
    }

    /**
     * Check that the SDK OAD internal state is set to {@link OADState#INIT}
     * if the binary file is found & complete.
     * Also check it returns a buffer of {@link OADManager#BUFFER_LENGTH} elements
     * of {@link OADManager#CHUNK_NB_BYTES} byte each.
     * Also check that the file length is encoded on {@link OADManager#FILE_LENGTH_NB_BYTES} bytes
     * and that the firmware version is encoded on {@link OADManager#FILE_LENGTH_NB_BYTES} bytes.
     */
    @Test
    public void prepareOADFile_valid(){
        assertEquals(oadManager.getCurrentState(), OADState.INIT);

    }

    /**
     * Check that the SDK aborts the OAD process
     * and the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void prepareOADFile_disconnection(){

        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#FIRMWARE_VALIDATION}
     * once the OAD request is ready.
     */
    @Test
    public void initiateOADRequest_request(){
        assertEquals(oadManager.getCurrentState(), OADState.FIRMWARE_VALIDATION);

    }

    /**
     * Check that the SDK raises the ERROR_REJECTED_REQUEST error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_FAILED}
     * for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void initiateOADRequest_failureResponse(){

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#TRANSFERRING}
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_SUCCESS}
     * for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void initiateOADRequest_SuccessResponse(){
        assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);

    }

    /**
     * Check that the SDK raises the ERROR_TIMEOUT_REQUEST error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void initiateOADRequest_timeout(){

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the SDK aborts the OAD process
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void initiateOADRequest_disconnection(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#TRANSFERRING}
     * if the SDK starts the OAD packets transfer.
     */
    @Test
    public void transferOADFile_started(){
        assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);

    }

    /**
     * Check that the SDK raises a Bluetooth error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the device fail to perform a write characteristic operation.
     */
    @Test
    public void transferOADFile_failureNotStarted(){

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the SDK raises the ERROR_TIMEOUT_TRANSFER error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a write characteristic operation.
     */
    @Test
    public void transferOADFile_timeout(){

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the SDK raises a Bluetooth error
     * aborts the OAD process,
     * and set the OAD internal state to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void transferOADFile_disconnection(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

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
     * Check that the OAD internal state is set to {@link OADState#AWAITING_DEVICE_READBACK}
     * if all the OAD packets has been sent.
     */
    @Test
    public void transferOADFile_complete(){
        assertEquals(oadManager.getCurrentState(), OADState.AWAITING_DEVICE_READBACK);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#READBACK_SUCCESS}
     * if the headset device returns a success readback response.
     */
    @Test
    public void transferOADFile_success(){
        assertEquals(oadManager.getCurrentState(), OADState.READBACK_SUCCESS);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns a failure readback response.
     */
    @Test
    public void transferOADFile_failureUncomplete(){

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#REBOOTING}
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

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

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
     * Check that the OAD internal state is set to {@link OADState#COMPLETE}
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

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }


}