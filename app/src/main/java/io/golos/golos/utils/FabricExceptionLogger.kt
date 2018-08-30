package io.golos.golos.utils

import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import io.golos.golos.App

object FabricExceptionLogger : ExceptionLogger {
    override fun log(t: Throwable) {
        try {
            if (!Fabric.isInitialized()) {
                Fabric.with(App.context, Crashlytics())
            }
            Crashlytics.logException(t)
            t.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}