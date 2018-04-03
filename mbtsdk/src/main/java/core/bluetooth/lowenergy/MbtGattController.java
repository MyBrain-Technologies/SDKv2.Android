package core.bluetooth.lowenergy;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import core.bluetooth.BtState;
import core.bluetooth.MbtBluetooth;

final class MbtGattController extends BluetoothGattCallback {

    private BluetoothGatt gatt = null;
    private BluetoothGattService mainService = null;
    private BluetoothGattService deviceInfoService = null;
    private BluetoothGattCharacteristic measurement = null ;
    private BluetoothGattCharacteristic headsetStatus = null ;
    private BluetoothGattCharacteristic mailBox = null ;
    private BluetoothGattCharacteristic oadPacketsCharac = null;
    private BluetoothGattCharacteristic battery = null ;
    private BluetoothGattCharacteristic fwVersion = null;
    private BluetoothGattCharacteristic hwVersion = null;
    private BluetoothGattCharacteristic serialNumber = null;

    private MbtBluetooth bluetoothController;

    public MbtGattController(MbtBluetooth bluetoothController) {
        super();
        this.bluetoothController = bluetoothController;
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyRead(gatt, txPhy, rxPhy, status);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String msg = "Connection state change : ";
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED:
                //gatt.requestMtu(MAX_MTU);
                gatt.discoverServices();
                this.bluetoothController.notifyStateChanged(BtState.CONNECTED);
                msg += "STATE_CONNECTED and now discovering services...";
                break;
            case BluetoothGatt.STATE_CONNECTING:
                this.bluetoothController.notifyStateChanged(BtState.CONNECTING);
                msg += "STATE_CONNECTING";
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                // This if is necessary because we might have disconnect after something went wrong while connecting
                this.gatt.close();
                this.gatt = null;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.bluetoothController.notifyStateChanged(BtState.DISCONNECTED);
                msg += "STATE_DISCONNECTED";
                break;
            case BluetoothGatt.STATE_DISCONNECTING:;
                msg += "STATE_DISCONNECTING";
                this.bluetoothController.notifyStateChanged(BtState.DISCONNECTING);
                break;
            default:
                this.bluetoothController.notifyStateChanged(BtState.INTERNAL_FAILURE);
                this.gatt.close();
                this.gatt = null;
                msg += "Unknown value " + newState;
        }
        Log.d("", msg);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
    }
}