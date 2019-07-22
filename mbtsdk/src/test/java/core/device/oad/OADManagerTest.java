package core.device.oad;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import command.DeviceCommands;
import core.bluetooth.BtState;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.bluetooth.requests.StartOrContinueConnectionRequestEvent;
import core.device.DeviceEvents;
import core.device.MbtDeviceManager;
import engine.clientevents.BaseError;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import engine.clientevents.OADStateListener;
import eventbus.MbtEventBus;
import utils.FileUtils;
import utils.MbtAsyncWaitOperation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith( PowerMockRunner.class )
@PrepareForTest({MbtEventBus.class, FileUtils.class})
public class OADManagerTest {

    private final String FILE_PATH = "anyString";
    private final String FIRMWARE_VERSION_VALID = "1.7.4";
    private final String FIRMWARE_VERSION_INVALID_LOWER = "1.0.0";
    private final String FIRMWARE_VERSION_INVALID_LENGTH = "1.0";
    private final int BINARY_FILE_LENGTH_INVALID = 5;

    private static byte[] completeExtractedFile = new byte[OADManager.INT];
    private static byte[] incompleteExtractedFile = new byte[OADManager.INT -1];
    private static final byte byteValue = 0x01;

    private OADManager oadManager;

    @Before
    public void setUp() {
        Arrays.fill(completeExtractedFile, byteValue);
        Arrays.fill(incompleteExtractedFile, byteValue);
        oadManager = new OADManager(Mockito.mock(Context.class), Mockito.mock(MbtDeviceManager.class));
    }

    /**
     * Check that the isFirmwareVersionUpToDate method returns false
     * if the firmware is not valid (one digit is missing).
     */
    @Test
    public void isFirmwareVersionUpToDate_false_wrongLength(){
        assertFalse(oadManager.isFirmwareVersionUpToDate(FIRMWARE_VERSION_INVALID_LENGTH));
    }

