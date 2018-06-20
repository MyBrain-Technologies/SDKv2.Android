package core.bluetooth;

/**
 * The different Bluetooth protocols that are supported in this SDK.
 */
public enum BtProtocol {
    /**
     * Bluetooth Low Energy is supported in this SDK.
     */
    BLUETOOTH_LE,

    /**
     * Bluetooth SPP is supported in this SDK
     */
    BLUETOOTH_SPP,

    /**
     * Bluetooth A2DP is slightly supported in this SDK. Only connection/disconnection fucntionnalities are
     * enabled. Scanning can be done using discovery scan.
     */
    BLUETOOTH_A2DP
}
