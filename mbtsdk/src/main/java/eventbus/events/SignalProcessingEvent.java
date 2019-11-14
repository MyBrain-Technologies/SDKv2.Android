package eventbus.events;


/**
 * Event posted when a raw EEG data array has to be filtered using a bandpass filter
 * Event data contains the bounds of the bandpass filter 
 *
 * @author Sophie Zecri on 24/05/2018
 */
public interface SignalProcessingEvent{

    /**
     * Request to apply a bandpass filter to the input signal to keep frequencies included between
     */
    class GetBandpassFilter { 

        private float minFrequency;
        private float maxFrequency;
        private float[] inputSignal;
        private int size;

        /**
         * Apply a bandpass filter to the input signal to keep frequencies included between
         * @param minFrequency and
         * @param maxFrequency .
         * @param size is the number of EEG data of one channel
         * @param inputData is the array of EEG data to filter for one channel
         */
        public GetBandpassFilter(float minFrequency, float maxFrequency, float[] inputData, int size) {
            this.minFrequency = minFrequency;
            this.maxFrequency = maxFrequency;
            this.inputSignal = inputData;
            this.size = size;
        }

        public float getMinFrequency() {
            return minFrequency;
        }

        public float getMaxFrequency() {
            return maxFrequency;
        }

        public float[] getInputSignal() {
            return inputSignal;
        }

        public int getSize() {
            return size;
        }
    }

    /**
     * Return the result of a bandpass filter operation on a signal
     */
    class PostBandpassFilter {

        private float[] outputSignal;

        public PostBandpassFilter(float[] outputData) {
            this.outputSignal = outputData;
        }

        public float[] getOutputSignal() {
            return outputSignal;
        }
    }

}
