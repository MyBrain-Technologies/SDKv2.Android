package core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

class BluetoothInitializer{
    /**
     * As headset transmits its sensed data to the SDK using Bluetooth,
     * the mobile device Bluetooth must be currently enabled and ready for use.
     * The Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isBluetoothDisabled() method.
     * @return false is Bluetooth is ON /enabled,
     * or true is Bluetooth is OFF / disabled.
     */
    fun isBluetoothDisabled(): Boolean {
        return !BluetoothAdapter.getDefaultAdapter().isEnabled
    }


    /**
     * As the Bluetooth scan requires access to the mobile device Location,
     * the Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isLocationDisabledOrNotGranted() method.
     * @return false is Location is ON /enabled and Location permission is granted,
     * or true is Location is OFF / disabled, and/or Location permission is not granted.
     */
    fun isLocationNotGranted(context: Context): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
    }
    /**
     * As the Bluetooth scan requires access to the mobile device Location,
     * the Bluetooth Manager must always check this prerequisite before starting any connection operation by calling the isLocationDisabledOrNotGranted() method.
     * @return false is Location is ON /enabled and Location permission is granted,
     * or true is Location is OFF / disabled, and/or Location permission is not granted.
     */
    fun isLocationDisabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
    }
}
