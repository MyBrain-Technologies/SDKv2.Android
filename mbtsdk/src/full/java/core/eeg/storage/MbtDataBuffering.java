package core.eeg.storage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;

import config.MbtConfig;
import core.bluetooth.BtProtocol;
import core.eeg.MbtEEGManager;
import core.eeg.acquisition.MbtDataConversion;
import core.eeg.signalprocessing.ContextSP;
import core.eeg.signalprocessing.MBTSignalQualityChecker;
import features.MbtFeatures;
import features.ScannableDevices;
import mbtsdk.com.mybraintech.mbtsdk.BuildConfig;
import utils.MatrixUtils;

import static config.MbtConfig.getEegBufferLengthClientNotif;
import static core.bluetooth.BtProtocol.BLUETOOTH_LE;
import static features.MbtFeatures.getEEGByteSize;
import static features.MbtFeatures.getNbStatusBytes;
import static features.MbtFeatures.getRawDataBufferSize;
import static features.MbtFeatures.getRawDataBytesPerWholeChannelsSamples;
import static features.MbtFeatures.getRawDataPacketSize;
import static features.MbtFeatures.setNbStatusBytes;
import static features.MbtFeatures.setRawDataPacketSize;

/**
 * MbtDataBuffering is responsible for storing and managing EEG raw data acquired in temporary buffers.
 * Notifies {@link MbtDataConversion} when the raw EEG data buffer is full, so that it can convert stored EEG raw data into user-readable data.
 *
 * @author Sophie ZECRI on 10/04/2018
 * @version 25/05/2018
 */
public class MbtDataBuffering {

    private static final String TAG = MbtDataBuffering.class.getName();

    /**
     * Buffer that will manage the EEG <b>RAW</b> data. It stores {@link RawEEGSample} objects.
     */
    private ArrayList<RawEEGSample> pendingRawData; // fixed-size buffer containing eeg data

    /**
     * Object that will manage the EEG <b>CONSOLIDATED</b> data. The buffer is internal and accessible using
     * {@link MbtEEGPacket#getChannelsData()} method.
     */
    private MbtEEGPacket mbtEEGPacketsBuffer;

    /**
     * Reference to the EEG module manager
     */
    private MbtEEGManager eegManager;



