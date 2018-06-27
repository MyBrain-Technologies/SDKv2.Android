package core.eeg.signalprocessing.requests;

import java.util.ArrayList;

/**
 * An event class when a EEG signal quality computation is requested
 */
public class QualityRequest extends EegRequests{

    private ArrayList<ArrayList<Float>> eegMatrix;
    private ArrayList<Float> qualities;

    public ArrayList<ArrayList<Float>> getEegMatrix() {
        return eegMatrix;
    }

    public ArrayList<Float> getQualities() {
        return qualities;
    }

    public QualityRequest(ArrayList<ArrayList<Float>> eegMatrix, ArrayList<Float> qualities) {
        this.eegMatrix = eegMatrix;
        this.qualities = qualities;
    }
}
