package engine.clientevents;

import android.support.annotation.Keep;

@Keep
public final class BluetoothError extends BaseError {
    
    private static final String DOMAIN = "Bluetooth";
    
    private static int CODE_RANGE = 1100;

    public static String ERROR_LABEL = DOMAIN + " Error :";

    public static BluetoothError ERROR_NOT_CONNECTED                        = new BluetoothError(CODE_RANGE,"No connected headset.");
    public static BluetoothError ERROR_LOST_CONNECTION                      = new BluetoothError( CODE_RANGE+1,  "Lost Headset Connection.");
    public static BluetoothError ERROR_ALREADY_CONNECTED                    = new BluetoothError( CODE_RANGE+2,  "Headset already connected");
    public static BluetoothError ERROR_ALREADY_DISCONNECTED                 = new BluetoothError( CODE_RANGE+3,  "Headset already disconnected");
    public static BluetoothError ERROR_ALREADY_SCANNING                     = new BluetoothError( CODE_RANGE+4,  "Scanning already started.");
    public static BluetoothError ERROR_ALREADY_STOPPPED_SCANNING            = new BluetoothError( CODE_RANGE+5,  "Scanning already stopped.");
    public static BluetoothError ERROR_SCANNING_INTERRUPTED                 = new BluetoothError( CODE_RANGE+6,  "Bluetooth Scanning has been interrupted.");
    public static BluetoothError ERROR_SCANNING_TIMEOUT                     = new BluetoothError( CODE_RANGE+7,  "Bluetooth scanning could not be completed within the permitted time.");
    public static BluetoothError ERROR_CONNECTION_TIMEOUT                   = new BluetoothError( CODE_RANGE+8,  "Bluetooth connection could not be completed within the permitted time.");
    public static BluetoothError ERROR_DISCONNECTION_TIMEOUT                = new BluetoothError( CODE_RANGE+9,  "Bluetooth disconnection could not be completed within the permitted time.");
    public static BluetoothError ERROR_READING_DEVICE_INFO                  = new BluetoothError( CODE_RANGE+10,  "Reading Device Informations values has failed or could not be completed within the permitted time.");
    public static BluetoothError ERROR_TIMEOUT                              = new BluetoothError( CODE_RANGE+11,  "Bluetooth operation has failed or could not be completed within the permitted time.");
    public static BluetoothError ERROR_REFUSED_PAIRING                      = new BluetoothError( CODE_RANGE+12,  "User refused to pair the headset.");
    public static BluetoothError ERROR_UNPAIRED                             = new BluetoothError( CODE_RANGE+13,  "Unpaired headset.");
    public static BluetoothError ERROR_PAIRING_FAILED                       = new BluetoothError( CODE_RANGE+14,  "Pairing failed.");
    public static BluetoothError ERROR_CONFLICT_CONNECTED_HEADSET           = new BluetoothError( CODE_RANGE+15,  "Headsets names are not matching.");
    public static BluetoothError ERROR_A2DP_CONNECT_FAILED                  = new BluetoothError( CODE_RANGE+16,  "Bluetooth A2DP connection failed.");
    public static BluetoothError ERROR_BLE_CONNECT_FAILED                   = new BluetoothError( CODE_RANGE+17,  "Bluetooth Low Energy connection failed.");
    public static BluetoothError ERROR_CONNECT_FAILED                       = new BluetoothError( CODE_RANGE+18,  "Bluetooth connection failed.");
    public static BluetoothError ERROR_DISCOVERING_SERVICES                 = new BluetoothError( CODE_RANGE+19,  "Discovering services and characteristics has failed or could not be completed within the permitted time..");
    public static BluetoothError ERROR_ALREADY_CONNECTED_ANOTHER            = new BluetoothError( CODE_RANGE+20,  "Another device is already connected.");
    public static BluetoothError ERROR_ALREADY_CONNECTED_ELSEWHERE          = new BluetoothError( CODE_RANGE+21,  "Audio is already connected to another device.");
    public static BluetoothError ERROR_ALREADY_CONNECTED_JACK               = new BluetoothError( CODE_RANGE+22,  "Jack cable already connected.");
    public static BluetoothError ERROR_NOT_SUPPORTED                        = new BluetoothError( CODE_RANGE+23,  "Bluetooth Low Energy not supported for this mobile device (incompatible Android OS version).");
    public static BluetoothError ERROR_SCANNING_NOT_STARTED                 = new BluetoothError( CODE_RANGE+24,  "Bluetooth Scanning could not be started.");
    public static BluetoothError ERROR_SCANNING_FAILED                      = new BluetoothError( CODE_RANGE+25,  "Bluetooth Scanning failed.");
    public static BluetoothError ERROR_CONNECTION_INTERRUPTED               = new BluetoothError( CODE_RANGE+26,  "Bluetooth Connection has been interrupted.");
    public static BluetoothError ERROR_SETTINGS_INTERFACE_ACTION            = new BluetoothError( CODE_RANGE+27,  "Bluetooth Audio Connection with an unpaired headset is not supported on your mobile. Please read the User Guide to connect Audio in a different way.");
    public static BluetoothError ERROR_WRITE_CHARACTERISTIC_OPERATION       = new BluetoothError( CODE_RANGE+28,  "Bluetooth characteristic write operation failed.");
    public static BluetoothError ERROR_REQUEST_OPERATION                    = new BluetoothError( CODE_RANGE+28,  "Bluetooth request operation failed.");
    public static BluetoothError ERROR_VALIDITY_CHARACTERISTIC_OPERATION    = new BluetoothError( CODE_RANGE+29,  "Error: failed to check service and characteristic validity.");


    private BluetoothError(int code, String exception){
        super(DOMAIN, code, exception);
    }
}