    public MbtDataBuffering(@NonNull MbtEEGManager eegManagerController) {

        eegManager = eegManagerController;

        pendingRawData = new ArrayList<>();//new byte[getRawDataBufferSize()];
        mbtEEGPacketsBuffer = new MbtEEGPacket();
        try {
            System.loadLibrary(ContextSP.LIBRARY_NAME + BuildConfig.USE_ALGO_VERSION);
        } catch (final UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        ContextSP.SP_VERSION = MBTSignalQualityChecker.initQualityChecker();

    }

    /**
     * Stores a part of the EEG raw data inside the pendingRawData buffer when the maximum size of the buffer is reached
     * This part begins at the [srcPos] position and the number of components stored is equal to the length argument.
     * It is stored in the part of the destination array that begins at the [bufPos] position and ends at the [bufPos+length] position.
     * In case packet size is too large for buffer, the overflow buffer is stored in a second buffer
     * @param data the source raw EEG data array acquired by the headset and transmitted by Bluetooth to the application (data is null is packets have been lost)
     */
    public void storePendingDataInBuffer(@NonNull final ArrayList<RawEEGSample> data){
        pendingRawData.addAll(data);

        if(pendingRawData.size() >= MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE){
            notifyPendingRawDataBufferFull();
        }
    }

    /**
     * This method is called by {@link #storePendingDataInBuffer(ArrayList)} when the buffer size reaches the maximum
     * allowed size. It notifies EEG manager that the buffer is ready to be converted into consolidated EEG
     */
    @SuppressWarnings("unchecked")
    private void notifyPendingRawDataBufferFull() {
        final ArrayList<RawEEGSample> rawEEGtoConvert = (ArrayList<RawEEGSample>) pendingRawData.clone(); //the pending raw data is stored in toDecodeBytes to be converted in readable EEG values
        eegManager.convertToEEG(rawEEGtoConvert);
        pendingRawData.clear();
    }


    /**
     * This method is called by {@link #storePendingDataInBuffer(ArrayList)} when the buffer size reaches the maximum
     * allowed size. It notifies EEG manager that the buffer is ready to be converted into consolidated EEG
     */
    private void notifyClientEEGDataBufferFull() {
        eegManager.notifyEEGDataIsReady(mbtEEGPacketsBuffer);
    }



    /**
     * Stores the newly created EEG packet in the Packets buffer
     * We wait to have a full packet buffer to send these EEG values to the UI.
     * @return true if the packet buffer is full (contains a number of data equals to eegBufferLengthNotification), false otherwise.
     */
    public void storeConsolidatedEegPacketInPacketBuffer(@NonNull final ArrayList<ArrayList<Float>> consolidatedEEG, @NonNull ArrayList<Float> status) {


        int maxElementsToAppend = getBufferLengthClientNotif() - mbtEEGPacketsBuffer.getChannelsData().size();

        if(maxElementsToAppend > consolidatedEEG.size()){
            mbtEEGPacketsBuffer.getChannelsData().addAll(consolidatedEEG);
            if(mbtEEGPacketsBuffer.getStatusData() != null)
                mbtEEGPacketsBuffer.getStatusData().addAll(status);
        }else{
            if(maxElementsToAppend > 0){
                mbtEEGPacketsBuffer.getChannelsData().addAll(consolidatedEEG.subList(0, maxElementsToAppend));
                if(mbtEEGPacketsBuffer.getStatusData() != null)
                    mbtEEGPacketsBuffer.getStatusData().addAll(status.subList(0, (status.size()>= maxElementsToAppend) ? maxElementsToAppend : status.size()));
            }
            notifyClientEEGDataBufferFull();

            mbtEEGPacketsBuffer = new MbtEEGPacket( new ArrayList<>(consolidatedEEG.subList(maxElementsToAppend, consolidatedEEG.size())),
                    null, status.size() != 0 ?
                    ( new ArrayList<>(status.subList(maxElementsToAppend, status.size() >= consolidatedEEG.size() ? consolidatedEEG.size() : status.size() ))) : null );
        }
    }

    private ArrayList<Float> computeSignalQuality(){
        ArrayList<ArrayList<Float>> channelData = MatrixUtils.invertFloatMatrix(mbtEEGPacketsBuffer.getChannelsData());
        ArrayList<Float[]> channels = new ArrayList<>();
        for (int nbChannel = 0; nbChannel < MbtFeatures.getNbChannels() ; nbChannel++){
            channels.add(new Float[channelData.get(nbChannel).size()]);
            channelData.get(nbChannel).toArray(channels.get(nbChannel));
        }

        final float[] qts = (MbtConfig.getScannableDevices().equals(ScannableDevices.MELOMIND) ?
                MBTSignalQualityChecker.computeQualitiesForPacketNew(MbtFeatures.getSampleRate(),MbtFeatures.getSampleRate(), channels.get(0), channels.get(1)) :
                MBTSignalQualityChecker.computeQualitiesForPacketNew(MbtFeatures.getSampleRate(),MbtFeatures.getSampleRate(), channels.get(0), channels.get(1), channels.get(2), channels.get(3), channels.get(4), channels.get(5), channels.get(6), channels.get(7), channels.get(8))) ;
        ArrayList<Float> listedQualities = new ArrayList<Float>(Arrays.asList(ArrayUtils.toObject(qts)));
        return listedQualities;
    }

    /**
     * Reconfigures the temporary buffers that are used to store the raw EEG data until conversion to user-readable EEG data.
     * Reset the buffers, status and packet size
     */
    public void reinitBuffers(){

        pendingRawData.clear(); //init the buffer that we will use for handle/convert EEG raw data //TODO see if mandatory to reinit this buffer
        mbtEEGPacketsBuffer = new MbtEEGPacket();

    }

    /**
     * Gets the length of the buffer that contains the EEG packets
     * a notification is sent to {@link core.MbtManager} when the converted EEG data buffer is full, so that the client can have access to the user-readable EEG data
     * @return
     */
    private int getBufferLengthClientNotif(){
        return Math.max(getEegBufferLengthClientNotif(), MbtFeatures.DEFAULT_MAX_PENDING_RAW_DATA_BUFFER_SIZE); //todo ajouter size buffer C++
    }

}
