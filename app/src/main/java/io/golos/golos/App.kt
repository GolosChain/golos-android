package io.golos.golos

import android.content.Context
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import timber.log.Timber

/**
 * Created by yuri on 30.10.17.
 */
class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
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