package com.simodev.gpspositionsaver

import android.app.Application

class GpsSaverApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
