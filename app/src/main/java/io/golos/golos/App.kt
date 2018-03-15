package io.golos.golos

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import io.fabric.sdk.android.Fabric
import io.golos.golos.repository.Repository
import timber.log.Timber


/**
 * Created by yuri on 30.10.17.
 */
class App : MultiDexApplication() {
    @SuppressLint("ApplySharedPref")
    private var aCreated = 0
    private var aResumed = 0
    private var aStopped = 0
    private var aDestroyed = 0


    override fun onCreate() {
        context = this
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Fabric.with(this, Crashlytics())
        Repository.get.onAppCreate(this)

        val ce = if (resources.getBoolean(R.bool.isTablet)) CustomEvent("app launched on tablet")
        else CustomEvent("app launched on phone")
        Answers.getInstance().logCustom(ce)
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                aCreated++
            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {
                aResumed++
            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {
                aStopped++
                if (aResumed == aStopped) Repository.get.onAppStop()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                aDestroyed++
                if (aCreated == aDestroyed) Repository.get.onAppDestroy()
            }
        })
        Repository.get.userSettingsRepository.setVoteQuestionMade(false)
    }

    companion object get {
        lateinit var context: Context
        val isMocked = false
        fun isAppOnline(): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return cm?.activeNetworkInfo != null && cm.activeNetworkInfo.isConnected
        }
    }
}