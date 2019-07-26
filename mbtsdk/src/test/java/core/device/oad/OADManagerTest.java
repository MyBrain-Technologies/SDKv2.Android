package core.device.oad;

import android.content.Context;
import android.content.res.AssetManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import core.bluetooth.BtState;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.device.MbtDeviceManager;
import core.device.model.FirmwareVersion;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import eventbus.MbtEventBus;
import utils.OADExtractionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static utils.OADExtractionUtils.EXPECTED_NB_BYTES_BINARY_FILE;
import static utils.OADExtractionUtils.EXPECTED_NB_PACKETS_BINARY_FILE;
import static utils.OADExtractionUtils.OAD_PACKET_SIZE;

@RunWith( PowerMockRunner.class )
@PrepareForTest({MbtEventBus.class, OADExtractionUtils.class})
public class OADManagerTest {

    private final FirmwareVersion FIRMWARE_VERSION_VALID = new FirmwareVersion("1.7.1");
    private final FirmwareVersion FIRMWARE_VERSION_INVALID_LENGTH = new FirmwareVersion("1.7");
    private final int BINARY_FILE_LENGTH_INVALID = 5;

    private static byte[] completeExtractedFile = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
    private static byte[] incompleteExtractedFile = new byte[EXPECTED_NB_BYTES_BINARY_FILE -1];
    private static final byte byteValue = 0x01;

    private OADManager oadManager;

