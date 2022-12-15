package com.mybraintech.sdk.core.acquisition

import java.util.concurrent.ThreadFactory

object AcquisierThreadFactory : ThreadFactory {

    override fun newThread(r: Runnable?): Thread {
        return Thread(r)
    }
}