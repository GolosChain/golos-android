package io.golos.golos

import android.content.Context
import android.support.multidex.MultiDexApplication
import eu.bittrade.libs.steemj.enums.PrivateKeyType
import io.golos.golos.repository.persistence.Persister
import timber.log.Timber

/**
 * Created by yuri on 30.10.17.
 */
class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
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