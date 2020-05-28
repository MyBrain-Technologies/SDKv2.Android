package core.bluetooth;

/**
 * The different Bluetooth protocols that are supported in this SDK.
 */
public enum BluetoothProtocol {
    /**
     * Bluetooth Low Energy is supported in this SDK.
     */
    LOW_ENERGY,

    /**
     * Bluetooth SPP is supported in this SDK
     */
    SPP,

    /**
     * Bluetooth A2DP is slightly supported in this SDK. Only connection/disconnection fucntionnalities are
     * enabled. Scanning can be done using discovery scan.
     */
    A2DP
}
