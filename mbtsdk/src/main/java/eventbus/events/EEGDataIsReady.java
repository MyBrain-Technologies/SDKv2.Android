package eventbus.events;

import android.support.annotation.NonNull;

import java.util.ArrayList;

public class EEGDataIsReady { //Events are just POJO without any specific implementation

    private ArrayList<ArrayList<Float>> matrix;
    private ArrayList<Float> status;
    private int sampleRate;
    private int nbChannels;

    public EEGDataIsReady(@NonNull ArrayList<ArrayList<Float>> matrix, ArrayList<Float> status, int sampleRate, int nbChannels) {
        this.matrix = matrix;
        this.status = status;
        this.sampleRate = sampleRate;
        this.nbChannels = nbChannels;
    }

    public ArrayList<ArrayList<Float>> getMatrix() {
        return matrix;
    }

    public ArrayList<Float> getStatus() {
        return status;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getNbChannels() {
        return nbChannels;
    }
}
