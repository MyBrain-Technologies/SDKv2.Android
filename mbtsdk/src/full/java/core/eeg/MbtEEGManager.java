package core.eeg;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import org.apache.commons.lang.ArrayUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import config.MbtConfig;
import core.BaseModuleManager;
import core.MbtManager;
import core.bluetooth.BtProtocol;
import core.bluetooth.IStreamable;
import core.bluetooth.MbtBluetoothManager;
import core.bluetooth.requests.BluetoothRequests;
import core.bluetooth.requests.ConnectRequestEvent;
import core.bluetooth.requests.DisconnectRequestEvent;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.signalprocessing.MBTComputeRelaxIndex;
import core.eeg.signalprocessing.MBTComputeStatistics;
import core.eeg.signalprocessing.requests.EegRequests;
import core.eeg.signalprocessing.requests.QualityRequest;
import core.eeg.storage.MbtEEGPacket;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.storage.MbtDataBuffering;
import core.eeg.storage.RawEEGSample;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.BluetoothEEGEvent;
import features.MbtFeatures;
import features.ScannableDevices;
import utils.AsyncUtils;
import utils.LogUtils;
import utils.MatrixUtils;


/**
 * MbtEEGManager contains all necessary methods to manage incoming EEG data from the MBT headset.
 * It is responsible for managing the communication between the different classes of the eeg package.
 * In chronological order, the incoming raw data are first transmitted to {@link MbtDataAcquisition},
 * to be stored by {@link MbtDataBuffering} in temporary buffers.
 * EEG data acquisition still continue until the buffers are full.
 * Then, raw data are converted into user-readable EEG values by {@link MbtDataConversion}.
 * Finally, a notification is sent to {@link MbtManager} when the converted EEG data buffer is full, so that the client can have access to the user-readable EEG data
 *
 * @author Etienne on 08/02/2018.
 * @version Sophie ZECRI 25/05/2018
 */

public final class MbtEEGManager extends BaseModuleManager {

    private static final String TAG = MbtEEGManager.class.getName();

    private MbtDataAcquisition dataAcquisition;
    private MbtDataBuffering dataBuffering;
    private ArrayList<ArrayList<Float>> consolidatedEEG;

    private BtProtocol protocol;

