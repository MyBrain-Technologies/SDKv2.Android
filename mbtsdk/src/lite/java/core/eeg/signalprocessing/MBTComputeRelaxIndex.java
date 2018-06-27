package core.eeg.signalprocessing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import core.eeg.storage.MbtEEGPacket;

/**
 * MBTComputeRelaxIndex contains methods for computing Relaxation Indexes
 *
 * @author Vincent on 26/11/2015.
 */
public final class MBTComputeRelaxIndex {
    private static final String TAG = MBTComputeRelaxIndex.class.getName();


    /**
     * Computes the relaxation index using the provided <code>MbtEEGPacket</code>.
     * For now, we admit there are only 2 channels for each packet
     * @param samprate the samprate of a channel (must be consistent)
     * @param calibParams the calibration paramters previously performed
     * @param packets the EEG packets containing EEG data, theirs status and qualities.
     * @return the relaxation index
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    public static float computeRelaxIndex(final int samprate,
                                          @Nullable final MBTCalibrationParameters calibParams,
                                          @Nullable final MbtEEGPacket... packets) {
        if (samprate < 0)
            throw new IllegalArgumentException("samprate MUST BE POSITIVE!");
        if (calibParams == null || calibParams.getSize() == 0)
            throw new IllegalArgumentException("invalid calibration parameters : cannot be NULL or EMPTY!");
        if (packets == null || packets.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE packet(s) !");

        final float[][] qualities = new float[2][packets.length];
        final float[][] mainMatrix = new float[2][packets.length * samprate];

        int qtCnt = 0;
        int chanCnt = 0;
        for (final MbtEEGPacket current : packets) {
             // Merging qualities

           // qualities[0][qtCnt] = current.getQualities().get(0); //todo decomment
           // qualities[1][qtCnt++] = current.getQualities().get(1); //todo decomment

             // Merging channels
// Merging channels
            Float[] channel1 = new Float[current.getChannelsData().get(0).size()];
            channel1 = current.getChannelsData().get(0).toArray(channel1);
            Float[] channel2 = new Float[current.getChannelsData().get(1).size()];
            channel2 = current.getChannelsData().get(1).toArray(channel2);

            final float[][] matrix = MBTSignalProcessingUtils.channelsToMatrixFloat(channel1, channel2);
            for (int it = 0; it < samprate; it++) {
                mainMatrix[0][chanCnt] = matrix[0][it];
                mainMatrix[1][chanCnt++] = matrix[1][it];
            }
        }
        return nativeComputeRelaxIndex(samprate, ContextSP.smoothingDuration, calibParams, qualities, mainMatrix);
    }


    @NonNull
    public static Map<String, float[]> getSessionMetadata(){

        return nativeGetSessionMetadata();
    }


    public static void reinitRelaxIndexVariables(){
        nativeReinitRelaxIndexVariables();
    }

    private native static void nativeReinitRelaxIndexVariables();


    private native static float nativeComputeRelaxIndex(final int samprate, final int smoothingDuration,
                                                        final MBTCalibrationParameters parameters,
                                                        final float[][] qualities, final float[][] matrix);

    @NonNull
    private native static HashMap<String, float[]> nativeGetSessionMetadata();


}
