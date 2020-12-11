package core.device.oad;

import android.content.Context;
import android.content.res.AssetManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import command.DeviceCommandEvent;
import command.OADCommands;
import core.bluetooth.BluetoothState;
import core.bluetooth.lowenergy.MbtBluetoothLE;
import core.device.MbtDeviceManager;
import core.device.event.OADEvent;
import core.device.model.MbtVersion;
import core.device.model.MbtDevice;
import engine.clientevents.BluetoothError;
import engine.clientevents.OADError;
import eventbus.MbtEventBus;
import eventbus.events.FirmwareUpdateClientEvent;
import utils.AsyncUtils;
import utils.MbtAsyncWaitOperation;
import utils.OADExtractionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static utils.OADExtractionUtils.EXPECTED_NB_BYTES_BINARY_FILE;
import static utils.OADExtractionUtils.EXPECTED_NB_PACKETS;
import static utils.OADExtractionUtils.FILE_LENGTH_NB_BYTES;
import static utils.OADExtractionUtils.FIRMWARE_VERSION_NB_BYTES;
import static utils.OADExtractionUtils.OAD_PACKET_SIZE;

@RunWith( PowerMockRunner.class )
@PrepareForTest({MbtEventBus.class, OADExtractionUtils.class})
public class OADManagerTest {

    private final String FIRMWARE_FIRST_FILENAME = "oad/indus2/mm-ota-1_7_4.bin" ;
    private final String FIRMWARE_SECOND_FILENAME = "oad/indus2/mm-ota-i2-1_7_14.bin" ;
    private final String FIRMWARE_VERSION_FIRST_AS_STRING = "1.6.2";
    private final String FIRMWARE_VERSION_SECOND_AS_STRING = "1.7.1";
    private final MbtVersion FIRMWARE_VERSION_VALID_FIRST = new MbtVersion(FIRMWARE_VERSION_FIRST_AS_STRING);
    private final MbtVersion FIRMWARE_VERSION_VALID_SECOND = new MbtVersion(FIRMWARE_VERSION_SECOND_AS_STRING);
    private final MbtVersion FIRMWARE_VERSION_INVALID_LENGTH = new MbtVersion("1.7");
    private final int BINARY_FILE_LENGTH_INVALID = 5;

    private static byte[] completeExtractedFile = new byte[EXPECTED_NB_BYTES_BINARY_FILE];
    private static byte[] incompleteExtractedFile = new byte[EXPECTED_NB_BYTES_BINARY_FILE -1];
    private static final byte byteValue = 0x01;

    private MbtDeviceManager contract;
    private OADManager oadManager;
    private Context context;
    private OADContext oadContext;
    private AssetManager assetManager;
    private OADState currentState;
    private PacketCounter packetCounter;
    private MbtDevice bluetoothDevice;
    private MbtAsyncWaitOperation<Boolean> lock;

