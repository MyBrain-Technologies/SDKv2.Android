package core.bluetooth.requests

import androidx.annotation.IntRange
import command.CommandInterface
import core.bluetooth.lowenergy.EnumIndus5Command
import engine.clientevents.BaseError
import java.lang.UnsupportedOperationException

class Indus5CommandRequest(private val indus5Command: EnumIndus5Command) : BluetoothRequests() {

    val command: CommandInterface.MbtCommand<BaseError> =
            when (indus5Command) {
                EnumIndus5Command.MBX_TRANSMIT_MTU_SIZE -> {
                    Indus5ChangeMTU()
                }
                EnumIndus5Command.MBX_START_EEG_ACQUISITION -> {
                    Indus5StartStream()
                }
                else -> {
                    throw UnsupportedOperationException("only supported MBX_TRANSMIT_MTU_SIZE")
                }
            }

    class Indus5ChangeMTU(@IntRange(from = 23, to = 47) val size: Int = 47) : CommandInterface.MbtCommand<BaseError>() {

        override fun serialize(): Any {
            val result = EnumIndus5Command.MBX_TRANSMIT_MTU_SIZE.bytes.toMutableList()
            result.add(size.toByte())
            return result.toByteArray()
        }

        override val isValid: Boolean
            get() = true

        override val invalidityError: String?
            get() = null
    }

    class Indus5StartStream() : CommandInterface.MbtCommand<BaseError>() {

        override fun serialize(): Any {
            return "null"
        }

        override val isValid: Boolean
            get() = true

        override val invalidityError: String?
            get() = null
    }
}