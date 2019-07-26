package core.device.oad;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import core.device.event.OADEvent;
import core.device.model.FirmwareVersion;
import engine.clientevents.BaseError;
import engine.clientevents.OADError;
import eventbus.events.FirmwareUpdateClientEvent;
import utils.LogUtils;
import utils.MbtAsyncWaitOperation;
import utils.OADExtractionUtils;

import static utils.OADExtractionUtils.EXPECTED_NB_BYTES_BINARY_FILE;
import static utils.OADExtractionUtils.OAD_PACKET_SIZE;

/**
 * OAD Manager class is responsible for managing the OAD (Over the Air Download) update process.
 * This process contains several steps that are handled one after another (no parallel tasks).
 * Each of these steps are represented with a state machine so that we can know which step is the current step at any moment.
 * This process uses Bluetooth to transfer a binary file that contains a firmware to the peripheral headset device.
 * The peripheral headset device can then install the received firmware.
 * As the whole file can not be sent in a single request, the OAD Manager prepares & chunks it into small packets that are sent one after another.
 * It also communicates with the current firmware
 */
public final class OADManager {

    private final static String TAG = OADManager.class.getName();

    /**
     * Expected number of packets of the OAD binary file (chunks of the file that hold the firmware to install) to send to the headset device
     */
    public static final short EXPECTED_NB_PACKETS_BINARY_FILE = 14223;

    private final Context context;

    /**
     * Interface used to notify its listener when an OAD event occurs
     */
    private OADContract oadContract;

    /**
     * Return the current state of the current OAD update process.
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    private OADState currentState;
    /**
     * Packet counter that is incremented at every sent OAD packet (chunk of file that holds the firmware to install)
     */
    private PacketCounter packetCounter;

    private OADContext oadContext;

    private MbtAsyncWaitOperation<Boolean> lock;

    public OADManager(Context context, OADContract oadContract, Object initData) {
        this.context = context;
        this.oadContext = new OADContext();
        this.oadContract = oadContract;
        onOADStateChanged(OADState.INITIALIZING, initData);
    }

    /**
     * Switch to the OAD update state that follow the current state
     * @param actionData is an optional data associated to the action to perform
     */
    void switchToNextStep(@Nullable Object actionData) {
        LogUtils.d(TAG, "Switch to next step");
        onOADStateChanged(getCurrentState().nextState(), actionData);
    }

    /**
     * Change the current state, execute the associated action, and notify the client of this change
     * @param state the new state
     * @param actionData the associated data
     */
    void onOADStateChanged(OADState state, @Nullable Object actionData) {
        LogUtils.d(TAG, "OAD State changed : "+currentState +" > "+state);
        currentState = state;
        oadContract.notifyClient(new FirmwareUpdateClientEvent(currentState));
        if(currentState != null)
            currentState.executeAction(this, actionData);
    }

    /**
     * Abort the current OAD update
     */
    void abort() {
        onOADStateChanged(OADState.ABORTED, null);
    }

    /**
     * Initialize the OAD update process
     * Extract from the binary file that holds the new firmware:
     * - the firmware version
     * - the number of OAD packets (chunks of binary file)
     * @param firmwareVersion is the firmware version to install
     */
    void init(FirmwareVersion firmwareVersion){
        LogUtils.d(TAG, "Initialize the OAD update for version "+firmwareVersion);
        oadContext.setOADfileName(OADExtractionUtils.getFileNameForFirmwareVersion(firmwareVersion.getFirmwareVersionAsString()));
        try {
            byte[] content = OADExtractionUtils.extractFileContent(context.getAssets(), oadContext.getOADfileName());
            //Check that the OAD file content extracted is valid to start the OAD update
            if(content == null) {
                onError(OADError.ERROR_INIT_FAILED, "Impossible to read the OAD binary file.");
                return;
            } else if(content.length != EXPECTED_NB_BYTES_BINARY_FILE){
                onError(OADError.ERROR_INIT_FAILED,"Expected length is " + EXPECTED_NB_BYTES_BINARY_FILE + ", but found length was " + content.length + ".");
                return;
            }

            packetCounter = new PacketCounter(OAD_PACKET_SIZE, content.length);

            ArrayList<byte[]> oadPackets = OADExtractionUtils.extractOADPackets(content);
            if(oadPackets == null || oadPackets.isEmpty()){
                onError(OADError.ERROR_INIT_FAILED, "Impossible to extract the OAD packets");
                return;
            }
            oadContext.setPacketsToSend(oadPackets);

            String firmwareVersionFromContent = OADExtractionUtils.extractFirmwareVersionFromContent(content);
            if(firmwareVersionFromContent == null || firmwareVersionFromContent.isEmpty()){
                onError(OADError.ERROR_INIT_FAILED, "Impossible to extract the firmware version from content of OAD binary file.");
                return;
            }
            oadContext.setFirmwareVersion(firmwareVersionFromContent);
        } catch (FileNotFoundException e) {
            onError(OADError.ERROR_INIT_FAILED, e.getMessage());
            return;
        }
        switchToNextStep(null);
    }

