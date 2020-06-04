package eventbus.events

/**
 * Event posted when a raw EEG data array has to be filtered using a bandpass filter
 * Event data contains the bounds of the bandpass filter
 *
 * @author Sophie Zecri on 24/05/2018
 */
interface SignalProcessingEvent : IEvent {
  /**
   * Request to apply a bandpass filter to the input signal to keep frequencies included between
   */
  class GetBandpassFilter
  /**
   * Apply a bandpass filter to the input signal to keep frequencies included between
   * @param minFrequency and
   * @param maxFrequency .
   * @param size is the number of EEG data of one channel
   * @param inputData is the array of EEG data to filter for one channel
   */(val minFrequency: Float, val maxFrequency: Float, val inputSignal: FloatArray, val size: Int)

  /**
   * Return the result of a bandpass filter operation on a signal
   */
  class PostBandpassFilter(val outputSignal: FloatArray)
}