package core.eeg.signalprocessing;

import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.lang.ArrayUtils;

import java.util.HashMap;

import core.eeg.storage.MbtEEGPacket;

/**
 * MBTComputeStatistics contains methods for computing statistics
 *
 * @author Etienne on 12/04/2017.
 */
public class MBTComputeStatistics {
    private static final String TAG = MBTComputeStatistics.class.getName();

    @NonNull
    public static HashMap<String, Float> computeStatistics(final int bestChannel, final int sampRate, final int packetLength, final MbtEEGPacket... packets){

        final float[][] qualities = new float[2][packetLength];
        final float[][] mainMatrix = new float[2][sampRate * packetLength];
        float[] bestChannelData = {0,0};

        int qtCnt = 0;
        int chanCnt = 0;
        for (final MbtEEGPacket current : packets) {
            // Merging qualities
            // qualities[0][qtCnt] = current.getQualities().get(0);  //todo decomment
            // qualities[1][qtCnt++] = current.getQualities().get(1);  //todo decomment

// Merging channels
            Float[] channel1 = new Float[current.getChannelsData().get(0).size()];
            channel1 = current.getChannelsData().get(0).toArray(channel1);
            Float[] channel2 = new Float[current.getChannelsData().get(1).size()];
            channel2 = current.getChannelsData().get(1).toArray(channel2);

            final float[][] matrix = MBTSignalProcessingUtils.channelsToMatrixFloat(channel1, channel2);
            for (int it = 0; it < sampRate; it++) {
                mainMatrix[0][chanCnt] = matrix[0][it];
                mainMatrix[1][chanCnt++] = matrix[1][it];
            }
        }

        bestChannelData = mainMatrix[bestChannel];

        HashMap<String, Float> result = new HashMap<>();
        long timeBefore = System.currentTimeMillis();
        result = nativeComputeStatistics(sampRate, packetLength, bestChannelData);
        long timeAfter = System.currentTimeMillis();
        Log.d(TAG, "Statistics computation time : " + (timeAfter-timeBefore));
        return result;
    }

    @NonNull
    public static HashMap<String, Float> computeStatisticsSNR(final float threshold, final Float[] snrValues){


        HashMap<String, Float> result = new HashMap<>();
        long timeBefore = System.currentTimeMillis();
        result = nativeComputeStatisticsSNR(threshold, snrValues.length,  ArrayUtils.toPrimitive(snrValues));
        long timeAfter = System.currentTimeMillis();
        Log.d(TAG, "Statistics computation time : " + (timeAfter-timeBefore));
        return result;
    }

    @NonNull
    private native static HashMap<String, Float> nativeComputeStatistics(final int sampRate, final int packetLength, final float[] inputData);

    @NonNull
    private native static HashMap<String, Float> nativeComputeStatisticsSNR(final float threshold, final int size, final float[] snrValues);

}
