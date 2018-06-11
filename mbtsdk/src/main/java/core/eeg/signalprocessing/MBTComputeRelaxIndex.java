package core.eeg.signalprocessing;

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
                                          final MBTCalibrationParameters calibParams,
                                          final MbtEEGPacket... packets) {
        if (samprate < 0)
            throw new IllegalArgumentException("samprate MUST BE POSITIVE!");
        if (calibParams == null || calibParams.getSize() == 0)
            throw new IllegalArgumentException("invalid calibration parameters : cannot be NULL or EMPTY!");
        if (packets == null || packets.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE packet(s) !");

        //Log.i(TAG, "starting relax index computation...");
        final float[][] qualities = new float[2][packets.length];
        final float[][] mainMatrix = new float[2][packets.length * samprate];

        int qtCnt = 0;
        int chanCnt = 0;
        for (final MbtEEGPacket current : packets) {
            // Merging qualities
            if(current == null){
//                Log.e(TAG, "error null value");
            }


            qualities[0][qtCnt] = current.getQualities().get(0);
            qualities[1][qtCnt++] = current.getQualities().get(1);

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
        return nativeComputeRelaxIndex(samprate, calibParams, qualities, mainMatrix);
    }


    public static Map<String, float[]> getSessionMetadata(){

        return nativeGetSessionMetadata();
    }


    public static void reinitRelaxIndexVariables(){
        nativeReinitRelaxIndexVariables();
    }

    private native static void nativeReinitRelaxIndexVariables();


    private native static float nativeComputeRelaxIndex(final int samprate,
                                                        final MBTCalibrationParameters parameters,
                                                        final float[][] qualities, final float[][] matrix);

    private native static HashMap<String, float[]> nativeGetSessionMetadata();


}
