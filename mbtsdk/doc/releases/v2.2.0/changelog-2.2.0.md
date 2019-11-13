SDKv2.Android v2.2.0:
--------------------
**Release date** : 2019-11-13


# Release Notes :

- VPro headset features
- Dynamic sampling rate
- JSON Recording 
- Status deserialization bug fix
- Application app things removed



## Tâche 

* [[SDK-32](https://mybrain.atlassian.net/browse/SDK-32)] - Test connection & acquisition with VPro 
* [[SDK-122](https://mybrain.atlassian.net/browse/SDK-122)] - Pas d'informations retournées dans BluetoothStateListener 
* [[SDK-145](https://mybrain.atlassian.net/browse/SDK-145)] - Configure the sampling rate for a 500Hz acquisition 
* [[SDK-213](https://mybrain.atlassian.net/browse/SDK-213)] - Remove app things
* [[SDK-211](https://mybrain.atlassian.net/browse/SDK-211)] - Add flavorDimension in dev, staging, osc, master

## Bogue 

* [[SDK-146](https://mybrain.atlassian.net/browse/SDK-146)] - Quality checker is not deinit when stop stream is called 
* [[SDK-209](https://mybrain.atlassian.net/browse/SDK-209)] - Status bytes are not handled correctly, which causes triggers not to be at the correct value in recording 

## Amélioration 

* [[SDK-143](https://mybrain.atlassian.net/browse/SDK-143)] - Rename onBatteryChanged callback 

* [[SDK-210](https://mybrain.atlassian.net/browse/SDK-210)] - Dynamic import of the SDK sources 

* [[SDK-212](https://mybrain.atlassian.net/browse/SDK-212)] - Update lite version


## Nouvelle fonctionnalité 

* [[SDK-140](https://mybrain.atlassian.net/browse/SDK-140)] - Storage of EEG packets in JSON file