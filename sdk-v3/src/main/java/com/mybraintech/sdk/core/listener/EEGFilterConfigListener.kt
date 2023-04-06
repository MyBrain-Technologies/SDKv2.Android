package com.mybraintech.sdk.core.listener

import com.mybraintech.sdk.core.model.EnumEEGFilterConfig

interface EEGFilterConfigListener {
    fun onEEGFilterConfig(config: EnumEEGFilterConfig)
    fun onEEGFilterConfigError(errorMsg: String)
}