    @Before
    public void setUp() {
        Arrays.fill(completeExtractedFile, byteValue);
        Arrays.fill(incompleteExtractedFile, byteValue);
        PowerMockito.spy(OADExtractionUtils.class);
        Context context = Mockito.mock(Context.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        AssetManager assetManager = Mockito.mock(AssetManager.class);
        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{"mm-ota-1_6_2.bin","mm-ota-1_7_1.bin"}).when(assetManager).list("oad");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Mockito.doReturn(inputStream).when(assetManager).open(Mockito.anyString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            PowerMockito.doReturn(completeExtractedFile)
                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(AssetManager.class), Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager = new OADManager(context, Mockito.mock(MbtDeviceManager.class), FIRMWARE_VERSION_VALID);
        assertEquals(OADState.INITIALIZING, oadManager.getCurrentState());
    }

    /**
     * Check that the SDK raises an exception and the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not found.
     */
    @Test(expected = FileNotFoundException.class)
    public void init_invalid_fileNotFound(){
        PowerMockito.spy(OADExtractionUtils.class);

        try { //mock the result returned by the extractFile method
            PowerMockito.doThrow(new FileNotFoundException("File path/name incorrect "))
                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(AssetManager.class), Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FIRMWARE_VERSION_VALID);
    }

    
    /**
     * Check that the OAD internal state is set to initializing and that init method is called 
     * if the {@link OADState#INITIALIZING} state is raised.
     */
    @Test
    public void onOADStateChanged_initializing(){
        OADState state = Mockito.mock(OADState.class);
        assertEquals(OADState.INITIALIZING, oadManager.getCurrentState());

        oadManager.onOADStateChanged(OADState.INITIALIZING, FIRMWARE_VERSION_VALID);

        assertEquals(OADState.INITIALIZED, oadManager.getCurrentState());
        Mockito.verify(state).executeAction(oadManager, FIRMWARE_VERSION_VALID);

    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not complete.
     */
    @Test
    public void init_invalid_fileIncomplete(){

        PowerMockito.spy(OADExtractionUtils.class);

        try {
            PowerMockito.doReturn(incompleteExtractedFile)
                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(AssetManager.class), Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FIRMWARE_VERSION_VALID);

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
    }

    /**
     * Check that the SDK OAD internal state is {@link OADState#INITIALIZED}
     * if the binary file is found & complete.
     * Also check it returns a buffer of {@link OADExtractionUtils#EXPECTED_NB_PACKETS_BINARY_FILE} elements
     * of {@link OADExtractionUtils#EXPECTED_NB_PACKETS_BINARY_FILE} byte each.
     */
    @Test
    public void init_valid(){

        try {
            PowerMockito.doReturn(completeExtractedFile)
                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(AssetManager.class), Mockito.anyString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        oadManager.init(FIRMWARE_VERSION_VALID);
        assertEquals(oadManager.getCurrentState(), OADState.INITIALIZED);
        assertEquals(oadManager.getPacketCounter().totalNbPackets, EXPECTED_NB_PACKETS_BINARY_FILE);
    }

    /**
     * Check that the SDK aborts the OAD process,
     * raises the {@link BluetoothError#ERROR_NOT_CONNECTED} error
     * and the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void init_disconnection(){
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        PacketCounter packetEngine = Mockito.mock(PacketCounter.class);
//
//        Mockito.doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
//            bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
//            return null;
//        }).when(packetEngine).reset(EXPECTED_NB_PACKETS_BINARY_FILE, OAD_PACKET_SIZE);
//        oadManager.init(FIRMWARE_VERSION_VALID);
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

//
//    /**
//     * Check that the SDK raises the {@link OADError#ERROR_VALIDATION_FAILED} error
//     * and that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the headset device returns the code {@link DeviceCommandEvent#CMD_CODE_OTA_MODE_EVT_FAILED}
//     * for a {@link DeviceCommandEvent#MBX_OTA_MODE_EVT} event.
//     */
//    @Test
//    public void requestFirmwareValidation_failureResponse(){
//
//
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> {
//                        return null;})
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_INVALID_LOWER);
//
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#READY_TO_TRANSFER}
//     * if the headset device returns the code {@link DeviceCommandEvent#CMD_CODE_OTA_MODE_EVT_SUCCESS}
//     * for a {@link DeviceCommandEvent#MBX_OTA_MODE_EVT} event.
//     */
//    @Test
//    public void requestFirmwareValidation_SuccessResponse(){
//
//
//
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> {
//                        return null;})
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID);
//
//        assertEquals(oadManager.getCurrentState(), OADState.INITIALIZED);
//
//    }
//
//    /**
//     * Check that the SDK raises the {@link OADError#ERROR_TIMEOUT_UPDATE} error
//     * and that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the headset device does not return any response
//     * within the allocated time for a {@link DeviceCommandEvent#MBX_OTA_MODE_EVT} event.
//     */
//    @Test
//    public void requestFirmwareValidation_timeout(){
//
//        ArgumentCaptor<DeviceEvents.OADValidationRequestEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADValidationRequestEvent.class);
//
//        try {
//            PowerMockito
//                    .doNothing()
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID);
//
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the SDK aborts the OAD process,
//     * raises the {@link BluetoothError#ERROR_NOT_CONNECTED} error,
//     * and that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the headset device is disconnected.
//     */
//    @Test
//    public void requestFirmwareValidation_disconnection(){
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//
//
//        ArgumentCaptor<DeviceEvents.OADValidationRequestEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADValidationRequestEvent.class);
//
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
//                        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.requestFirmwareValidation(OADManager.EXPECTED_NB_PACKETS_BINARY_FILE, FIRMWARE_VERSION_VALID);
//
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#AWAITING_DEVICE_REBOOT}
//     * if the SDK has performed the OAD packets transfer.
//     */
//    @Test
//    public void transferOADFile_started(){
//
//
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.transferOADFile();
//
//        assertEquals(oadManager.getCurrentState(), OADState.AWAITING_DEVICE_REBOOT);
//
//    }
//
//    /**
//     * Check that the SDK raises a {@link BluetoothError#} error
//     * and that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the device fail to perform a write characteristic operation.
//     */
//    @Test
//    public void transferOADFile_failureSending(){
//        MbtBluetoothManager bluetoothManager = Mockito.mock(MbtBluetoothManager.class);
//
//        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> {
//                        bluetoothManager.notifyResponseReceived(null, new DeviceCommands.ConnectAudio()); //todo create new mailbox command for OAD and replace connectaudio
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.transferOADFile();
//
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the SDK raises the {@link OADError#ERROR_TIMEOUT_UPDATE} error
//     * and that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the headset device does not return any response
//     * within the allocated time for a write characteristic operation.
//     */
//    @Test
//    public void transferOADFile_timeout(){
//
//        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
//
//        try {
//            PowerMockito
//                    .doNothing()
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.transferOADFile();
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    } //todo not wait 60 minutes
//
//    /**
//     * Check that the SDK raises a {@link BluetoothError#} error
//     * aborts the OAD process,
//     * and set the OAD internal state to {@link OADState#ABORTED}
//     * if the headset device is disconnected.
//     */
//    @Test
//    public void transferOADFile_disconnection(){
//
//        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
//
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
//                        bluetoothLE.notifyConnectionStateChanged(BtState.DATA_BT_DISCONNECTED);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.transferOADFile();
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the SDK resets the internal packet index
//     * to the value received in the headset response
//     * if the headset device returns a valid packet index
//     * for a {@link DeviceCommandEvent#MBX_OTA_IDX_RESET_EVT} event.
//     */
//    @Test
//    public void transferOADFile_lostPacket_validIndex(){
//
//        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
//
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
//                        bluetoothLE.notifyOADPacketLost(100);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.init(FIRMWARE_VERSION_VALID);
//
//        oadManager.transferOADFile();
//        assertEquals(oadManager.getCurrentState(), OADState.READY_TO_TRANSFER);
//    }
//
//    /**
//     * Check that the SDK raises an Out Of Bounds Exception and abort the OAD process
//     * if the headset device notifies that a packet
//     * with index beyond the size of the SDK buffer has been lost during transfer
//     * for a {@link DeviceCommandEvent#MBX_OTA_IDX_RESET_EVT} event.
//     */
//    @Test(expected = IndexOutOfBoundsException.class)
//    public void transferOADFile_lostPacket_invalidIndex(){
//
//        ArgumentCaptor<DeviceEvents.OADTransferEvent> captor = ArgumentCaptor.forClass(DeviceEvents.OADTransferEvent.class);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
//                        bluetoothLE.notifyOADPacketLost(-1);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.transferOADFile();
//        assertEquals(oadManager.getCurrentState(), OADState.AWAITING_DEVICE_REBOOT);
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#COMPLETED}
//     * if all the OAD packets has been sent.
//     */
//    @Test
//    public void transferOADFile_complete(){
//
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//
//        oadManager.transferOADFile();
//
//        assertEquals(oadManager.packetCounter.nbPacketCounted, oadManager.packetCounter.totalNbPackets);
//        assertEquals(oadManager.getCurrentState(), OADState.COMPLETED);
//
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#AWAITING_DEVICE_REBOOT}
//     * if the headset device returns a success readback response.
//     */
//    @Test
//    public void transferOADFile_success(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        MbtEventBus.postEvent(new DeviceEvents.OADReadbackEvent(true));
//
//        assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRED);
//
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the headset device returns a failure readback response.
//     */
//    @Test
//    public void transferOADFile_failureIncomplete(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        MbtEventBus.postEvent(new DeviceEvents.OADReadbackEvent(false));
//
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the {@link OADState#READY_TO_RECONNECT} state is triggered
//     * and the headset failed to disconnect within the allocated time.
//     */
//    @Test
//    public void reboot_timeout(){
//
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        MbtEventBus.postEvent(new DeviceEvents.OADReadbackEvent(true));
//        try {
//            new MbtAsyncWaitOperation().waitOperationResult(OADState.REBOOTING.getMaximumDuration());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (TimeoutException e) {
//            e.printStackTrace();
//        }
//        //clearBluetooth might be called when a disconnected state is triggered so here we don't need to do anything
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#RECONNECTING}
//     * once the clearBluetooth has been performed.
//     */
//    @Test
//    public void reconnect_started(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//
//        oadManager.resetCacheAndKeys();
//
//        assertEquals(oadManager.getCurrentState(), OADState.RECONNECTING);
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#INITIALIZED}
//     * once the headset device is reconnected.
//     */
//    @Test
//    public void reconnect_success(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        oadManager.resetCacheAndKeys();
//
//        oadManager.reconnect();
//
//        assertEquals(oadManager.getCurrentState(), OADState.INITIALIZED);
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the headset device fails to reconnect within the allocated time.
//     * Also check that the SDK raises an {@link BluetoothError#ERROR_CONNECTION_TIMEOUT} error
//     */
//    @Test
//    public void reconnect_failure(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        oadManager.resetCacheAndKeys();
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        ArgumentCaptor<StartOrContinueConnectionRequestEvent> captor = ArgumentCaptor.forClass(StartOrContinueConnectionRequestEvent.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> {
//                        bluetoothLE.notifyConnectionStateChanged(BtState.SCAN_TIMEOUT);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.reconnect();
//
//        assertEquals(oadManager.getCurrentState(), OADState.VERIFYING_FIRMWARE_VERSION);
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#COMPLETED}
//     * if the SDK receives a firmware version equal to the last released version.
//     */
//    @Test
//    public void verifyingFirmwareVersion_success(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        oadManager.resetCacheAndKeys();
//        OADManager oadManager = Mockito.mock(OADManager.class);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        ArgumentCaptor<StartOrContinueConnectionRequestEvent> captor = ArgumentCaptor.forClass(StartOrContinueConnectionRequestEvent.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> {
//                        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.reconnect();
//        try {
//            PowerMockito
//                    .doReturn(true)
//                    .when(oadManager).isFirmwareVersionUpToDate(FIRMWARE_VERSION_VALID);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.verifyFirmwareVersion();
//
//        assertEquals(oadManager.getCurrentState(), OADState.COMPLETED);
//    }
//
//    /**
//     * Check that the OAD internal state is set to {@link OADState#ABORTED}
//     * if the SDK receives a firmware version not equal to the last released version.
//     */
//    @Test
//    public void verifyingFirmwareVersion_failure(){
//        oadManager.init(FIRMWARE_VERSION_VALID) ;
//        oadManager.transferOADFile();
//        oadManager.resetCacheAndKeys();
//        OADManager oadManager = Mockito.mock(OADManager.class);
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        ArgumentCaptor<StartOrContinueConnectionRequestEvent> captor = ArgumentCaptor.forClass(StartOrContinueConnectionRequestEvent.class);
//        try {
//            PowerMockito
//                    .doAnswer((Answer<Void>) invocation -> {
//                        bluetoothLE.notifyConnectionStateChanged(BtState.CONNECTED_AND_READY);
//                        return null;
//                    })
//                    .when(MbtEventBus.class, "postEvent", captor.capture());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.reconnect();
//        try {
//            PowerMockito
//                    .doReturn(false)
//                    .when(oadManager).isFirmwareVersionUpToDate(FIRMWARE_VERSION_INVALID_LOWER);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.verifyFirmwareVersion();
//
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    }


    /**
     * Check that the OAD internal state is reset
     * if the {@link OADState#ABORTED} state is raised.
     */
    @Test
    public void abort(){
        OADState state = Mockito.mock(OADState.class);

        oadManager.abort();

        assertEquals(OADState.ABORTED,oadManager.getCurrentState());
        Mockito.verify(state).executeAction(oadManager, null);
    }
}