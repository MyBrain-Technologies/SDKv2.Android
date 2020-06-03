package eventbus.events

/**
 * Event posted when a new raw EEG data acquired is transmitted through Bluetooth
 * Event data contains the raw EEG data array
 *
 * @author Sophie Zecri on 29/05/2018
 */
class BluetoothEEGEvent(val data: ByteArray) : IEvent {
  /**
   * Gets the raw EEG data array acquired
   * @return the raw EEG data array acquired
   */

}