    private boolean requestBeingProcessed = false;
    private MbtEEGManager.RequestThread requestThread;
    private Handler requestHandler;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController, @NonNull BtProtocol protocol) {
        super(context, mbtManagerController);
        this.protocol = protocol;
        this.dataAcquisition = new MbtDataAcquisition(this, protocol);
        this.dataBuffering = new MbtDataBuffering(this);

        requestThread = new MbtEEGManager.RequestThread("requestThread", Thread.MAX_PRIORITY);
        requestThread.start();
        requestHandler = new Handler(requestThread.getLooper());
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     *
     * @param rawEEGdata the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public void storePendingDataInBuffer(@NonNull final ArrayList<RawEEGSample> rawEEGdata) {
        dataBuffering.storePendingDataInBuffer(rawEEGdata);
    }


    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers arrays, status list, the number of status bytes and the packet Size
     */
    private void reinitBuffers() {
        dataBuffering.reinitBuffers();
        dataAcquisition.resetIndex();
    }


    /**
     * Convert the raw EEG data array into a readable EEG data matrix of float values
     * and notify that EEG data is ready to the User Interface
     *
     * @param toDecodeRawEEG the EEG raw data array to convert
     */
    public void convertToEEG(@NonNull final ArrayList<RawEEGSample> toDecodeRawEEG) {
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                LogUtils.i(TAG, "computing and sending to application");

                ArrayList<Float> toDecodeStatus = new ArrayList<>();
                for (RawEEGSample rawEEGSample : toDecodeRawEEG) {
                    if (rawEEGSample.getStatus() != null) {
                        if (rawEEGSample.getStatus() != Float.NaN)
                            toDecodeStatus.add(rawEEGSample.getStatus());
                    }
                }

                consolidatedEEG = MbtDataConversion.convertRawDataToEEG(toDecodeRawEEG, protocol); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

                dataBuffering.storeConsolidatedEegPacketInPacketBuffer(consolidatedEEG, toDecodeStatus);// if the packet buffer is full, this method returns the non null packet buffer

            }
        });
    }

    /**
     * Publishes a ClientReadyEEGEvent event to the Event Bus to notify the client that the EEG raw data have been converted.
     * The event returns a list of MbtEEGPacket object, that contains the EEG data, and their associated qualities and status
     *
     * @param eegPackets the list that contains EEG packets ready to use for the client.
     */
    public void notifyEEGDataIsReady(@NonNull MbtEEGPacket eegPackets) {
        LogUtils.d(TAG, "notify EEG Data Is Ready ");
        EventBusManager.postEvent(new ClientReadyEEGEvent(eegPackets));
    }

    /**
     * Initializes the main quality checker object in the JNI which will live throughout all session.
     * Should be destroyed at the end of the session
     */
    @NonNull
    public void initQualityChecker() {
        MBTSignalQualityChecker.initQualityChecker();
    }

    /**
     * Destroy the main quality checker object in the JNI at the end of the session.
     */
    public void deinitQualityChecker() {
        MBTSignalQualityChecker.deinitQualityChecker();
    }

    /**
     * Computes the result of the previously done session
     *
     * @param bestChannel  the best quality channel index
     * @param sampRate     the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param packets      the EEG packets containing the EEG data matrix, their associated status and qualities.
     * @return the result of the previously done session
     * @throws IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public HashMap<String, Float> computeStatistics(final int bestChannel, final int sampRate, final int packetLength, final MbtEEGPacket... packets) {
        return MBTComputeStatistics.computeStatistics(bestChannel, sampRate, packetLength, packets);
    }

    /**
     * Computes the result of the previously done session
     *
     * @param threshold the level above which the relaxation indexes are considered in a relaxed state (under this threshold, they are considered not relaxed)
     * @param snrValues the array that contains the relaxation indexes of the session
     * @return the qualities for each provided channels
     * @throws IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public HashMap<String, Float> computeStatisticsSNR(final float threshold, @NonNull final Float[] snrValues) {
        return MBTComputeStatistics.computeStatisticsSNR(threshold, snrValues);
    }

    /**
     * Computes the quality for each provided channels
     *
     * @param consolidatedEEG the user-readable EEG data matrix
     *                        The Melomind headset has 2 channels and the VPRO headset has 9 channels.
     * @return an array that contains the quality of each EEG acquisition channels
     * This array contains 2 qualities (items) if the headset used is MELOMIND.
     * This array contains 9 qualities (items) if the headset used is VPRO.
     * @throws IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public void computeEEGSignalQuality(ArrayList<ArrayList<Float>> consolidatedEEG) {

        ArrayList<Float> listedQualities = new ArrayList<>();

        if(consolidatedEEG != null && consolidatedEEG.size()!=0){
            consolidatedEEG = MatrixUtils.invertFloatMatrix(consolidatedEEG);
            ArrayList<Float[]> channels = new ArrayList<>();
            for (int nbChannel = 0; nbChannel < consolidatedEEG.size(); nbChannel++) {
                channels.add(new Float[consolidatedEEG.get(nbChannel).size()]);
                consolidatedEEG.get(nbChannel).toArray(channels.get(nbChannel));
            }

            final float[] qualities = (MbtConfig.getScannableDevices().equals(ScannableDevices.MELOMIND) ?
                    MBTSignalQualityChecker.computeQualitiesForPacketNew(MbtFeatures.getSampleRate(), MbtFeatures.getSampleRate(), channels.get(0), channels.get(1)) :
                    MBTSignalQualityChecker.computeQualitiesForPacketNew(MbtFeatures.getSampleRate(), MbtFeatures.getSampleRate(), channels.get(0), channels.get(1), channels.get(2), channels.get(3), channels.get(4), channels.get(5), channels.get(6), channels.get(7), channels.get(8)));
            listedQualities = new ArrayList<>(Arrays.asList(ArrayUtils.toObject(qualities)));
        }
        EventBusManager.postEvent(new QualityRequest(null, listedQualities));
        requestBeingProcessed  = false;
    }

    /**
     * Computes the relaxation index using the provided <code>MbtEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     *
     * @param sampRate    the samprate of a channel (must be consistent)
     * @param calibParams the calibration paramters previously performed
     *                    the EEG packets containing EEG data, theirs status and qualities.
     * @return the relaxation index
     * @throws IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MbtEEGPacket... packets) {
        return MBTComputeRelaxIndex.computeRelaxIndex(sampRate, calibParams, packets);
    }

    /**
     * Resets the relaxation index.
     */
    public void reinitRelaxIndexVariables() {
        MBTComputeRelaxIndex.reinitRelaxIndexVariables();
    }


    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     *
     * @return the MbtManager instance.
     */
    public MbtManager getMbtManager() {
        return mbtManager;
    }

    /**
     * Gets the user-readable EEG data matrix
     *
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> getConsolidatedEEG() {
        return consolidatedEEG;
    }

    /**
     * Gets the instance of MbtDataAcquisition
     *
     * @return the instance of MbtDataAcquisition
     */
    public MbtDataAcquisition getDataAcquisition() {
        return dataAcquisition;
    }

    /**
     * Unregister the MbtEEGManager class from the bus to avoid memory leak
     */
    public void deinit() { //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        EventBusManager.registerOrUnregister(false, this);
    }


    /**
     * Handles the raw EEG data acquired by the headset and transmitted to the application
     * onEvent is called by the Event Bus when a BluetoothEEGEvent event is posted
     * This event is published by {@link core.bluetooth.MbtBluetoothManager}:
     * this manager handles Bluetooth communication between the headset and the application and receive raw EEG data from the headset.
     *
     * @param event contains data transmitted by the publisher : here it contains the raw EEG data array
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(BluetoothEEGEvent event) { //warning : this method is used
        dataAcquisition.handleDataAcquired(event.getData());
    }

    /**
     * Called when a new stream state event has been broadcast on the event bus.
     *
     * @param newState
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onStreamStateChanged(IStreamable.StreamState newState) {
        if (newState == IStreamable.StreamState.STOPPED)
            reinitBuffers();
    }


    /**
     * Add the new {@link EegRequests} to the handler thread that will execute tasks one after another
     * This method must return quickly in order not to block the thread.
     * @param request the new {@link EegRequests } to execute
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNewEegRequest(final EegRequests request){
        LogUtils.i(TAG, "onNewEEGRequest");

            requestHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestThread.parseRequest(request);
                }
            });
    }

    private class RequestThread extends HandlerThread {
        RequestThread(String name) {
            super(name);
        }

        RequestThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
        }

        /**
         * Checks the subclass type of {@link EegRequests} and handles the correct method/action to perform.
         *
         * @param request the {@link EegRequests} request to execute.
         */
        void parseRequest(EegRequests request) {
            LogUtils.i(TAG, "parsing new request");
            requestBeingProcessed = true;
            if(request instanceof QualityRequest){
                computeEEGSignalQuality(((QualityRequest) request).getEegMatrix());
            }
        }
    }

}
