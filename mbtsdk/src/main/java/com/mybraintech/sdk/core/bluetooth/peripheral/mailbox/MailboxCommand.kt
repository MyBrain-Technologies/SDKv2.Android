package com.mybraintech.sdk.core.bluetooth.peripheral.mailbox

import com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer.characteristics.PreIndus5Characteristic
import java.util.*

// TODO: Or use sealed class?
enum class MailboxCommand(val raw: Byte) {
  // Product name configuration request
  SetProductName(0x02.toByte()), // Only melomind

  /// Used by app to request an OTA update (provides software major and minor
  ///  in payload)
  StartOTATFX(0x03.toByte()),

  /// Notifies app of a lead off modification
//  leadOffEvent(0x04 // Only melomind. Not used. For reference.

  /// Notifies appli that we switched to OTA mode
  OtaModeEvent(0x05.toByte()),

  /// Notifies appli that we request a packet Idx reset
  OtaIndexResetEvent(0x06.toByte()),

  /// Notifies appli with the status of the OTA transfert.
  OtaStatusEvent(0x07.toByte()),

  /// allows to retrieve to system global status
  SystemGetStatus(0x08.toByte()),

  /// trigger a reboot event at disconnection
  SystemRebootEvent(0x09.toByte()), // 0x09, 0x29, 0x08

  /// Set the melomind serial nb
  SetSerialNumber(0x0A.toByte()),

  SetA2dpName(0x1A.toByte()), // Set QRCode

  /// allows to hotswap the filters' parameters
  SetNotchFilter(0x0B.toByte()),

  /// Set the signal bandwidth by changing the embedded bandpass filter
  SetBandpassFilter(0x0C.toByte()),

  /// Set the eeg signal amplifier gain
  SetAmplifierSignalGain(0x0D.toByte()),

  /// Get the current configuration of the Notch filter, the bandpass filter,
  /// and the amplifier gain.
  GetEEGConfig(0x0E.toByte()),

  /// Enable or disable the p300 functionnality of the melomind.
  ToggleP300(0x0F.toByte()),

  EnableDCOffset(0x10.toByte()),

  A2dpConnection(0x11.toByte()),

  /////////////
  BatteryLevel(0x20.toByte()),

  SerialNumber(0x22.toByte()),

  DeviceId(0x23.toByte()),

  /// Response: 0 failure, 1 success
  StartEeg(0x24.toByte()),

  // Response: 0 failure, 1 success
  StopEeg(0x25.toByte()),

  Reset(0x26.toByte()),

  FirmewareVersion(0x27.toByte()),

  HardwareVersion(0x28.toByte()),

  MtuSize(0x29.toByte()),

  GetFilterConfigurationType(0x30.toByte()),

  SetFilterConfigurationType(0x31.toByte()),

  SetAudioconfig(0x32.toByte()),

  StartImsAcquisition(0x33.toByte()),

  StopImsAcquisition(0x34.toByte()),

  StartPpgAcquisition(0x35.toByte()),

  StopPpgAcquisition(0x36.toByte()),

  StartTemperatureAcquisition(0x37.toByte()),

  StopTemperatureAcquisition(0x38.toByte()),

  SetImsConfiguration(0x39.toByte()),

  GetSensorStatus(0x41.toByte()),

  SetPpgConfiguration(0x42.toByte()),

  ImsDataFrameEvent(0x50.toByte()),

  PpgDataFrameEvent(0x60.toByte()),

  EegDataFrameEvent(0x40.toByte()),

  UnknownEvent(0xFF.toByte());

  companion object {
    private val rawToEnum = mapOf(
      MailboxCommand.SetProductName.raw to MailboxCommand.SetProductName,
      MailboxCommand.StartOTATFX.raw to MailboxCommand.StartOTATFX,
      MailboxCommand.OtaModeEvent.raw to MailboxCommand.OtaModeEvent,
      MailboxCommand.OtaIndexResetEvent to MailboxCommand.OtaIndexResetEvent,
      MailboxCommand.OtaStatusEvent to MailboxCommand.OtaStatusEvent,
      MailboxCommand.SystemGetStatus to MailboxCommand.SystemGetStatus,
      MailboxCommand.SystemRebootEvent to MailboxCommand.SystemRebootEvent,
      MailboxCommand.SetSerialNumber to MailboxCommand.SetSerialNumber,
      MailboxCommand.SetA2dpName to MailboxCommand.SetA2dpName,
      MailboxCommand.SetNotchFilter to MailboxCommand.SetNotchFilter,
      MailboxCommand.SetBandpassFilter to MailboxCommand.SetBandpassFilter,
      MailboxCommand.SetAmplifierSignalGain to MailboxCommand.SetAmplifierSignalGain,
      MailboxCommand.GetEEGConfig to MailboxCommand.GetEEGConfig,
      MailboxCommand.ToggleP300 to MailboxCommand.ToggleP300,
      MailboxCommand.EnableDCOffset to MailboxCommand.EnableDCOffset,
      MailboxCommand.A2dpConnection to MailboxCommand.A2dpConnection,
      MailboxCommand.BatteryLevel to MailboxCommand.BatteryLevel,
      MailboxCommand.SerialNumber to MailboxCommand.SerialNumber,
      MailboxCommand.DeviceId to MailboxCommand.DeviceId,
      MailboxCommand.StartEeg to MailboxCommand.StartEeg,
      MailboxCommand.StopEeg to MailboxCommand.StopEeg,
      MailboxCommand.Reset to MailboxCommand.Reset,
      MailboxCommand.FirmewareVersion to MailboxCommand.FirmewareVersion,
      MailboxCommand.HardwareVersion to MailboxCommand.HardwareVersion,
      MailboxCommand.MtuSize to MailboxCommand.MtuSize,
      MailboxCommand.GetFilterConfigurationType to MailboxCommand.GetFilterConfigurationType,
      MailboxCommand.SetFilterConfigurationType to MailboxCommand.SetFilterConfigurationType,
      MailboxCommand.SetAudioconfig to MailboxCommand.SetAudioconfig,
      MailboxCommand.StartImsAcquisition to MailboxCommand.StartImsAcquisition,
      MailboxCommand.StopImsAcquisition to MailboxCommand.StopImsAcquisition,
      MailboxCommand.StartPpgAcquisition to MailboxCommand.StartPpgAcquisition,
      MailboxCommand.StopPpgAcquisition to MailboxCommand.StopPpgAcquisition,
      MailboxCommand.StartTemperatureAcquisition to MailboxCommand.StartTemperatureAcquisition,
      MailboxCommand.SetImsConfiguration to MailboxCommand.SetImsConfiguration,
      MailboxCommand.GetSensorStatus to MailboxCommand.GetSensorStatus,
      MailboxCommand.SetPpgConfiguration to MailboxCommand.SetPpgConfiguration,
      MailboxCommand.ImsDataFrameEvent to MailboxCommand.ImsDataFrameEvent,
      MailboxCommand.PpgDataFrameEvent to MailboxCommand.PpgDataFrameEvent,
      MailboxCommand.EegDataFrameEvent to MailboxCommand.EegDataFrameEvent,
      MailboxCommand.UnknownEvent to MailboxCommand.UnknownEvent
    )

    fun ofRaw(raw: UUID): MailboxCommand? {
      return rawToEnum[raw]
    }
  }
}