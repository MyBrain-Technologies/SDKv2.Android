package core.bluetooth.lowenergy

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import core.bluetooth.lowenergy.MbtBluetoothLE
import core.bluetooth.lowenergy.MbtGattController
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_HEADSET_STATUS
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_EEG
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.CHARAC_MEASUREMENT_MAILBOX
import core.bluetooth.lowenergy.MelomindCharacteristics.Companion.SERVICE_MEASUREMENT
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import utils.CommandUtils
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(MbtGattController::class)
class MbtGattControllerTest {
  private val MAILBOX_RAW_RESPONSE_5 = byteArrayOf(5, 10, 1, 2, 3, 4, 5)
  private val MAILBOX_RAW_RESPONSE_0 = byteArrayOf(0, 10, 1, 2, 3, 4, 5)
  private val MAILBOX_RESPONSE = byteArrayOf(10, 1, 2, 3, 4, 5)
  private val IN_PROGRESS_RESPONSE = byteArrayOf(1, 1, 2, 3, 4, 5)
  private val SERVICE = SERVICE_MEASUREMENT
  private val CHARACTERISTIC_MAILBOX = CHARAC_MEASUREMENT_MAILBOX
  private val CHARACTERISTIC_EEG = CHARAC_MEASUREMENT_EEG
  private val CHARACTERISTIC_STATUS = CHARAC_HEADSET_STATUS
  private val UNKNOWN = UUID.fromString("0-0-0-0-0")
  private lateinit var gattController: MbtGattController
  private lateinit var spyGattController: MbtGattController
  private lateinit var mbtBluetoothLE: MbtBluetoothLE
  private lateinit var gatt: BluetoothGatt
  private lateinit var characteristic: BluetoothGattCharacteristic

  @Before
  @Throws(Exception::class)
  fun setUp() {
    val context = Mockito.mock(Context::class.java)
    mbtBluetoothLE = Mockito.mock(MbtBluetoothLE::class.java)
    gattController = MbtGattController(mbtBluetoothLE)
    spyGattController = PowerMockito.spy(gattController)
    gatt = Mockito.mock(BluetoothGatt::class.java)
    characteristic = Mockito.mock(BluetoothGattCharacteristic::class.java)
  }

  /**
   * Check that the Bluetooth Gatt controller
   * notifies the Bluetooth LE class
   * when a characteristic related to the mailbox has changed
   */
  @Test
  fun onCharacteristicChanged_MailboxCommandReceived() {
    Mockito.`when`(characteristic!!.uuid).thenReturn(CHARACTERISTIC_MAILBOX)
    Mockito.`when`(characteristic!!.value).thenReturn(MAILBOX_RAW_RESPONSE_5)
    PowerMockito.spy(CommandUtils::class.java)
    gattController!!.onCharacteristicChanged(gatt!!, characteristic!!)
    Mockito.verify(mbtBluetoothLE).stopWaitingOperation(MAILBOX_RESPONSE)
  }

  /**
   * Check that the Bluetooth Gatt controller
   * notifies the Bluetooth LE class
   * when a characteristic related to the EEG has changed
   */
  @Test
  fun onCharacteristicChanged_EegReceived() {
    Mockito.`when`(characteristic!!.uuid).thenReturn(CHARACTERISTIC_EEG)
    Mockito.`when`(characteristic!!.value).thenReturn(byteArrayOf(0, 1))
    gattController!!.onCharacteristicChanged(gatt!!, characteristic!!)
    Mockito.verify(mbtBluetoothLE).notifyNewDataAcquired(characteristic!!.value)
  }

  /**
   * Check that the Bluetooth Gatt controller
   * notifies the Bluetooth LE class
   * when a characteristic related to the EEG has changed
   */
  @Test
  fun onCharacteristicChanged_StatusReceived() {
    Mockito.`when`(characteristic!!.uuid).thenReturn(CHARACTERISTIC_STATUS)
    Mockito.`when`(characteristic!!.value).thenReturn(byteArrayOf(0, 1))
    gattController!!.onCharacteristicChanged(gatt!!, characteristic!!)
    Mockito.verify(mbtBluetoothLE).notifyNewHeadsetStatus(characteristic!!.value)
  }

  /**
   * Check that the Bluetooth Gatt controller
   * do not notify the Bluetooth LE class
   * when a unknown characteristic has changed
   */
  @Test
  fun onCharacteristicChanged_UnknownCharacteristicReceived() {
    Mockito.`when`(characteristic!!.uuid).thenReturn(UNKNOWN)
    gattController!!.onCharacteristicChanged(gatt!!, characteristic!!)
    Mockito.verifyZeroInteractions(mbtBluetoothLE)
  }
}