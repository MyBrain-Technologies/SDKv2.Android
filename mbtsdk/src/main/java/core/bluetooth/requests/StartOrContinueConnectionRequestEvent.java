package core.bluetooth.requests;


import core.bluetooth.BluetoothContext;
import features.MbtDeviceType;

/**
 * An event class when a connection request is being sent
 */
public class StartOrContinueConnectionRequestEvent extends BluetoothRequests {

  private boolean isClientUserRequest;
  private BluetoothContext context;

  public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest, BluetoothContext context) {
    this.isClientUserRequest = isClientUserRequest;
    this.context = context; }

  public boolean isClientUserRequest() {
    return isClientUserRequest;
  }
  public BluetoothContext getContext() {
    return context;
  }
}