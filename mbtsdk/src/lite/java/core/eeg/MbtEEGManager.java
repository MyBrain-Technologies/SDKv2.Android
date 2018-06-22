package core.eeg;

import android.content.Context;
import android.support.annotation.NonNull;
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
import core.bluetooth.IStreamable;
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
import utils.LogUtils;

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
    private MbtDataBuffering dataBuffering;
    private ArrayList<ArrayList<Float>> consolidatedEEG;

    private BtProtocol protocol;

    public MbtEEGManager(@NonNull Context context, MbtManager mbtManagerController, @NonNull BtProtocol protocol){
        super(context, mbtManagerController);
        this.protocol = protocol;
        this.dataAcquisition = new MbtDataAcquisition(this, protocol);
        this.dataBuffering = new MbtDataBuffering(this);
    }

    /**
     * Stores the EEG raw data buffer when the maximum size of the buffer is reached
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param rawEEGdata the raw EEG data array acquired by the headset and transmitted by Bluetooth to the application
     */
    public void storePendingDataInBuffer(@NonNull final ArrayList<RawEEGSample> rawEEGdata){
        dataBuffering.storePendingDataInBuffer(rawEEGdata);
    }


    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers arrays, status list, the number of status bytes and the packet Size
     */
    private void reinitBuffers(){
        dataBuffering.reinitBuffers();
        dataAcquisition.resetIndex();
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
                LogUtils.i(TAG, "computing and sending to application");

                ArrayList<Float> toDecodeStatus = new ArrayList<>();
                for (RawEEGSample rawEEGSample : toDecodeRawEEG) {
                    if(rawEEGSample.getStatus() != null){
                        if(rawEEGSample.getStatus() != Float.NaN)
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
        dataAcquisition.handleDataAcquired(event.getData());
    }

    /**
     * Called when a new stream state event has been broadcast on the event bus.
     * @param newState
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onStreamStateChanged(IStreamable.StreamState newState){
        if(newState == IStreamable.StreamState.STOPPED)
            reinitBuffers();
    }

}
