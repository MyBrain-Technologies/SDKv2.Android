package core.bluetooth

import android.content.Context
import androidx.annotation.VisibleForTesting
import core.device.model.MelomindsQRDataBase
import features.MbtDeviceType

class BluetoothContext (val context: Context,
                        val deviceTypeRequested: MbtDeviceType,
                        val connectAudio: Boolean,
                        var deviceNameRequested: String?,
                        var deviceQrCodeRequested: String?,
                        val mtu: Int){

    init {
        val qrCode = deviceQrCodeRequested
        if (qrCode != null) {

            //if a QR code has been specified but no device name
            if (deviceNameRequested == null) { //retrieve the BLE name from the QR code database
                deviceNameRequested = MelomindsQRDataBase(context, true)[deviceQrCodeRequested]
            }
            //if QR code contains only 9 digits
            if (qrCode.startsWith(MelomindsQRDataBase.QR_PREFIX)
                    && qrCode.length == MelomindsQRDataBase.QR_LENGTH - 1) {
                deviceQrCodeRequested += MelomindsQRDataBase.QR_SUFFIX //homogenization with the 10 digits QR code by adding a dot at the end
            }

            //if a device name has been specified but no QR code
        } else if (deviceNameRequested != null && (deviceTypeRequested == MbtDeviceType.MELOMIND)) {
            deviceQrCodeRequested = MelomindsQRDataBase(context, false).get(deviceNameRequested) //retrieve the QR code from BLE name using QR code database
        }
    }

}
