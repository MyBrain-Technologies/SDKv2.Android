package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
import core.eeg.acquisition.MbtDataAcquisition;
import core.eeg.signalprocessing.MBTCalibrationParameters;
import core.eeg.signalprocessing.MBTComputeRelaxIndex;
import core.eeg.signalprocessing.MBTComputeStatistics;
import core.eeg.storage.MbtEEGPacket;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.storage.MbtDataBuffering;
import core.eeg.storage.RawEEGSample;
import eventbus.EventBusManager;
import eventbus.events.ClientReadyEEGEvent;
import eventbus.events.BluetoothEEGEvent;
import utils.AsyncUtils;
import utils.MatrixUtils;

import static config.MbtConfig.getEegBufferLengthClientNotif;
import static config.MbtConfig.getSampleRate;
import static core.eeg.signalprocessing.MBTSignalQualityChecker.computeQualitiesForPacketNew;
import static features.ScannableDevices.VPRO;


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

public final class MbtEEGManager extends BaseModuleManager{

    private static final String TAG = MbtEEGManager.class.getName();

    private MbtDataAcquisition dataAcquisition;
    private MbtDataBuffering mbtDataBuffering;
    private ArrayList<ArrayList<Float>> consolidatedEEG;

    private BtProtocol protocol;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController, @NonNull BtProtocol protocol){
        super(context, mbtManagerController);
        this.protocol = protocol;
        this.dataAcquisition = new MbtDataAcquisition(this, protocol);
        this.mbtDataBuffering = new MbtDataBuffering(this);
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param rawEEGdata the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public void storePendingDataInBuffer(@NonNull final ArrayList<RawEEGSample> rawEEGdata){
        mbtDataBuffering.storePendingDataInBuffer(rawEEGdata);
    }


    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers arrays, status list, the number of status bytes and the packet Size
     * @param sampleRate the sample rate
     * @param samplePerNotif the number of sample per notification
     * @param nbStatusBytes the number of bytes used for status data
     */
    public void reconfigureBuffers(final int sampleRate, byte samplePerNotif, final int nbStatusBytes){
        mbtDataBuffering.reconfigureBuffers(sampleRate,samplePerNotif,nbStatusBytes);
    }



    /**
     * Convert the raw EEG data array into a readable EEG data matrix of float values
     * and notify that EEG data is ready to the User Interface
     * @param toDecodeRawEEG the EEG raw data array to convert
     */
    public void convertToEEG(@NonNull final ArrayList<RawEEGSample> toDecodeRawEEG){
        AsyncUtils.executeAsync(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "computing and sending to application");

                ArrayList<Float> toDecodeStatus = new ArrayList<>();
                for (RawEEGSample rawEEGSample : toDecodeRawEEG) {
                    if(rawEEGSample.getStatus() != null){
                        if(rawEEGSample.getStatus() != Float.NaN)
                            toDecodeStatus.add(rawEEGSample.getStatus());
                    }
                }

                consolidatedEEG = MbtDataConversion.convertRawDataToEEG(toDecodeRawEEG, protocol); //convert byte table data to Float matrix and store the matrix in MbtEEGManager as eegResult attribute

                mbtDataBuffering.storeConsolidatedEegPacketInPacketBuffer(consolidatedEEG, toDecodeStatus);// if the packet buffer is full, this method returns the non null packet buffer

            }
        });
    }

    /**
     * Publishes a ClientReadyEEGEvent event to the Event Bus to notify the client that the EEG raw data have been converted.
     * The event returns a list of MbtEEGPacket object, that contains the EEG data, and their associated qualities and status
     * @param eegPackets the list that contains EEG packets ready to use for the client.
     */
    public void notifyEEGDataIsReady(@NonNull MbtEEGPacket eegPackets) {
        Log.d(TAG, "notify EEG Data Is Ready ");
        EventBusManager.postEvent(new ClientReadyEEGEvent(eegPackets));
    }

    /**
     * Initializes the main quality checker object in the JNI which will live throughout all session.
     * Should be destroyed at the end of the session
     */
    @NonNull
    public String initQualityChecker() {
        return MBTSignalQualityChecker.initQualityChecker();
    }

    /**
     * Destroy the main quality checker object in the JNI at the end of the session.
     */
    public void deinitQualityChecker() {
        MBTSignalQualityChecker.deinitQualityChecker();
    }

    /**
     * Computes the result of the previously done session
     * @param bestChannel the best quality channel index
     * @param sampRate the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param packets the EEG packets containing the EEG data matrix, their associated status and qualities.
     * @return the result of the previously done session
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public HashMap<String, Float> computeStatistics(final int bestChannel, final int sampRate, final int packetLength, final MbtEEGPacket... packets){
        return MBTComputeStatistics.computeStatistics(bestChannel, sampRate, packetLength, packets);
    }

    /**
     * Computes the result of the previously done session
     * @param threshold the level above which the relaxation indexes are considered in a relaxed state (under this threshold, they are considered not relaxed)
     * @param snrValues the array that contains the relaxation indexes of the session
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public HashMap<String, Float> computeStatisticsSNR(final float threshold, @NonNull final Float[] snrValues){
        return MBTComputeStatistics.computeStatisticsSNR(threshold, snrValues);
    }

    /**
     * Computes the quality for each provided channels
     * @param consolidatedEEG the user-readable EEG data matrix
     * @param packetLength how long is a packet (time x samprate)
     * The Melomind headset has 2 channels and the VPRO headset has 9 channels.
     * @return an array that contains the quality of each EEG acquisition channels
     * This array contains 2 qualities (items) if the headset used is MELOMIND.
     * This array contains 9 qualities (items) if the headset used is VPRO.
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public ArrayList<Float> computeEEGSignalQuality(ArrayList<ArrayList<Float>> consolidatedEEG, int packetLength){

        float[] qualities;
        Float[] channel1 = new Float[getSampleRate()];
        Float[] channel2 = new Float[getSampleRate()];
        //MELOMIND & VPRO headset have at least 2 channels
        Float[] channel3, channel4, channel5, channel6, channel7, channel8, channel9 = null;
        //For the VPRO, the others channels are initialized after
        consolidatedEEG.get(0).toArray(channel1);
        consolidatedEEG.get(1).toArray(channel2);

        if(MbtConfig.getScannableDevices().equals(VPRO)) {

            channel3 = new Float[getSampleRate()];
            channel4 = new Float[getSampleRate()];
            channel5 = new Float[getSampleRate()];
            channel6 = new Float[getSampleRate()];
            channel7 = new Float[getSampleRate()];
            channel8 = new Float[getSampleRate()];
            channel9 = new Float[getSampleRate()];

            consolidatedEEG.get(2).toArray(channel3);
            consolidatedEEG.get(3).toArray(channel4);
            consolidatedEEG.get(4).toArray(channel5);
            consolidatedEEG.get(5).toArray(channel6);
            consolidatedEEG.get(6).toArray(channel7);
            consolidatedEEG.get(7).toArray(channel8);
            consolidatedEEG.get(8).toArray(channel9);

            qualities = computeQualitiesForPacketNew(getSampleRate(),packetLength, channel1, channel2, channel3, channel4, channel5, channel6, channel7, channel8, channel9);
        }else //MELOMIND headset
            qualities = computeQualitiesForPacketNew(getSampleRate(),packetLength, channel1, channel2);
        return (ArrayList<Float>) Arrays.asList(ArrayUtils.toObject(qualities)); //convert float array into Float Arraylist
    }

    /**
     * Computes the relaxation index using the provided <code>MbtEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     * @param sampRate the samprate of a channel (must be consistent)
     * @param calibParams the calibration paramters previously performed
     * the EEG packets containing EEG data, theirs status and qualities.
     * @return the relaxation index
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public float computeRelaxIndex(int sampRate, MBTCalibrationParameters calibParams, MbtEEGPacket... packets){
        return MBTComputeRelaxIndex.computeRelaxIndex(sampRate,calibParams,packets);
    }

    /**
     * Resets the relaxation index.
     */
    public void reinitRelaxIndexVariables(){
        MBTComputeRelaxIndex.reinitRelaxIndexVariables();
    }


    /**
     * Gets the MbtManager instance.
     * MbtManager is responsible for managing all the package managers
     * @return the MbtManager instance.
     */
    public MbtManager getMbtManager() {
        return mbtManager;
    }

    /**
     * Gets the user-readable EEG data matrix
     * @return the converted EEG data matrix that contains readable values for any user
     */
    public ArrayList<ArrayList<Float>> getConsolidatedEEG() {
        return consolidatedEEG;
    }

    /**
     * Gets the instance of MbtDataAcquisition
     * @return the instance of MbtDataAcquisition
     */
    public MbtDataAcquisition getDataAcquisition() {
        return dataAcquisition;
    }

    /**
     * Unregister the MbtEEGManager class from the bus to avoid memory leak
     */
    public void deinit(){ //TODO CALL WHEN MbtEEGManager IS NOT USED ANYMORE TO AVOID MEMORY LEAK
        EventBusManager.registerOrUnregister(false,this);
    }


    /**
     * Handles the raw EEG data acquired by the headset and transmitted to the application
     * onEvent is called by the Event Bus when a BluetoothEEGEvent event is posted
     * This event is published by {@link core.bluetooth.MbtBluetoothManager}:
     * this manager handles Bluetooth communication between the headset and the application and receive raw EEG data from the headset.
     * @param event contains data transmitted by the publisher : here it contains the raw EEG data array
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(BluetoothEEGEvent event){ //warning : this method is used
        Log.i(TAG, Arrays.toString(event.getData()));
        dataAcquisition.handleDataAcquired(event.getData());
    }

}
