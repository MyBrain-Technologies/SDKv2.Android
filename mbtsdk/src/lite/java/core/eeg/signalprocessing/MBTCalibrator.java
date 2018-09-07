package core.eeg.signalprocessing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;

import core.eeg.storage.MbtEEGPacket;

/**
 * MBTCalibrator contains methods for calibrating new EEG packets
 *
 * @author Vincent on 24/11/2015.
 */
public final class MBTCalibrator {
    private static final String TAG = MBTCalibrator.class.getSimpleName();
    @NonNull
    public static HashMap<String, float[]> calibrateNew(final int sampRate, final int packetLength, final int smoothingDuration, @Nullable final MbtEEGPacket... packets){

        if (sampRate < 0)
            throw new IllegalArgumentException("samprate MUST BE POSITIVE!");
        if (packetLength < 0)
            throw new IllegalArgumentException("packetLength MUST BE POSITIVE!");
        if (packets == null || packets.length == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE packet(s) !");

        final float[][] qualities = new float[2][packets.length];
        final float[][] mainMatrix = new float[2][packets.length * packetLength];

        int qtCnt = 0;
        int chanCnt = 0;
        for (final MbtEEGPacket current : packets) {
            // Merging qualities
            if(current.getQualities() == null)
                Log.e(TAG, "NULL QUALITIES");
            else{
                qualities[0][qtCnt] = current.getQualities().get(0);
                qualities[1][qtCnt++] = current.getQualities().get(1);
            }

            //Merging channels
            Float[] channel1 = new Float[current.getChannelsData().get(0).size()];
            channel1 = current.getChannelsData().get(0).toArray(channel1);
            Float[] channel2 = new Float[current.getChannelsData().get(1).size()];
            channel2 = current.getChannelsData().get(1).toArray(channel2);

            final float[][] matrix = MBTSignalProcessingUtils.channelsToMatrixFloat(channel1, channel2);
            if(matrix[0].length > 0) {
                for (int it = 0; it < sampRate; it++) {
                    mainMatrix[0][chanCnt] = matrix[0][it];
                    mainMatrix[1][chanCnt++] = matrix[1][it];
                }
            }
        }
        return nativeCalibrateNew(sampRate, packetLength, packets.length, smoothingDuration, qualities, mainMatrix);
    }

    @NonNull
    public static HashMap<String, float[]> calibrateTest(final int sampRate, final int packetLength){
        final float[][] qualities = new float[2][250];
        final float[][] mainMatrix = new float[2][250];
        return nativeCalibrateNew(sampRate, packetLength, 30, ContextSP.smoothingDuration, null, mainMatrix);
    }

    @NonNull
    private native static HashMap<String, float[]> nativeCalibrateNew(final int samprate, final int packetLength, final int calibLength, final int smoothingDuration, final float[][] qualities, final float[][] matrix);
}
