package com.mybraintech.sdk.sample

import android.app.Application
import timber.log.Timber

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        Timber.i("Timber DebugTree is planted.")
    }
}