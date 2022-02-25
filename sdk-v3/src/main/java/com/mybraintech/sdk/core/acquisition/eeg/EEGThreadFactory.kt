package com.mybraintech.sdk.core.acquisition.eeg

import java.util.concurrent.ThreadFactory

object EEGThreadFactory : ThreadFactory {

    override fun newThread(r: Runnable?): Thread {
        return Thread(r)
    }
}