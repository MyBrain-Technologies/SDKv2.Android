package eventbus.events;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import core.eeg.storage.MbtEEGPacket;

/**
 * Event posted when a raw EEG data array has been converted to user-readable EEG matrix
 * Event data contains the converted EEG data matrix
 *
 * @author Sophie Zecri on 24/05/2018
 */
public interface SignalProcessingEvent{ //Events are just POJO without any specific implementation

    class GetBandpassFilter { //Events are just POJO without any specific implementation

        private float frequencyBoundMin;
        private float frequencyBoundMax;
        private float[] inputData;
        private int size;

        public GetBandpassFilter(float frequencyBoundMin, float frequencyBoundMax, float[] inputData, int size) {
            this.frequencyBoundMin = frequencyBoundMin;
            this.frequencyBoundMax = frequencyBoundMax;
            this.inputData = inputData;
            this.size = size;
        }

        public GetBandpassFilter(float frequencyBoundMin) {
            this.frequencyBoundMin = frequencyBoundMin;
        }

        public float getFrequencyBoundMin() {
            return frequencyBoundMin;
        }

        public float getFrequencyBoundMax() {
            return frequencyBoundMax;
        }

        public float[] getInputData() {
            return inputData;
        }

        public int getSize() {
            return size;
        }
    }

    class PostBandpassFilter { //Events are just POJO without any specific implementation

        private float[] outputData;

        public PostBandpassFilter(float[] outputData) {
            this.outputData = outputData;
        }

        public float[] getOutputData() {
            return outputData;
        }
    }

}
