package core.eeg;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import core.BaseModuleManager;
import core.Indus5FastMode;
import core.MbtManager;
import core.bluetooth.BluetoothProtocol;
import core.bluetooth.BluetoothState;
import core.bluetooth.StreamState;
import core.bluetooth.requests.StreamRequestEvent;
import core.device.model.MbtDevice;
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.signalprocessing.MBTCalibrator;
import core.eeg.signalprocessing.MBTComputeRelaxIndex;
import core.eeg.signalprocessing.MBTComputeStatistics;
import core.eeg.signalprocessing.MBTEegFilter;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import core.eeg.storage.MbtDataBuffering;
import core.eeg.storage.MbtEEGPacket;
import core.eeg.storage.RawEEGSample;
import eventbus.MbtEventBus;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.BluetoothEEGEvent;
import eventbus.events.EEGConfigEvent;
import eventbus.events.ConnectionStateEvent;
import eventbus.events.SignalProcessingEvent;
import features.MbtFeatures;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
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
    private static final int UNCHANGED_VALUE = -1;
    public static final int UNDEFINED_DURATION = -1;

    private int sampRate = MbtFeatures.DEFAULT_SAMPLE_RATE;
    private int packetLength = MbtFeatures.DEFAULT_EEG_PACKET_LENGTH;

//    private int nbChannels = MbtFeatures.MELOMIND_NB_CHANNELS; //TODO: think how we can different basic melomind with Q+

    private MbtDataAcquisition dataAcquisition;
    private MbtDataBuffering dataBuffering;
    private ArrayList<ArrayList<Float>> consolidatedEEG;

    private BluetoothProtocol protocol;

    private boolean hasQualities = false;