    /**
     * Check that the isFirmwareVersionUpToDate method returns false
     * if the firmware is not up-to-date.
     */
    @Test
    public void isFirmwareVersionUpToDate_false_lowerVersion(){
        assertFalse(oadManager.isFirmwareVersionUpToDate(FIRMWARE_VERSION_INVALID_LOWER));
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#COMPLETE}
     * if the firmware is up-to-date.
     */
    @Test
    public void isFirmwareVersionUpToDate_true(){
        assertTrue(oadManager.isFirmwareVersionUpToDate(FIRMWARE_VERSION_VALID));
    }

    /**
     * Check that the SDK raises an exception and the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not found.
     */
    @Test(expected = FileNotFoundException.class)
    public void init_invalid_fileNotFound(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {
                assertEquals(progress, 0);
            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_INIT_FAILED);
            }
        });

        PowerMockito.spy(FileUtils.class);

        try { //mock the result returned by the extractFile method
            PowerMockito.doThrow(new FileNotFoundException("File path/name incorrect : "+FILE_PATH))
                    .when(FileUtils.class, "extractFile", Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertFalse(oadManager.init(FILE_PATH));
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is reset 
     * if the {@link OADState#ABORTED} state is raised.
     */
    @Test
    public void notifyOADStateChanged_Aborted(){
        oadManager.abort(null);
        assertEquals(OADState.ABORTED,oadManager.getCurrentState());
    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not complete.
     */
    @Test
    public void init_invalid_fileIncomplete(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {
                assertEquals(progress, 0);
            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_INIT_FAILED);
            }
        });
        PowerMockito.spy(FileUtils.class);

        try {
            PowerMockito.doReturn(incompleteExtractedFile)
                    .when(FileUtils.class, "extractFile", Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertFalse(oadManager.init(FILE_PATH));

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
    }

    /**
     * Check that the SDK OAD internal state is set to {@link OADState#INIT}
     * if the binary file is found & complete.
     * Also check it returns a buffer of {@link OADManager#EXPECTED_NB_PACKETS_BINARY_FILE} elements
     * of {@link OADManager#EXPECTED_NB_PACKETS_BINARY_FILE} byte each.
     */
    @Test
    public void init_valid(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.INIT);
            }

            @Override
            public void onProgressPercentChanged(int progress) {
                assertEquals(progress, OADState.INIT.convertToProgress());
            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
            }
        });
        try {
            PowerMockito.doReturn(completeExtractedFile)
                    .when(FileUtils.class, "extractFile", Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(oadManager.init(FILE_PATH));
        assertEquals(oadManager.getCurrentState(), OADState.INIT);

    }

    /**
     * Check that the SDK aborts the OAD process,
     * raises the {@link BluetoothError#ERROR_NOT_CONNECTED} error
     * and the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void init_disconnection(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        PacketCounter packetEngine = Mockito.mock(PacketCounter.class);

        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {
                assertEquals(progress, OADState.INIT.convertToProgress());
            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        Mockito.doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
            bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
            return null;
        }).when(packetEngine).reset(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE);
        assertTrue(oadManager.init(FILE_PATH));
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#FIRMWARE_VALIDATION}
     * once the OAD validation request is sent
     */
    @Test
    public void requestFirmwareValidation_request(){
        oadManager.init(FILE_PATH);

        assertTrue(oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID));

        assertEquals(oadManager.getCurrentState(), OADState.FIRMWARE_VALIDATION);

    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_VALIDATION_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_FAILED}
     * for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void requestFirmwareValidation_failureResponse(){

        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState, OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {
                assertEquals(progress, OADState.INIT.convertToProgress());

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_VALIDATION_FAILED);
            }
        });

        ArgumentCaptor<DeviceEvents.OADValidationRequestEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADValidationRequestEvent.class);

        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        MbtEventBus.postEvent(new DeviceEvents.OADValidationResponseEvent(false));
                        return null;})
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FILE_PATH);

        oadManager.requestFirmwareValidation(BINARY_FILE_LENGTH_INVALID, FIRMWARE_VERSION_INVALID_LOWER);

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#TRANSFERRING}
     * if the headset device returns the code {@link command.DeviceCommandEvents#CMD_CODE_OTA_MODE_EVT_SUCCESS}
     * for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void requestFirmwareValidation_SuccessResponse(){

        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.TRANSFERRING);
            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
            }
        });

        ArgumentCaptor<DeviceEvents.OADValidationRequestEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADValidationRequestEvent.class);

        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        MbtEventBus.postEvent(new DeviceEvents.OADValidationResponseEvent(true));
                        return null;})
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FILE_PATH);

        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID);

        assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);

    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_TIMEOUT_UPDATE} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a {@link command.DeviceCommandEvents#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void requestFirmwareValidation_timeout(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_TIMEOUT_UPDATE);
            }
        });

        ArgumentCaptor<DeviceEvents.OADValidationRequestEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADValidationRequestEvent.class);

        try {
            PowerMockito
                    .doNothing()
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FILE_PATH);

        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID);

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the SDK aborts the OAD process,
     * raises the {@link BluetoothError#ERROR_NOT_CONNECTED} error,
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void requestFirmwareValidation_disconnection(){
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);

        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState,OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });

        ArgumentCaptor<DeviceEvents.OADValidationRequestEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADValidationRequestEvent.class);

        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
                        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FILE_PATH);

        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID);

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#AWAITING_DEVICE_READBACK}
     * if the SDK has performed the OAD packets transfer.
     */
    @Test
    public void transferOADFile_started(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState, OADState.AWAITING_DEVICE_READBACK);
            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
            }
        });

        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();

        assertEquals(oadManager.getCurrentState(), OADState.AWAITING_DEVICE_READBACK);

    }

    /**
     * Check that the SDK raises a {@link BluetoothError#} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the device fail to perform a write characteristic operation.
     */
    @Test
    public void transferOADFile_failureSending(){
        MbtBluetoothManager bluetoothManager = Mockito.mock(MbtBluetoothManager.class);

        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState, OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
            }
        });
        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        bluetoothManager.notifyResponseReceived(null, new DeviceCommands.ConnectAudio()); //todo create new mailbox command for OAD and replace connectaudio
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_TIMEOUT_UPDATE} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a write characteristic operation.
     */
    @Test
    public void transferOADFile_timeout(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {
                assertEquals(newState, OADState.ABORTED);
            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_TIMEOUT_UPDATE);

            }
        });
        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);

        try {
            PowerMockito
                    .doNothing()
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    } //todo not wait 60 minutes

    /**
     * Check that the SDK raises a {@link BluetoothError#} error
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
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);

        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
                        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();
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
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);

        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
                        bluetoothLE.notifyOADPacketLost(100);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();
        assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);
    }

    /**
     * Check that the SDK raises an Out Of Bounds Exception and abort the OAD process
     * if the headset device notifies that a packet
     * with index beyond the size of the SDK buffer has been lost during transfer
     * for a {@link command.DeviceCommandEvents#MBX_OTA_IDX_RESET_EVT} event.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void transferOADFile_lostPacket_invalidIndex(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, BluetoothError.ERROR_NOT_CONNECTED);
            }
        });
        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
                        bluetoothLE.notifyOADPacketLost(-1);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();
        assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#COMPLETE}
     * if all the OAD packets has been sent.
     */
    @Test
    public void transferOADFile_complete(){

        oadManager.init(FILE_PATH);

        oadManager.transferOADFile();

        assertEquals(oadManager.packetCounter.nbPacketSent, oadManager.packetCounter.nbPacketToSend);
        assertEquals(oadManager.getCurrentState(), OADState.COMPLETE);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#REBOOTING}
     * if the headset device returns a success readback response.
     */
    @Test
    public void transferOADFile_success(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        MbtEventBus.postEvent(new DeviceEvents.OADReadbackEvent(true));

        assertEquals(oadManager.getCurrentState(), OADState.REBOOTING);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns a failure readback response.
     */
    @Test
    public void transferOADFile_failureIncomplete(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        MbtEventBus.postEvent(new DeviceEvents.OADReadbackEvent(false));

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the {@link OADState#REBOOTING} state is triggered
     * and the headset failed to disconnect within the allocated time.
     */
    @Test
    public void reboot_timeout(){
        oadManager.setStateListener(new OADStateListener() {
            @Override
            public void onStateChanged(OADState newState) {

            }

            @Override
            public void onProgressPercentChanged(int progress) {

            }

            @Override
            public void onError(BaseError error, String additionalInfo) {
                assertEquals(error, OADError.ERROR_TIMEOUT_UPDATE);
            }
        });
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        MbtEventBus.postEvent(new DeviceEvents.OADReadbackEvent(true));
        try {
            new MbtAsyncWaitOperation().waitOperationResult(OADState.REBOOTING.getMaximumDuration());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        //reboot might be called when a disconnected state is triggered so here we don't need to do anything
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#RECONNECTING}
     * once the reboot has been performed.
     */
    @Test
    public void reconnect_started(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();

        oadManager.reboot();

        assertEquals(oadManager.getCurrentState(), OADState.RECONNECTING);
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#VERIFYING_FIRMWARE_VERSION}
     * once the headset device is reconnected.
     */
    @Test
    public void reconnect_success(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        oadManager.reboot();

        oadManager.reconnect();

        assertEquals(oadManager.getCurrentState(), OADState.VERIFYING_FIRMWARE_VERSION);
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device fails to reconnect within the allocated time.
     * Also check that the SDK raises an {@link BluetoothError#ERROR_CONNECTION_TIMEOUT} error
     */
    @Test
    public void reconnect_failure(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        oadManager.reboot();
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        ArgumentCaptor<StartOrContinueConnectionRequestEvent> captor = ArgumentCaptor.forClass(StartOrContinueConnectionRequestEvent.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_TIMEOUT);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.reconnect();

        assertEquals(oadManager.getCurrentState(), OADState.VERIFYING_FIRMWARE_VERSION);
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#COMPLETE}
     * if the SDK receives a firmware version equal to the last released version.
     */
    @Test
    public void verifyingFirmwareVersion_success(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        oadManager.reboot();
        OADManager oadManager = Mockito.mock(OADManager.class);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        ArgumentCaptor<StartOrContinueConnectionRequestEvent> captor = ArgumentCaptor.forClass(StartOrContinueConnectionRequestEvent.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.reconnect();
        try {
            PowerMockito
                    .doReturn(true)
                    .when(oadManager).isFirmwareVersionUpToDate(FIRMWARE_VERSION_VALID);

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.verifyFirmwareVersion();

        assertEquals(oadManager.getCurrentState(), OADState.COMPLETE);
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the SDK receives a firmware version not equal to the last released version.
     */
    @Test
    public void verifyingFirmwareVersion_failure(){
        oadManager.init(FILE_PATH);
        oadManager.transferOADFile();
        oadManager.reboot();
        OADManager oadManager = Mockito.mock(OADManager.class);
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        ArgumentCaptor<StartOrContinueConnectionRequestEvent> captor = ArgumentCaptor.forClass(StartOrContinueConnectionRequestEvent.class);
        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
                        return null;
                    })
                    .when(MbtEventBus.class, "postEvent", captor.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.reconnect();
        try {
            PowerMockito
                    .doReturn(false)
                    .when(oadManager).isFirmwareVersionUpToDate(FIRMWARE_VERSION_INVALID_LOWER);

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.verifyFirmwareVersion();

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }


}