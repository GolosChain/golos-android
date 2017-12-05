package io.golos.golos

import android.content.Context
import android.net.ConnectivityManager
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import java.util.concurrent.Executors

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

        Fabric.with(this, Crashlytics())
    }

    companion object get {
        lateinit var context: Context
        val isMocked = false
        fun isAppOnline(): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return cm?.activeNetworkInfo != null && cm.activeNetworkInfo.isConnected
        }

        val computationExecutor = Executors.newSingleThreadExecutor()
    }
}