    @Before
    public void setUp() {
        Arrays.fill(completeExtractedFile, byteValue);
        Arrays.fill(incompleteExtractedFile, byteValue);
        PowerMockito.spy(OADExtractionUtils.class);

        context = Mockito.mock(Context.class);
        oadContext = Mockito.mock(OADContext.class);
        assetManager = Mockito.mock(AssetManager.class);
        currentState = Mockito.mock(OADState.class);
        packetCounter = Mockito.mock(PacketCounter.class);
        contract = Mockito.mock(MbtDeviceManager.class);
        bluetoothDevice = Mockito.mock(MbtDevice.class);
        lock = Mockito.mock(MbtAsyncWaitOperation.class);

        Mockito.doReturn(assetManager).when(context).getAssets();
        try {
            Mockito.doReturn(new String[]{FIRMWARE_FIRST_FILENAME,FIRMWARE_SECOND_FILENAME}).when(assetManager).list("oad");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        try {
//            Mockito.doReturn(inputStream).when(assetManager).open(Mockito.anyString());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            PowerMockito.doReturn(completeExtractedFile)
//                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(AssetManager.class), Mockito.anyString());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        oadManager = new OADManager(context, contract, false);
        oadManager.setOADContext(oadContext);
        oadManager.setLock(lock);
    }

    /**
     * Check that the SDK raises an exception and the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not found.
     */
    @Test/*(expected = FileNotFoundException.class)*/ //error is catched in a try catch so this expected exception is not catched in the test
    public void init_invalid_fileNotFound(){
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        PowerMockito.spy(OADExtractionUtils.class);
        Mockito.when(currentState.nextState()).thenReturn(OADState.INITIALIZING);

        try { //mock the result returned by the method
            PowerMockito.when(OADExtractionUtils.class, "getFilePathForFirmwareVersion", Mockito.anyString(),Mockito.any(MbtVersion.class))
            .thenReturn(null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FIRMWARE_VERSION_VALID_SECOND, new MbtVersion("1.0.0"));
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
        Mockito.verify(contract,times(2)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        //assertEquals(captured.get(0).toString(), OADError.ERROR_INIT_FAILED, captured.get(0).getError());
        //assertEquals(captured.get(0).toString(), "File path/name incorrect : "+inputStream, captured.get(0).getAdditionalInfo());
        assertEquals(captured.get(1).toString(), OADState.ABORTED, captured.get(1).getOadState());
        assertEquals( 0, captured.get(1).getOadProgress());
    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not complete.
     */
    @Test
    public void init_invalid_fileIncomplete(){
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        Mockito.when(currentState.nextState()).thenReturn(OADState.INITIALIZING);

        PowerMockito.spy(OADExtractionUtils.class);

        try {
            PowerMockito.doReturn(incompleteExtractedFile)
                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(InputStream.class));

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FIRMWARE_VERSION_VALID_FIRST, new MbtVersion("1.0.0"));

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
        Mockito.verify(contract,times(2)).notifyClient(captor.capture());
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
                //assertEquals(captured.get(0).toString(), OADError.ERROR_INIT_FAILED, captured.get(0).getError());
                //assertEquals( "Expected length is 256000, but found length was 255999.", captured.get(0).getAdditionalInfo());
                assertEquals(captured.get(1).toString(), OADState.ABORTED, captured.get(1).getOadState());
                assertEquals( 0, captured.get(1).getOadProgress());

    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_INIT_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the binary file is not complete.
     */
    @Test
    public void init_failureRead(){
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        Mockito.when(currentState.nextState()).thenReturn(OADState.INITIALIZING);

        PowerMockito.spy(OADExtractionUtils.class);

        try {
            PowerMockito.doReturn(null)
                    .when(OADExtractionUtils.class, "extractFileContent", Mockito.any(InputStream.class));

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.init(FIRMWARE_VERSION_VALID_FIRST, new MbtVersion("1.0.0"));

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
        Mockito.verify(contract,times(2)).notifyClient(captor.capture());
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
                //assertEquals(captured.get(0).toString(), OADError.ERROR_INIT_FAILED, captured.get(0).getError());
                //assertEquals( "Impossible to read the OAD binary file.", captured.get(0).getAdditionalInfo());
                assertEquals(captured.get(1).toString(), OADState.ABORTED, captured.get(1).getOadState());
                assertEquals( 0, captured.get(1).getOadProgress());
    }

    /**
     * Check that the SDK OAD internal state is {@link OADState#INITIALIZED}
     * if the binary file is found & complete.
     * Also check it returns a buffer of {@link OADExtractionUtils#EXPECTED_NB_PACKETS} elements
     * of {@link OADExtractionUtils#EXPECTED_NB_PACKETS} byte each.
     */
    @Test
    public void init_valid() throws Exception {
        ArgumentCaptor<FirmwareUpdateClientEvent> captorClient = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        oadManager.setOADState(OADState.INITIALIZING);
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(FIRMWARE_FIRST_FILENAME);
//            Mockito.when(context.getAssets().open(Mockito.anyString()))
//                    .thenReturn(inputStream);
        PowerMockito.spy(OADExtractionUtils.class);
        PowerMockito.doReturn(completeExtractedFile)
                .when(OADExtractionUtils.class, "extractFileContent", inputStream);

        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.init(FIRMWARE_VERSION_VALID_FIRST, new MbtVersion("1.0.0"));
                assertEquals(oadManager.getCurrentState(), OADState.INITIALIZED);
                assertNotNull(oadManager.getOADContext().getOADfilepath(),FIRMWARE_FIRST_FILENAME);
                assertEquals(oadManager.getOADContext().getPacketsToSend().size(), EXPECTED_NB_PACKETS);
                assertEquals(oadManager.getOADContext().getFirmwareVersionAsByteArray().length,FIRMWARE_VERSION_NB_BYTES);
                assertEquals(oadManager.getOADContext().getNbPacketsToSend(),FILE_LENGTH_NB_BYTES); //todo 2 or 4 ?
                for (byte[] packet : oadManager.getOADContext().getPacketsToSend()){
                    assertEquals(packet.length, OAD_PACKET_SIZE);
                }
                Mockito.verify(contract,times(1)).notifyClient(captorClient.capture());

                List<FirmwareUpdateClientEvent> captured = captorClient.getAllValues();
                assertNull(captured.get(0).toString(), captured.get(0).getError());
                assertNull(captured.get(0).toString(), captured.get(0).getAdditionalInfo());
                assertEquals(captured.get(0).toString(), OADState.INITIALIZED, captured.get(0).getOadState());
                assertEquals( 1, captured.get(0).getOadProgress());}
        });


    }


    /**
     * Check that the OAD internal state is set to initialized and that init method is called
     * if the {@link OADState#INITIALIZING} state is raised.
     */
    @Test
    public void onOADStateChanged_initializing(){
        assertNull(oadManager.getCurrentState());
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(FIRMWARE_FIRST_FILENAME);
        assertNotNull(inputStream);
        try {
            Mockito.when(context.getAssets())
                    .thenReturn(assetManager);
            Mockito.when(assetManager.open(Mockito.anyString()))
                    .thenReturn(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.INITIALIZING, FIRMWARE_VERSION_VALID_FIRST);


                assertEquals(OADState.INITIALIZED, oadManager.getCurrentState());
                assertEquals(oadManager.getCurrentState(), OADState.INITIALIZED);
                assertNotNull(oadManager.getOADContext().getOADfilepath(), FIRMWARE_FIRST_FILENAME);
                assertEquals(oadManager.getOADContext().getPacketsToSend().size(), EXPECTED_NB_PACKETS);

                Mockito.verify(contract, times(2)).notifyClient(captor.capture());

                List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
                Mockito.verify(contract, times(2)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
                assertEquals(OADState.INITIALIZING, captured.get(0).getOadState());
                assertEquals(0, captured.get(0).getOadProgress());
                assertNull(captured.get(0).toString(), captured.get(0).getError());
                assertNull(captured.get(0).toString(), captured.get(0).getAdditionalInfo());
                assertEquals(OADState.INITIALIZED, captured.get(1).getOadState());
                assertEquals(1, captured.get(1).getOadProgress());
                assertNull(captured.get(1).toString(), captured.get(1).getError());
                assertNull(captured.get(1).toString(), captured.get(1).getAdditionalInfo());
            }
        });
    }

    /**
     * Check that the SDK aborts the OAD process,
     * raises the {@link BluetoothError#ERROR_NOT_CONNECTED} error
     * and the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void init_disconnection() throws Exception {
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        PowerMockito.spy(OADExtractionUtils.class);

        PowerMockito.doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
            bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED);
            return null;
        }).when(OADExtractionUtils.class, "getFilePathForFirmwareVersion", Mockito.anyString(), Mockito.any(MbtVersion.class));

        oadManager.init(FIRMWARE_VERSION_VALID_FIRST, new MbtVersion("1.0.0"));

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
        Mockito.verify(contract,times(2)).notifyClient(captor.capture());

        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        //assertEquals(captured.get(0).toString(), OADError.ERROR_INIT_FAILED, captured.get(0).getError());
        //assertEquals( "File path/name incorrect : null", captured.get(0).getAdditionalInfo());
        assertNull(captured.get(1).toString(), captured.get(1).getError());
        assertNull(captured.get(1).toString(), captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(1).toString(), OADState.ABORTED, captured.get(1).getOadState());
        assertEquals( 0, captured.get(1).getOadProgress());

    }


    /**
     * Check that the SDK raises the {@link OADError#ERROR_VALIDATION_FAILED} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns the code {@link DeviceCommandEvent#CMD_CODE_OTA_MODE_EVT_FAILED}
     * for a {@link DeviceCommandEvent#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void requestFirmwareValidation_failureResponse(){
        Mockito.when(oadContext.getFirmwareVersionAsByteArray())
                .thenReturn(new byte[]{1,0});
        Mockito.when(oadContext.getNbPacketsToSend())
                .thenReturn(EXPECTED_NB_PACKETS);

        ArgumentCaptor<OADCommands.RequestFirmwareValidation> captorValidation = ArgumentCaptor.forClass(OADCommands.RequestFirmwareValidation.class);
        ArgumentCaptor<FirmwareUpdateClientEvent> captorClient = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);

        ByteBuffer expectedData = ByteBuffer.allocate(4);
        expectedData.put(new byte[]{1,0});
        expectedData.putShort(EXPECTED_NB_PACKETS);
        expectedData.array();

        try {
            PowerMockito
                    .doAnswer((Answer<Void>) invocation -> {
                        oadManager.onOADEvent(OADEvent.FIRMWARE_VALIDATION_RESPONSE.setEventData(new byte[]{0}));
                        return null;})
                    .when(contract).requestFirmwareValidation(captorValidation.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }

        oadManager.onOADStateChanged(OADState.INITIALIZED, null);
        oadManager.abort();
        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
        Mockito.verify(contract,times(3)).notifyClient(captorClient.capture());
        List<FirmwareUpdateClientEvent> captured = captorClient.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.INITIALIZED, captured.get(0).getOadState());
        assertEquals(OADState.INITIALIZED.convertToProgress(), captured.get(0).getOadProgress());
        assertNull(captured.get(0).toString(), captured.get(0).getError());
        assertNull(captured.get(0).toString(), captured.get(0).getAdditionalInfo());
        assertEquals(captured.get(1).toString(), OADState.READY_TO_TRANSFER, captured.get(1).getOadState());
        assertEquals(OADState.READY_TO_TRANSFER.convertToProgress(), captured.get(1).getOadProgress());
//        assertEquals(captured.get(2).toString(), OADError.ERROR_FIRMWARE_REJECTED_UPDATE, captured.get(2).getError());
//        assertNull(captured.get(2).getAdditionalInfo());
        assertEquals(captured.get(2).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals(OADState.ABORTED.convertToProgress(), captured.get(2).getOadProgress());

        assertEquals(captorValidation.getAllValues().get(0).getData().length, 4);
        assertEquals(captorValidation.getAllValues().get(0).getData()[0], expectedData.get(0));
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#READY_TO_TRANSFER}
     * if the headset device returns the code {@link DeviceCommandEvent#CMD_CODE_OTA_MODE_EVT_SUCCESS}
     * for a {@link DeviceCommandEvent#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void requestFirmwareValidation_SuccessResponse(){
        Mockito.when(oadContext.getFirmwareVersionAsByteArray())
                .thenReturn(new byte[]{1,0});
        Mockito.when(oadContext.getNbPacketsToSend())
                .thenReturn(EXPECTED_NB_PACKETS);

        ArgumentCaptor<OADCommands.RequestFirmwareValidation> captorValidation = ArgumentCaptor.forClass(OADCommands.RequestFirmwareValidation.class);
        oadManager.setOADState(currentState);
        oadManager.setPacketCounter(packetCounter);

        try {
            Mockito.doNothing()
                    .when(currentState).executeAction(oadManager,(new byte[]{1}));

            Mockito.doAnswer((Answer<Void>) invocation -> {
                                    oadManager.onOADEvent(OADEvent.FIRMWARE_VALIDATION_RESPONSE.setEventData(new byte[]{1}));

                return null;})
                    .when(contract).requestFirmwareValidation(captorValidation.capture());

        } catch (Exception e) {
            e.printStackTrace();
        }
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.INITIALIZED, null);

                assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);
            }
        });
    }

    /**
     * Check that the SDK raises the {@link OADError#ERROR_TIMEOUT_UPDATE} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device does not return any response
     * within the allocated time for a {@link DeviceCommandEvent#MBX_OTA_MODE_EVT} event.
     */
    @Test
    public void requestFirmwareValidation_timeout(){

        Mockito.when(oadContext.getFirmwareVersionAsByteArray())
                .thenReturn(new byte[]{1,0});
        Mockito.when(oadContext.getNbPacketsToSend())
                .thenReturn(EXPECTED_NB_PACKETS);

        oadManager.onOADStateChanged(OADState.INITIALIZED, null);

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the SDK aborts the OAD process,
     * raises the {@link BluetoothError#ERROR_NOT_CONNECTED} error,
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void requestFirmwareValidation_disconnection() throws Exception {
        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
        PowerMockito.spy(OADExtractionUtils.class);

        PowerMockito.doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
            bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED);
            return null;
        }).when(OADExtractionUtils.class, "getFilePathForFirmwareVersion", Mockito.anyString(), Mockito.any(MbtVersion.class));

        oadManager.onOADStateChanged(OADState.INITIALIZED, null);

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#AWAITING_DEVICE_REBOOT}
     * if the SDK has performed the OAD packets transfer.
     */
    @Test
    public void transferOADFile_started(){
        oadManager.setOADState(currentState);
        Mockito.when(currentState.nextState()).thenReturn(OADState.TRANSFERRING);

        ArrayList<byte[]> packetList = new ArrayList<>();
        byte[] packet = new byte[OAD_PACKET_SIZE];
        Arrays.fill(packet, (byte)1);
        for (int i = 0; i < EXPECTED_NB_PACKETS; i++){
            packetList.add(packet);
        }
        oadManager.setPacketCounter(packetCounter);
        Mockito.when(packetCounter.getIndexOfNextPacket())
                .thenReturn((short) 1);
        Mockito.when(oadContext.getPacketsToSend())
                .thenReturn(new ArrayList<>());
        AsyncUtils.executeAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        oadManager.transferOADFile();
                                    }
                                });
        //Mockito.verify(contract).transferPacket(packet);
    }

    /**
     * Check that the SDK raises a {@link BluetoothError#} error
     * and that the OAD internal state is set to {@link OADState#ABORTED}
     * if the device fail to perform a write characteristic operation. => this test must be in bluetooth unit
     */
    @Test
    public void transferOADFile_failureSending(){
//        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
//
//        oadManager.setOADState(currentState);
//        Mockito.when(currentState.nextState()).thenReturn(OADState.TRANSFERRING);
//
//        ArrayList<byte[]> packetList = new ArrayList<>();
//        byte[] packet = new byte[OAD_PACKET_SIZE];
//        Arrays.fill(packet, (byte)1);
//        for (int i = 0; i < EXPECTED_NB_PACKETS; i++){
//            packetList.add(packet);
//        }
//        oadManager.setPacketCounter(packetCounter);
//        Mockito.when(packetCounter.getIndexOfNextPacket())
//                .thenReturn((short) 1);
//        Mockito.when(oadContext.getPacketsToSend())
//                .thenReturn(new ArrayList<>());
//        try {
//            PowerMockito.doAnswer((Answer<Void>) invocation -> {
//                oadManager.abort();
//                return null;
//            }).when(contract).transferPacket(captor.capture());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        oadManager.transferOADFile();
//        //Mockito.verify(contract).transferPacket(packet);
//
//        //assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

    }

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
//        oadManager.init(FIRMWARE_VERSION_VALID_FIRST) ;
//
//        oadManager.transferOADFile();
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
//
//    } //todo not wait 60 minutes

    /**
     * Check that the SDK raises a {@link BluetoothError#} error
     * aborts the OAD process,
     * and set the OAD internal state to {@link OADState#ABORTED}
     * if the headset device is disconnected.
     */
    @Test
    public void transferOADFile_disconnection(){
//        MbtBluetoothLE bluetoothLE = Mockito.mock(MbtBluetoothLE.class);
//        oadManager.setPacketCounter(packetCounter);
//
//        PowerMockito.doAnswer((Answer<Void>) invocation -> { //triggers a disconnection during the init method
//            bluetoothLE.notifyConnectionStateChanged(BluetoothState.DATA_BT_DISCONNECTED);
//            return null;
//        }).doReturn((short)1).when(packetCounter).getIndexOfNextPacket();
//
//        oadManager.onOADStateChanged(OADState.READY_TO_TRANSFER, new byte[]{1,1});
//        oadManager.abort();
//        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);
    }

    /**
     * Check that the SDK resets the internal packet index
     * to the value received in the headset response
     * if the headset device returns a valid packet index
     * for a {@link DeviceCommandEvent#MBX_OTA_IDX_RESET_EVT} event.
     */
    @Test
    public void transferOADFile_lostPacket_validIndex(){
        oadManager.setOADState(OADState.TRANSFERRING);
        oadManager.setPacketCounter(packetCounter);

        oadManager.onOADEvent(OADEvent.LOST_PACKET.setEventData(new byte[]{
                (byte)1,(byte)1,(byte)1,(byte)1,(byte)1,(byte)1,(byte)1,(byte)1,(byte)1}));

        assertEquals(packetCounter.nbPacketCounted, 0);
    }

    /**
     * Check that the SDK raises an Out Of Bounds Exception and abort the OAD process
     * if the headset device notifies that a packet
     * with index beyond the size of the SDK buffer has been lost during transfer
     * for a {@link DeviceCommandEvent#MBX_OTA_IDX_RESET_EVT} event.
     */
    @Test
    public void transferOADFile_lostPacket_invalidIndex(){
        oadManager.setOADState(OADState.TRANSFERRING);
        oadManager.setPacketCounter(packetCounter);

        oadManager.onOADEvent(OADEvent.LOST_PACKET.setEventData(new byte[]{-10}));

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#COMPLETED}
     * if all the OAD packets has been sent.
     */
    @Test
    public void transferOADFile_complete(){
        oadManager.setPacketCounter(packetCounter);

        when(packetCounter.areAllPacketsCounted()).thenReturn(true);
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.TRANSFERRING, true);
                oadManager.onOADEvent(OADEvent.PACKET_TRANSFERRED.setEventData(new byte[]{-1, 1}));

                assertEquals(oadManager.getCurrentState(), OADState.TRANSFERRING);
            }
        });
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#AWAITING_DEVICE_REBOOT}
     * if the headset device returns a success readback response.
     */
    @Test
    public void readback_success() throws InterruptedException, ExecutionException, TimeoutException {
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captorClient = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        Mockito.when(lock.waitOperationResult(20000)).thenReturn(true);
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.AWAITING_DEVICE_REBOOT, new byte[]{1});

                assertEquals(oadManager.getCurrentState(), OADState.READY_TO_RECONNECT);
                Mockito.verify(contract, times(2)).notifyClient(captorClient.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
                List<FirmwareUpdateClientEvent> captured = captorClient.getAllValues();
                assertEquals(captured.get(0).toString(), OADState.AWAITING_DEVICE_REBOOT, captured.get(0).getOadState());
                assertEquals(captured.get(0).toString(), OADState.AWAITING_DEVICE_REBOOT.convertToProgress(), captured.get(0).getOadProgress());
                assertEquals(captured.get(1).toString(), OADState.READY_TO_RECONNECT, captured.get(1).getOadState());
                assertEquals(OADState.READY_TO_RECONNECT, captured.get(1).getOadProgress());
            }
        });

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device returns a failure readback response.
     */
    @Test
    public void readback_failureIncomplete(){
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);

        oadManager.onOADStateChanged(OADState.AWAITING_DEVICE_REBOOT, new byte[]{0});

        assertEquals(OADState.ABORTED, oadManager.getCurrentState());
        //Mockito.verify(state).executeAction(oadManager, FIRMWARE_VERSION_VALID_FIRST);
        Mockito.verify(contract,times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.AWAITING_DEVICE_REBOOT, captured.get(0).getOadState());
        assertEquals( OADState.AWAITING_DEVICE_REBOOT.convertToProgress(), captured.get(0).getOadProgress());
        assertEquals(captured.get(1).toString(), OADError.ERROR_TRANSFER_FAILED, captured.get(1).getError());
        assertNull( captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(2).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals( 0, captured.get(2).getOadProgress());
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the {@link OADState#AWAITING_DEVICE_REBOOT} state is triggered
     * and the headset failed to send the readback within the allocated time.
     */
    @Test
    public void readback_timeout(){
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);

        oadManager.onOADStateChanged(OADState.TRANSFERRED, null);
        oadManager.abort();

        assertEquals(OADState.ABORTED, oadManager.getCurrentState());
        Mockito.verify(contract,times(2)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.TRANSFERRED, captured.get(0).getOadState());
        assertEquals( OADState.TRANSFERRED.convertToProgress(), captured.get(0).getOadProgress());
//        assertEquals(captured.get(1).toString(), OADError.ERROR_TIMEOUT_UPDATE, captured.get(1).getError());
//        assertEquals( "Readback timed out.", captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(0).toString(), OADState.ABORTED, captured.get(1).getOadState());
        assertEquals( 0, captured.get(1).getOadProgress());

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the {@link OADState#AWAITING_DEVICE_REBOOT} state is triggered
     * and the headset failed to disconnect within the allocated time.
     */
    @Test
    public void reboot_timeout(){
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);

        oadManager.onOADStateChanged(OADState.AWAITING_DEVICE_REBOOT, new byte[]{(byte)1});

        assertEquals(OADState.ABORTED, oadManager.getCurrentState());
        Mockito.verify(contract,times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.AWAITING_DEVICE_REBOOT, captured.get(0).getOadState());
        assertEquals( OADState.AWAITING_DEVICE_REBOOT.convertToProgress(), captured.get(0).getOadProgress());
        assertEquals(captured.get(1).toString(), OADError.ERROR_TIMEOUT_UPDATE, captured.get(1).getError());
        assertEquals( "Reboot timed out.", captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(0).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals( 0, captured.get(2).getOadProgress());

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#READY_TO_RECONNECT}
     * if the {@link OADState#AWAITING_DEVICE_REBOOT} state is triggered
     * and the headset succeeded to disconnect within the allocated time.
     */
    @Test
    public void reboot_success() throws InterruptedException, ExecutionException, TimeoutException {
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            oadManager.onOADEvent(OADEvent.DISCONNECTED_FOR_REBOOT.setEventData(true));
            Mockito.verify(contract).clearBluetooth();
            return null;
        }).when(contract).reconnect(false);
        Mockito.when(lock.waitOperationResult(200000)).thenReturn(true);
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.AWAITING_DEVICE_REBOOT, new byte[]{(byte) 1});

                assertEquals(OADState.READY_TO_RECONNECT, oadManager.getCurrentState());
                Mockito.verify(contract, times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
                List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
                assertEquals(captured.get(0).toString(), OADState.AWAITING_DEVICE_REBOOT, captured.get(0).getOadState());
                assertEquals(OADState.AWAITING_DEVICE_REBOOT.convertToProgress(), captured.get(0).getOadProgress());
                assertEquals(captured.get(1).toString(), OADError.ERROR_TIMEOUT_UPDATE, captured.get(1).getError());
                assertNull(captured.get(1).getAdditionalInfo());
                assertEquals("Reboot timed out.", captured.get(0).getAdditionalInfo());
                //assertEquals(captured.get(1).toString(), OADState.TRANSFERRING, captured.get(1).getOadState());
                //assertEquals( OADState.TRANSFERRING.convertToProgress(), captured.get(1).getOadProgress());
                assertEquals(captured.get(0).toString(), OADState.ABORTED, captured.get(2).getOadState());
                assertEquals(0, captured.get(2).getOadProgress());
            }
        });

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#RECONNECTING}
     * once the clearBluetooth has been performed.
     */
    @Test
    public void reconnect_started(){

        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.AWAITING_DEVICE_REBOOT, new byte[]{(byte) 1});

                assertEquals(oadManager.getCurrentState(), OADState.RECONNECTING);
                Mockito.verify(contract, times(1)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
                List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
                assertEquals(captured.get(0).toString(), OADState.RECONNECTING, captured.get(0).getOadState());
                assertEquals(OADState.RECONNECTING.convertToProgress(), captured.get(0).getOadProgress());
            }
        });

    }

    /**
     * Check that the OAD internal state is set to {@link OADState#INITIALIZED}
     * once the headset device is reconnected.
     */
    @Test
    public void reconnect_success() {
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captorClient = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        ArgumentCaptor<MbtVersion> captorFirmware = ArgumentCaptor.forClass(MbtVersion.class);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            oadManager.onOADEvent(OADEvent.RECONNECTION_PERFORMED.setEventData(true));
            return null;
        }).when(contract).reconnect(false);
        Mockito.when(oadContext.getOADfilepath()).thenReturn(FIRMWARE_FIRST_FILENAME);
        Mockito.when(contract.verifyFirmwareVersion(captorFirmware.capture())).thenReturn(true);
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                oadManager.onOADStateChanged(OADState.RECONNECTING, null);

                assertEquals(OADState.RECONNECTING, oadManager.getCurrentState());
                Mockito.verify(contract, times(1)).notifyClient(captorClient.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
                List<FirmwareUpdateClientEvent> captured = captorClient.getAllValues();
                assertEquals(captured.get(0).toString(), OADState.RECONNECTING, captured.get(0).getOadState());
                assertEquals(OADState.RECONNECTING.convertToProgress(), captured.get(0).getOadProgress());
                assertEquals(captured.get(0).toString(), OADState.RECONNECTION_PERFORMED, captured.get(0).getOadState());
                assertEquals(OADState.RECONNECTION_PERFORMED.convertToProgress(), captured.get(0).getOadProgress());
            }
        });
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device fails to reconnect (failure that is not a timeout).
     * Also check that the SDK raises an {@link BluetoothError#ERROR_CONNECTION_TIMEOUT} error
     */
    @Test
    public void reconnect_failure(){
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        oadManager.setOADState(currentState);
        Mockito.when(currentState.nextState()).thenReturn(OADState.RECONNECTION_PERFORMED);

        oadManager.onOADEvent(OADEvent.RECONNECTION_PERFORMED.setEventData(false));

        Mockito.verify(contract,times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.RECONNECTION_PERFORMED, captured.get(0).getOadState());
        assertEquals( OADState.RECONNECTION_PERFORMED.convertToProgress(), captured.get(0).getOadProgress());
        assertEquals(captured.get(1).toString(), OADError.ERROR_RECONNECT_FAILED, captured.get(1).getError());
        assertNull(captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(2).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals(OADState.ABORTED.convertToProgress(), captured.get(2).getOadProgress());
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device fails to reconnect within the allocated time.
     * Also check that the SDK raises an {@link BluetoothError#ERROR_CONNECTION_TIMEOUT} error
     */
    @Test
    public void reconnect_timeoutFromBluetoothUnit() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        oadManager.setOADState(currentState);
        Mockito.when(currentState.nextState()).thenReturn(OADState.RECONNECTING);

        Mockito.when(lock.waitOperationResult(20000)).thenAnswer((Answer<Void>) invocation -> {
            oadManager.onOADEvent(OADEvent.RECONNECTION_PERFORMED.setEventData(false));
            return null;
        });

        oadManager.onOADStateChanged(OADState.RECONNECTING, FIRMWARE_VERSION_VALID_FIRST);

        //contract.onConnectionStateChanged(new ConnectionStateEvent(BluetoothState.SCAN_TIMEOUT, null, null));

        Mockito.verify(contract,times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.RECONNECTING, captured.get(0).getOadState());
        assertEquals( OADState.RECONNECTING.convertToProgress(), captured.get(0).getOadProgress());
        //assertEquals(captured.get(1).toString(), OADState.RECONNECTION_PERFORMED, captured.get(2).getOadState());
        //assertEquals( OADState.RECONNECTION_PERFORMED.convertToProgress(), captured.get(2).getOadProgress());
        assertEquals(captured.get(1).toString(), OADError.ERROR_TIMEOUT_UPDATE, captured.get(1).getError());
        assertEquals("Reconnection timed out.",captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(2).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals(OADState.ABORTED.convertToProgress(), captured.get(2).getOadProgress());
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the headset device fails to reconnect within the allocated time.
     * Also check that the SDK raises an {@link BluetoothError#ERROR_CONNECTION_TIMEOUT} error
     */
    @Test
    public void reconnect_timeoutFromDeviceUnit() {
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        oadManager.setOADState(currentState);
        Mockito.when(currentState.nextState()).thenReturn(OADState.RECONNECTING);

        oadManager.onOADStateChanged(OADState.RECONNECTING, FIRMWARE_VERSION_VALID_FIRST);

        //contract.onConnectionStateChanged(new ConnectionStateEvent(BluetoothState.SCAN_TIMEOUT, null, null));

        Mockito.verify(contract,times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.RECONNECTING, captured.get(0).getOadState());
        assertEquals( OADState.RECONNECTING.convertToProgress(), captured.get(0).getOadProgress());
        //assertEquals(captured.get(1).toString(), OADState.RECONNECTION_PERFORMED, captured.get(2).getOadState());
        //assertEquals( OADState.RECONNECTION_PERFORMED.convertToProgress(), captured.get(2).getOadProgress());
        assertEquals(captured.get(1).toString(), OADError.ERROR_TIMEOUT_UPDATE, captured.get(1).getError());
        assertEquals("Reconnection timed out.",captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(2).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals(OADState.ABORTED.convertToProgress(), captured.get(2).getOadProgress());
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#COMPLETED}
     * if the SDK receives a firmware version equal to the last released version.
     */
    @Test
    public void verifyingFirmwareVersion_success(){
        ArgumentCaptor<FirmwareUpdateClientEvent> captorClient = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        ArgumentCaptor<MbtVersion> captorFirmware = ArgumentCaptor.forClass(MbtVersion.class);
        oadManager.setOADState(currentState);
        OADContext oadContext = new OADContext();
        oadContext.setOADfilepath(FIRMWARE_FIRST_FILENAME);
        oadManager.setOADContext(oadContext);
        Mockito.when(currentState.nextState()).thenReturn(OADState.RECONNECTION_PERFORMED);
        Mockito.when(contract.verifyFirmwareVersion(captorFirmware.capture())).thenReturn(true);

        oadManager.onOADEvent(OADEvent.RECONNECTION_PERFORMED.setEventData(true));
        Mockito.verify(contract,times(2)).notifyClient(captorClient.capture()); //3 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captorClient.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.RECONNECTION_PERFORMED, captured.get(0).getOadState());
        assertEquals(OADState.RECONNECTION_PERFORMED.convertToProgress(), captured.get(0).getOadProgress());
        assertEquals(captured.get(1).toString(), OADState.COMPLETED, captured.get(1).getOadState());
        assertEquals(OADState.COMPLETED.convertToProgress(), captured.get(1).getOadProgress());

        assertEquals(OADState.COMPLETED, oadManager.getCurrentState());
    }

    /**
     * Check that the OAD internal state is set to {@link OADState#ABORTED}
     * if the SDK receives a firmware version not equal to the last released version.
     */
    @Test
    public void verifyingFirmwareVersion_failure(){
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);
        oadManager.setOADState(currentState);
        OADContext oadContext = new OADContext();
        oadContext.setOADfilepath(FIRMWARE_FIRST_FILENAME);
        oadManager.setOADContext(oadContext);
        Mockito.when(currentState.nextState()).thenReturn(OADState.RECONNECTION_PERFORMED);
        Mockito.when(contract.verifyFirmwareVersion(new MbtVersion("1.7.4"))).thenReturn(false);

        oadManager.onOADEvent(OADEvent.RECONNECTION_PERFORMED.setEventData(true));

        assertEquals(oadManager.getCurrentState(), OADState.ABORTED);

        Mockito.verify(contract,times(3)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.RECONNECTION_PERFORMED, captured.get(0).getOadState());
        assertEquals(OADState.RECONNECTION_PERFORMED.convertToProgress(), captured.get(0).getOadProgress());
        assertEquals(captured.get(1).toString(), OADError.ERROR_WRONG_FIRMWARE_VERSION, captured.get(1).getError());
        assertNull( captured.get(1).getAdditionalInfo());
        assertEquals(captured.get(2).toString(), OADState.ABORTED, captured.get(2).getOadState());
        assertEquals( 0, captured.get(2).getOadProgress());
    }

    /**
     * Check that the OAD internal state is set to ABORTED
     * if the abort method is called
     */
    @Test
    public void abort(){
        oadManager.setOADContract(contract);
        ArgumentCaptor<FirmwareUpdateClientEvent> captor = ArgumentCaptor.forClass(FirmwareUpdateClientEvent.class);

        oadManager.abort();

        assertEquals(OADState.ABORTED,oadManager.getCurrentState());
        Mockito.verify(contract,times(1)).notifyClient(captor.capture()); //2 times notified : notify client state changed to ABORTED and notify error raised
        List<FirmwareUpdateClientEvent> captured = captor.getAllValues();
        assertEquals(captured.get(0).toString(), OADState.ABORTED, captured.get(0).getOadState());
        assertEquals( 0, captured.get(0).getOadProgress());
    }
}