//    private boolean requestBeingProcessed = false;
//    private MbtEEGManager.RequestThread requestThread;
//    private Handler requestHandler;

    public MbtEEGManager(@NonNull Context context) {
        super(context);
        this.dataBuffering = new MbtDataBuffering(this);
        try {
            System.loadLibrary(ContextSP.LIBRARY_NAME + BuildConfig.USE_ALGO_VERSION);
        } catch (final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    int getNumberOfChannels() {
        if (Indus5FastMode.INSTANCE.isEnabled()) {
            return 4; //in Q+ melomind, there is 4 channels
        } else {
            return MbtFeatures.MELOMIND_NB_CHANNELS;
        }
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
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
    private void resetBuffers(byte samplePerNotif, final int statusByteNb, byte gain) {
        if(statusByteNb != UNCHANGED_VALUE)
            MbtFeatures.setNbStatusBytes(statusByteNb);

        if(samplePerNotif != UNCHANGED_VALUE) {
            MbtFeatures.setPacketSize(protocol, samplePerNotif);
            MbtFeatures.setSamplePerNotif(samplePerNotif);
        }
        dataBuffering.resetBuffers();
        dataAcquisition.resetIndex();
        MbtDataConversion.setGain(gain);
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
                consolidatedEEG = new ArrayList<>();
                ArrayList<Float> toDecodeStatus = new ArrayList<>();
                for (RawEEGSample rawEEGSample : toDecodeRawEEG) {
                    if (rawEEGSample.getStatus() != null) {
                        if (rawEEGSample.getStatus() != Float.NaN)
                            toDecodeStatus.add(rawEEGSample.getStatus());
                    }
                }
                consolidatedEEG = MbtDataConversion.convertRawDataToEEG(toDecodeRawEEG, protocol, getNumberOfChannels()); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

                dataBuffering.storeConsolidatedEegInPacketBuffer(consolidatedEEG, toDecodeStatus);// if the packet buffer is full, this method returns the non null packet buffer

            }
        });
    }

    /**
     * Publishes a ClientReadyEEGEvent event to the Event Bus to notify the client that the EEG raw data have been converted.
     * The event returns a list of MbtEEGPacket object, that contains the EEG data, and their associated qualities and status
     *
     * @param eegPackets the list that contains EEG packets ready to use for the client.
     */
    public void notifyEEGDataIsReady(@NonNull final MbtEEGPacket eegPackets) {

        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                if (hasQualities) {
                    eegPackets.setQualities(MbtEEGManager.this.computeEEGSignalQuality(eegPackets));
                    try{
                        if(Integer.parseInt(ContextSP.SP_VERSION.replace(".","")) >=
                                Integer.parseInt(FREQUENCY_BAND_FEATURES_VERSION.replace(".","")))
                            eegPackets.setFeatures(MBTSignalQualityChecker.getFeatures());
                    }catch (NumberFormatException e){
                        Log.e(TAG, "Qualities checker version unknown");
                    }
                }

                MbtEventBus.postEvent(new ClientReadyEEGEvent(eegPackets));
            }
        });
        LogUtils.d(TAG, "New packet: "+eegPackets.toString());
    }
    private final String FREQUENCY_BAND_FEATURES_VERSION = "2.3.1";

    /**
     * Initializes the main quality checker object in the JNI which will live throughout all session.
     * Should be destroyed at the end of the session
     */
    private void initQualityChecker() {
        ContextSP.SP_VERSION = MBTSignalQualityChecker.initQualityChecker();
    }

    /**
     * Destroy the main quality checker object in the JNI at the end of the session.
     */
    private void deinitQualityChecker() {
        MBTSignalQualityChecker.deinitQualityChecker();
    }

    /**
     * Computes the result of the previously done session
     *
     * @param threshold the level above which the relaxation indexes are considered in a relaxed state (under this threshold, they are considered not relaxed)
     * @param snrValues the array that contains the relaxation indexes of the session
     * @return the qualities for each provided channels
     */
    @NonNull
    public HashMap<String, Float> computeStatisticsSNR(final float threshold, @NonNull final Float[] snrValues) {
        return MBTComputeStatistics.computeStatisticsSNR(threshold, snrValues);
    }

    /**
     * Computes the quality for each provided channels
     *
     * @param packet the user-readable EEG data matrix
     *                        The Melomind headset has 2 channels and the VPRO headset has 9 channels.
     * @return an array that contains the quality of each EEG acquisition channels
     * This array contains 2 qualities (items) if the headset used is MELOMIND.
     * This array contains 9 qualities (items) if the headset used is VPRO.
     * The method computes and displays the duration for quality computation.
     */
    private ArrayList<Float> computeEEGSignalQuality(final MbtEEGPacket packet) {

        if(packet.getChannelsData() != null && packet.getChannelsData().size()!=0){

            float[] qualities = new float[getNumberOfChannels()];
            Arrays.fill(qualities, -1f);
            try{
                if(protocol.equals(BluetoothProtocol.LOW_ENERGY)){
                    qualities = MBTSignalQualityChecker.computeQualitiesForPacketNew(sampRate, packetLength, MatrixUtils.invertFloatMatrix(packet.getChannelsData()));

                }else if(protocol.equals(BluetoothProtocol.SPP)){
                    ArrayList<Float> qualitiesList = new ArrayList<>();
                    ArrayList<ArrayList<Float>> temp = new  ArrayList<ArrayList<Float>>();
                    //WARNING : quality checker C++ algo only takes into account 2 channels
                    for(int i = 0; i < getNumberOfChannels(); i+=2) {
                        final float[] qts;
                        temp.add(0, downsample(MatrixUtils.invertFloatMatrix(packet.getChannelsData()).get(i)));
                        temp.add(1, downsample(MatrixUtils.invertFloatMatrix(packet.getChannelsData()).get(i+1)));
                        int mSampRate = temp.get(0).size();
                        int mPacketLentgh = mSampRate;
                        qts = MBTSignalQualityChecker.computeQualitiesForPacketNew(mSampRate, mPacketLentgh, temp);
                        temp.clear();
                        for (float q : qts) {
                            qualitiesList.add(q);
                        }
                    }
                    return qualitiesList;
                }

            } catch (IllegalStateException e){
                e.printStackTrace();
            }

            return new ArrayList<>(Arrays.asList(ArrayUtils.toObject(qualities)));
        }

        return null;
    }

    /**
     * Temporary used to fix the quality checker bug for 500 Hz (non 250Hz)
     * @param vector 500 Hz
     * @return vector of 250 Hz (one of out 2 values are removed)
     */
    private ArrayList<Float> downsample(ArrayList<Float> vector){
        int size = 0;
        if(vector != null)
            size = vector.size()/2;

        ArrayList<Float> temp = new ArrayList<>(size);
        Iterator<Float> iterator = vector.iterator();
        boolean keep = true;
        while (iterator.hasNext()){
            Float value = iterator.next();
            if(keep){
                temp.add(value);
            }
            keep = !keep;
        }
        return temp;
    }
    /**
     * Computes the relaxation index using the provided <code>MbtEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     *
     * @param sampRate    the samprate of a channel (must be consistent)
     * @param calibParams the calibration paramters previously performed
     *                    the EEG packets containing EEG data, theirs status and qualities.
     * @return the relaxation index
     */
    private float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MbtEEGPacket... packets) {
        return MBTComputeRelaxIndex.computeRelaxIndex(sampRate, calibParams, packets);
    }

    /**
     * Computes the calibration parameters
     */
    private HashMap<String, float[]> calibrate(MbtEEGPacket... packets) {
        return MBTCalibrator.calibrateNew(sampRate, packetLength, ContextSP.smoothingDuration, packets);
    }

    /**
     * Resets the relaxation index.
     */
    private void resetRelaxIndexVariables() {
        MBTComputeRelaxIndex.resetRelaxIndexVariables();
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> getConsolidatedEEG() {
        return consolidatedEEG;
    }

    /**
     * Unregister the MbtEEGManager class from the bus to avoid memory leak
     */
    public void deinit() { //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        MbtEventBus.registerOrUnregister(false, this);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onConnectionStateChanged(ConnectionStateEvent connectionStateEvent) {
        if(connectionStateEvent.getDevice() == null) {
            protocol = null;
        }else {
            if(connectionStateEvent.getNewState().equals(BluetoothState.CONNECTED_AND_READY)){
                protocol = connectionStateEvent.getDevice().getDeviceType().getProtocol();
//                TODO: think how can we implement this, the number of eeg channels
//                nbChannels = connectionStateEvent.getDevice().getNbChannels();
            }
        }
    }

    /**
     * Handles the raw EEG data acquired by the headset and transmitted to the application
     * onEvent is called by the Event Bus when a BluetoothEEGEvent event is posted
     * This event is published by {@link core.bluetooth.MbtBluetoothManager}:
     * this manager handles Bluetooth communication between the headset and the application and receive raw EEG data from the headset.
     * @param event contains data transmitted by the publisher : here it contains the raw EEG data array
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(BluetoothEEGEvent event) { //warning : this method is used
        dataAcquisition.handleDataAcquired(event.getData(), getNumberOfChannels());
    }

    /**
     * Called when a new stream state event has been broadcast on the event bus.
     * @param newState
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onStreamStateChanged(StreamState newState) {
        if (newState == StreamState.STOPPED && dataAcquisition != null)
            resetBuffers((byte) UNCHANGED_VALUE, UNCHANGED_VALUE, (byte) 0);
    }

    @Subscribe
    public void onStreamStartedOrStopped(StreamRequestEvent event){
        if(event.startStream()){
            this.dataAcquisition = new MbtDataAcquisition(this, protocol);
            this.dataBuffering = new MbtDataBuffering(this);
            if(event.computeQualities()){
                hasQualities = true;
                initQualityChecker();
            }
        }
        else if(event.isStopStream() && !ContextSP.SP_VERSION.equals("0.0.0"))
            deinitQualityChecker();

    }

    @Subscribe
    public void applyBandpassFilter(SignalProcessingEvent.GetBandpassFilter config){
        MbtEventBus.postEvent(
                new SignalProcessingEvent.PostBandpassFilter(
                        MBTEegFilter.bandpassFilter(
                                config.getMinFrequency(),
                                config.getMaxFrequency(),
                                config.getSize(),
                                config.getInputSignal())
                )
        );
    }

    @Subscribe
    public void onConfigurationChanged(EEGConfigEvent configEEGEvent){
        MbtDevice.InternalConfig internalConfig = configEEGEvent.getConfig();
        LogUtils.d(TAG, "new config "+ internalConfig.toString());
        sampRate = internalConfig.getSampRate();
        packetLength = configEEGEvent.getDevice().getEegPacketLength();
//        nbChannels = internalConfig.getNbChannels();
        resetBuffers(internalConfig.getNbPackets(), internalConfig.getStatusBytes(), internalConfig.getGainValue());
    }

    public int getSampRate() {
        return sampRate;
    }

    @VisibleForTesting
    public void setTestConsolidatedEEG(ArrayList<ArrayList<Float>> consolidatedEEG) {
        this.consolidatedEEG = consolidatedEEG;
    }

    @VisibleForTesting
    public void setTestHasQualities(boolean hasQualities) {
        this.hasQualities = hasQualities;
    }
}
