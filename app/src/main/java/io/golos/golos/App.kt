package io.golos.golos

import android.app.Application
import android.content.Context

import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Created by yuri on 30.10.17.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        //   MultiDex.install(this)
        context = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    companion object get {
        lateinit var context: Context
        val isMocked = false
    }
}