    /**
     *  The whole OAD binary file previously chucked in packets is sent to the current firmware
     *  The packets are sent one after another using the WRITE_NO_RESPONSE capability.
     *  This means that the firmware will not notify the client that the packet is correctly received.
     * @param timeout maximum duration allocated to transfer all the OAD packets to the connected peripheral headsdet device.
     */
    void transferOADFile(int timeout){
        sendOADPacket(packetCounter.getIndexOfNextPacketToSend());
        switchToNextStep(null);

            boolean isTransferSuccess = waitUntilTimeout(timeout);
            if(isTransferSuccess)
                switchToNextStep(null);
            else
                onError(OADError.ERROR_TIMEOUT_UPDATE, "Transfer timed out.");
    }

    /**
     * Send an OAD packet to the peripheral headset device a Bluetooth write characteristic operation.
     * @param packetIndex is the index of the packet to send
     */
    private void sendOADPacket(int packetIndex) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(oadContract != null)
                    oadContract.transferPacket(oadContext.getPacketsToSend().get(packetIndex));
            }
        },100);
    }

    /**
     * Method triggered when an OAD packet has been well sent in Bluetooth to the peripheral headset device.
     */
    private void onOADPacketSent(){
        packetCounter.incrementNbPacketsCounted();
        onProgressChanged();
        if(packetCounter.areAllPacketsCounted()) {
            stopWaiting(true);
        }else
            sendOADPacket(packetCounter.getIndexOfNextPacketToSend());
    }

    /**
     * Wait an OAD event to be triggered until timeout
     */
    boolean waitUntilTimeout(int timeout){
        try {
            lock = new MbtAsyncWaitOperation<>();
            return lock.waitOperationResult(timeout);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LogUtils.e(TAG,""+e);
        }
        return false;
    }

    /**
     * Stop the current lock instance
     * @param isSuccess is the status/response provided by the stopWaiting caller
     */
    private void stopWaiting(boolean isSuccess){
        lock.stopWaitingOperation(isSuccess);
    }

    /**
     * Method triggered when a message/response of the headset device
     * related to the OAD update process
     * is received by the Device unit Manager
     */
    public void onOADEvent(OADEvent event) {
        LogUtils.d(TAG, "on OAD event "+event.toString());
        switch (event){
            case LOST_PACKET:
                packetCounter.setNbPacketsCounted(event.getEventDataAsByteArray()); //Packet index is set back to the requested value
                break;

            case DISCONNECTED:
                onError(OADError.ERROR_LOST_CONNECTION, null);
                break;

                case PACKET_TRANSFERRED:
                onOADPacketSent();
                break;

            default:
                if(lock.isWaiting())
                    stopWaiting(true);
                switchToNextStep(event.getEventData());
                break;
        }
    }

    private void onProgressChanged(){
        currentState.incrementProgress();
        oadContract.notifyClient(new FirmwareUpdateClientEvent(currentState.convertToProgress()));
    }

    void onError(BaseError error, String additionalInfo){
        oadContract.notifyClient(new FirmwareUpdateClientEvent(error, additionalInfo));
        abort();
    }
    /**
     * Return the current state of the current OAD update process.
     * Each step of the OAD update process is represented with a state machine
     * so that we can know which step is the current step at any moment of the update process.
     */
    public OADState getCurrentState(){
        return currentState;
    }

    /**
     * Returns the OAD context
     * @return the OAD context
     */
    OADContext getOADContext() {
        return oadContext;
    }

    /**
     * Return the OAD contract
     * @return the OAD contract
     */
    OADContract getOADContract() {
        return oadContract;
    }

    @VisibleForTesting
    PacketCounter getPacketCounter() {
        return packetCounter;
    }
}
