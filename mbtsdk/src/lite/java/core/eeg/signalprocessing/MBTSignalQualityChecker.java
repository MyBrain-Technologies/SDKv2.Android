package core.eeg.signalprocessing;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;

import utils.AsyncUtils;
import utils.LogUtils;

/**
 * MBTSignalQualityChecker contains methods for computing the EEG signal quality
 *
 * @author Vincent on 26/08/2015.
 */
public final class MBTSignalQualityChecker {

    private static final String TAG = MBTSignalQualityChecker.class.getSimpleName();

    public static QCStateMachine qcCurrentState = QCStateMachine.NOT_READY;

    /**
     * Initializes the MBT_MainQC object in the JNI which will live throughout all session.
     * Should be destroyed at the end of the session
     */
    @NonNull
    public static String initQualityChecker(){
        if(qcCurrentState != QCStateMachine.NOT_READY)
            return "";

        qcCurrentState = QCStateMachine.INIT;

        String res = nativeInitQualityChecker();

        qcCurrentState = QCStateMachine.IDLE;

        return res;
    }


    /**
     * Destroy the MBT_MainQc object in the JNI at the end of the session.
     */
    public static void deinitQualityChecker(){

        while(qcCurrentState == QCStateMachine.COMPUTING);
        LogUtils.d(TAG, "deinit quality checker started");
        qcCurrentState = QCStateMachine.DEINIT;
        nativeDeinitQualityChecker();
        qcCurrentState = QCStateMachine.NOT_READY;

    }


    /**
     * Computes the quality for each provided channels
     * @param samprate the number of value(s) inside each channel
     * @param packetLength how long is a packet (time x samprate)
     * @param channels the channel(s) to be computed
     * @return the qualities for each provided channels
     * @exception IllegalArgumentException if any of the provided arguments are <code>null</code> or invalid
     */
    @NonNull
    public static float[] computeQualitiesForPacketNew(final int samprate, final int packetLength,
                                                       @Nullable final ArrayList<ArrayList<Float>> channels ) {

        if (samprate < 0)
            throw new IllegalArgumentException("samprate MUST BE POSITIVE!");
        if (packetLength < 0)
            throw new IllegalArgumentException("packetLength MUST BE POSITIVE!");
        if (channels == null || channels.size() == 0)
            throw new IllegalArgumentException("there MUST be at least ONE or MORE channel(s) !");

        //checking first if quality checker is not in an invalid state
        if(qcCurrentState == QCStateMachine.DEINIT || qcCurrentState == QCStateMachine.NOT_READY)
            throw new IllegalStateException("quality checker is not in a valid state");

        while(qcCurrentState != QCStateMachine.IDLE);

        qcCurrentState = QCStateMachine.COMPUTING;
        float[] res = nativeComputeQualityCheckerNew(MBTSignalProcessingUtils.channelsToMatrixFloat(channels), samprate, packetLength);
        qcCurrentState = QCStateMachine.IDLE;
        return res;
    }



    @NonNull
    public static Float[][] getModifiedInputData(){
        float [][] modifiedData = nativeGetModifiedInputData();
        Float[][] data = new Float[modifiedData.length][];
        for (int i = 0; i<modifiedData.length; i++){
            Float[] temp = ArrayUtils.toObject(modifiedData[i]);
            data[i] = temp;
        }
        return data;
    }


    @NonNull
    private native static String nativeInitQualityChecker();

    private native static void nativeDeinitQualityChecker();

    @NonNull
    private native static float[][] nativeGetModifiedInputData();

    @NonNull
    public native static float[] nativeComputeQualityCheckerNew(float[][] matrix, int samprate, int packetLength);

    enum QCStateMachine{
        NOT_READY,
        INIT,
        IDLE,
        COMPUTING,
        DEINIT
    }
}
