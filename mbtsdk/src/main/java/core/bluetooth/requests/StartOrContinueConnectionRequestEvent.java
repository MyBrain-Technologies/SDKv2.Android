package core.bluetooth.requests;


import core.bluetooth.BluetoothContext;
import features.MbtDeviceType;

/**
 * An event class when a connection request is being sent
 */
public class StartOrContinueConnectionRequestEvent extends BluetoothRequests {

  private boolean isClientUserRequest;
  private String nameOfDeviceRequested;
  private String qrCodeOfDeviceRequested;
  private MbtDeviceType typeOfDeviceRequested;
  private int mtu;
  private boolean connectAudioIfDeviceCompatible = false;

  public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest, String nameOfDeviceRequested, String qrCodeOfDeviceRequested, MbtDeviceType typeOfDeviceRequested, int mtu, boolean connectAudioIfDeviceCompatible) {
    this.isClientUserRequest = isClientUserRequest;
    this.nameOfDeviceRequested = nameOfDeviceRequested;
    this.qrCodeOfDeviceRequested = qrCodeOfDeviceRequested;
    this.typeOfDeviceRequested = typeOfDeviceRequested;
    this.mtu = mtu;
    this.connectAudioIfDeviceCompatible = typeOfDeviceRequested.equals(MbtDeviceType.MELOMIND) && connectAudioIfDeviceCompatible;
  }

  public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest, String nameOfDeviceRequested, String qrCodeOfDeviceRequested, MbtDeviceType typeOfDeviceRequested, int mtu) {
    this.isClientUserRequest = isClientUserRequest;
    this.nameOfDeviceRequested = nameOfDeviceRequested;
    this.qrCodeOfDeviceRequested = qrCodeOfDeviceRequested;
    this.typeOfDeviceRequested = typeOfDeviceRequested;
    this.mtu = mtu;
    this.connectAudioIfDeviceCompatible = false;
  }

  public StartOrContinueConnectionRequestEvent(boolean isClientUserRequest, BluetoothContext context) {
    this.isClientUserRequest = isClientUserRequest;
    this.nameOfDeviceRequested = context.getDeviceNameRequested();
    this.qrCodeOfDeviceRequested = context.getDeviceQrCodeRequested();
    this.typeOfDeviceRequested = context.getDeviceTypeRequested();
    this.mtu = context.getMtu();
    this.connectAudioIfDeviceCompatible = typeOfDeviceRequested.equals(MbtDeviceType.MELOMIND) && context.getConnectAudio();
  }

  public boolean isClientUserRequest() {
    return isClientUserRequest;
  }
  public boolean shouldReset() {
    return isClientUserRequest;
  }

  public String getNameOfDeviceRequested() {
    return nameOfDeviceRequested;
  }

  public MbtDeviceType getTypeOfDeviceRequested() {
    return typeOfDeviceRequested;
  }

  public String getQrCodeOfDeviceRequested() {
    return qrCodeOfDeviceRequested;
  }

  public int getMtu() {
    return mtu;
  }

  public boolean connectAudioIfDeviceCompatible() {
    return connectAudioIfDeviceCompatible;
  }
}