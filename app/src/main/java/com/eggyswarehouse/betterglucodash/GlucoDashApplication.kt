package com.eggyswarehouse.betterglucodash

import android.app.Application
import com.eggyswarehouse.betterglucodash.di.AppContainer

class GlucoDashApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
