# USER GUIDE

Modified date: 2021-08-11 
Version: 0.1

# How to install and use the SDK in your project

## 1. Requirement & How to install 

------

The SDK is compiled with Android API version 28 and is compatible with Android API version 22 and higher.

Import the SDK file in the *build.gradle* of your application module (for example: *app/build.gradle*)

```
implementation files('libs/lib_full_release_2.2.16.qplus.aar')
```

## 2. How to use the SDK (example in Kotlin):

------

Add the permissions in the *AndroidManifest.xml*

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

The first step is to initialize the SDK 

```
MbtClient.init(context)
```

To scan QPlus devices:

```
val mbtClient = MbtClient.getClientInstance()
val callback: ConnectionStateListener
connectionConfig = ConnectionConfig.Builder(callback)
        .createForDevice(MbtDeviceType.MELOMIND_Q_PLUS)
mbtClient.connectBluetooth(connectionConfig)
```

To start EEG streaming:

```
val eegListener : EegListener
val streamConfig = StreamConfig.Builder(eegListener)
        .useQualities()
        .createForDevice(MbtDeviceType.MELOMIND_Q_PLUS)
mbtClient.startStreaming(streamConfig)
```

Here is the functions of EegListener interface:

```
//receives EEGPacket from the SDK
fun onNewPackets(eegPackets: MbtEEGPacket)

//callback for stream connection state
fun onNewStreamState(streamState: StreamState)
```

MbtEEGPacket contains data samples of one second. Here is some functions of MbtEEGPacket class:

```
//get eeg signals
fun getChannelsData(): ArrayList<ArrayList<Float>>

//get eeg qualitie values calculated by the SDK
fun getQualities(): ArrayList<Float>

//get trigger(status) data
fun getStatusData(): ArrayList<Float>
```

To stop EEG streaming:

```
mbtClient.stopStream()
```

To disconnect the connected device:

```
mbtClient.disconnectBluetooth()
```

## 3. QPlus Main functions:

In MbtClient class, here is the available functions for QPlus device:

```
//init the SDK
fun init(context: Context): MbtClient

//get the MbtClient object to communicate with the SDK
fun getClientInstance(): MbtClient
 
//connect to QPlus device by BLE
fun connectBluetooth(config: ConnectionConfig)

//disconnect the current connected device
fun disconnectBluetooth()

//read the battery level of connected device
fun readBattery(listener: DeviceBatteryListener)

//start to mesure eeg signals
fun startStream(streamConfig: StreamConfig)

//stop mesuring eeg
fun stopStream()

//start recording eeg
fun startRecord(context: Context)
    
//stop recording eeg
fun stopRecord(recordConfig: RecordConfig)

//get connected device firmware
fun getFirmwareVersion(listener: FirmwareListener)
``` 

## 3. Class and package name:


```
config.ConnectionConfig
config.StreamConfig
core.eeg.storage.MbtEEGPacket
engine.clientevents.ConnectionStateListener
engine.clientevents.EegListener
engine.MbtClient
features.MbtDeviceType
```