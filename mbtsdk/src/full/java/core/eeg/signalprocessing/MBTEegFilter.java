package core.eeg.signalprocessing;

public class MBTEegFilter {

    public static native float[] bandpassFilter(float freqBound1, float freqBound2, int size, float[] inputData_);

}
