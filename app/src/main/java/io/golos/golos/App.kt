package io.golos.golos

import android.annotation.SuppressLint
import android.app.*
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.multidex.MultiDexApplication
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatDelegate
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import io.fabric.sdk.android.Fabric
import io.golos.golos.notifications.*
import io.golos.golos.repository.AppLifecycleRepository
import io.golos.golos.repository.LifeCycleEvent
import io.golos.golos.repository.Repository
import io.golos.golos.screens.main_activity.MainActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.getVectorAsBitmap
import timber.log.Timber


/**
 * Created by yuri on 30.10.17.
 */
class App : MultiDexApplication(), AppLifecycleRepository, Observer<GolosNotifications> {
    @SuppressLint("ApplySharedPref")
    private var aCreated = 0
    private var aStarted = 0
    private var aStopped = 0
    private var aDestroyed = 0
    private val mLiveData = MutableLiveData<LifeCycleEvent>()
    private val notifId = Integer.MAX_VALUE


    override fun onCreate() {
        context = this
        super.onCreate()
        if (isRoboUnitTest()) return

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Fabric.with(this, Crashlytics())
        mLiveData.value = LifeCycleEvent.APP_CREATE
        Repository.get.onAppCreate(this)

        val ce = if (resources.getBoolean(R.bool.isTablet)) CustomEvent("app launched on tablet")
        else CustomEvent("app launched on phone")
        Answers.getInstance().logCustom(ce)
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                aCreated++

            }

            override fun onActivityStarted(activity: Activity) {
                aStarted++
                if (aStopped == (aStarted - 1)) {
                    mLiveData.value = LifeCycleEvent.APP_IN_FOREGROUND
                    (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancelAll()
                }
            }

            override fun onActivityResumed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {

                aStopped++
                if (aStarted == aStopped) {
                    mLiveData.value = LifeCycleEvent.APP_IN_BACKGROUND
                    Repository.get.onAppStop()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                aDestroyed++
                if (aCreated == aDestroyed) {
                    mLiveData.value = LifeCycleEvent.APP_DESTROY
                    Repository.get.onAppDestroy()
                }
            }
        })
        Repository.get.userSettingsRepository.setVoteQuestionMade(false)
        mLiveData.value = LifeCycleEvent.APP_IN_BACKGROUND
        Repository.get.notificationsRepository.notifications.observeForever(this)
    }


    override fun onChanged(t: GolosNotifications?) {
        t?.let { notifications ->
            val notification = notifications.notifications.firstOrNull() ?: return

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val resultIntent = Intent(this, MainActivity::class.java)


            if (getLifeCycleLiveData().value == LifeCycleEvent.APP_IN_BACKGROUND) {
                val dismissIntent = Intent(this, NotificationsBroadCastReceiver::class.java)

                dismissIntent.putExtra(NOTIFICATION_KEY, notification.hashCode())
                resultIntent.putExtra(MainActivity.STARTED_FROM_NOTIFICATION, notification.hashCode())

                val builder = NotificationCompat
                        .Builder(this, getNotificationChannel())
                        .setContentIntent(PendingIntent.getActivities(this, 0, arrayOf(resultIntent), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setDeleteIntent(PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setSmallIcon(R.drawable.d_icon_18dp)
                            .color = ContextCompat.getColor(this, R.color.blue_dark)


                } else builder.setSmallIcon(R.drawable.ic_logo_white)

                if (Repository.get.userSettingsRepository.getNotificationsSettings().value?.playSoundWhenAppStopped == true) {
                    builder.setDefaults(Notification.DEFAULT_SOUND)
                } else {
                    builder.setDefaults(0).setSound(null)
                }

                (notification as? PostLinkable)?.getLink()?.let {
                    builder.setContentIntent(PendingIntent.getActivities(this, 0, arrayOf(resultIntent,
                            StoryActivity.getStartIntent(this, it.author, it.blog, it.permlink,
                                    FeedType.UNCLASSIFIED, null), dismissIntent), PendingIntent.FLAG_UPDATE_CURRENT))
                }
                val appearance = NotificationAppearanceManagerImpl.makeAppearance(notification)
                Handler(Looper.getMainLooper()).post {
                    builder.setContentText(appearance.body)
                            .setLargeIcon(getVectorAsBitmap(appearance.iconId))
                            .setContentTitle(appearance.title)
                    if (appearance.title != null) builder.setContentTitle(appearance.title)
                    notificationManager.notify(notifId, builder.build())
                }

            } else if (getLifeCycleLiveData().value != LifeCycleEvent.APP_IN_BACKGROUND) {
                notificationManager.cancelAll()
            }
            return Unit
        }
    }

    override fun getLifeCycleLiveData() = mLiveData

    private fun getNotificationChannel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = this.packageName
            val channelName = "Main Channel"
            val chan = NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH)
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            return channelId
        } else this.packageName
    }


    fun isRoboUnitTest(): Boolean {
        return "robolectric" == Build.FINGERPRINT
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