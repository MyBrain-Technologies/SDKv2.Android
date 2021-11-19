package com.mybraintech.sdk.core.bluetooth.attributes.characteristiccontainer

interface IMBTCharacteristic: IMBTAttribute {
  var readCharacteristics: Array<IMBTCharacteristic>
  var writeCharacteristics: Array<IMBTCharacteristic>
}