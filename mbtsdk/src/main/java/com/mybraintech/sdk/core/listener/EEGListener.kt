package com.mybraintech.sdk.core.listener

interface EEGListener {
    fun onEegPackage()
    fun onEegError()
}