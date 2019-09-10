SDKv2.Android v2.0.5:
--------------------
**Release date** : 2019-06-28


# Release Notes :

- Mailbox commands refactored 
- MTU change integrated in the Bluetooth connection process 


## Bug

- [[SDK-123]https://mybrain.atlassian.net/browse/SDK-123] - System status mailbox command do not return any value
- [[SDK-128]https://mybrain.atlassian.net/browse/SDK-128] - Connected audio device is not detected if the audio connection is performed after BLE connection
- [[SDK-124]https://mybrain.atlassian.net/browse/SDK-124] - Improve the mailbox command management to avoid timeout bug for command with no response

## New Feature

- [[SDK-125]https://mybrain.atlassian.net/browse/SDK-125] - Add a feature to get the connected A2DP device address and name
- 
## Task

- [[SDK-120]https://mybrain.atlassian.net/browse/SDK-120] - Differenciate the serial number and external name chnage received in the gatt controller

## Improvement

- [[SDK-129]https://mybrain.atlassian.net/browse/SDK-129] - Improve the MTU command management
- [[SDK-119]https://mybrain.atlassian.net/browse/SDK-119] - Use the current build variant in the gradle generation script

