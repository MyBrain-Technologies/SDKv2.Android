package com.mybraintech.sdk.core.listener

interface AudioNameListener {
    fun onAudioNameChanged(newAudioName: String)
    fun onAudioNameError(errorMessage: String)
}