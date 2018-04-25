package io.golos.golos

import android.annotation.SuppressLint
import android.app.*
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.multidex.MultiDexApplication
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import io.fabric.sdk.android.Fabric
import io.golos.golos.notifications.NOTIFICATION_KEY
import io.golos.golos.notifications.NotificationsBroadCastReceiver
import io.golos.golos.repository.AppLifecycleRepository
import io.golos.golos.repository.LifeCycleEvent
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.*
import io.golos.golos.screens.main_activity.MainActivity
import io.golos.golos.utils.ImageUriResolver
import io.golos.golos.utils.getVectorAsBitmap
import io.golos.golos.utils.siteUrl
import io.golos.golos.utils.toHtml
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

                val builder = NotificationCompat
                        .Builder(this, getString(R.string.notifications_channel_main))
                        .setSmallIcon(R.drawable.d_icon_18dp)
                        .setContentIntent(PendingIntent.getActivities(this, 0, arrayOf(resultIntent), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setDeleteIntent(PendingIntent.getBroadcast(this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .setAutoCancel(true)
                if (Repository.get.userSettingsRepository.getNotificationsSettings().value?.playSoundWhenAppStopped == true) {
                    builder.setDefaults(Notification.DEFAULT_SOUND)
                } else {
                    builder.setDefaults(0).setSound(null)
                }
                var isBuilt = false

                when (notification) {
                    is GolosUpVoteNotification -> {
                        val voteNotification = notification.voteNotification
                        builder.setContentTitle(getString(R.string.new_upvote))
                        if (voteNotification.count > 1) {

                            builder
                                    .setLargeIcon(getVectorAsBitmap(R.drawable.ic_double_arrows_on_blue))
                                    .setContentText(getString(R.string.users_voted_on_post,
                                            "${siteUrl}${voteNotification.parentUrl}",
                                            voteNotification.count.toString(),
                                            resources.getQuantityString(R.plurals.times, voteNotification.count)).toHtml())

                        } else if (voteNotification.count == 1) {

                            val text = getString(R.string.user_voted_on_post,
                                    "<b>${voteNotification.from.name.capitalize()}</b>", "${siteUrl}${voteNotification.parentUrl}").toHtml()

                            if (voteNotification.from.avatar == null) builder
                                    .setContentText(text)
                                    .setLargeIcon(getVectorAsBitmap(R.drawable.ic_person_gray_32dp))
                            else {
                                isBuilt = true
                                loadImageThenShowNotification(builder, text, voteNotification.from.avatar)
                            }
                        }
                    }
                    is GolosDownVoteNotification -> {
                        builder.setContentTitle(getString(R.string.new_flag))
                        val voteNotification = notification.voteNotification

                        if (voteNotification.count > 1) {

                            builder.setLargeIcon(getVectorAsBitmap(R.drawable.ic_dislike_on_blue))

                            builder.setContentText(getString(R.string.users_downvoted_on_post,
                                    "${siteUrl}${voteNotification.parentUrl}",
                                    Math.abs(voteNotification.count).toString(),
                                    resources.getQuantityString(R.plurals.users, Math.abs(voteNotification.count))).toHtml())
                        } else if (voteNotification.count == 1) {
                            val text = getString(R.string.user_downvoted_on_post,
                                    "<b>${voteNotification.from.name.capitalize()}</b>", "${siteUrl}${voteNotification.parentUrl}").toHtml()

                            if (voteNotification.from.avatar == null) builder.setContentText(text)
                                    .setLargeIcon(getVectorAsBitmap(R.drawable.ic_person_gray_32dp))
                            else {
                                isBuilt = true
                                loadImageThenShowNotification(builder, text, voteNotification.from.avatar)
                            }

                        }
                    }
                    is GolosTransferNotification -> {
                        builder.setContentTitle(getString(R.string.new_transfer))
                        val transferNotification = notification.transferNotification
                        val text = getString(R.string.user_transferred_you,
                                "<b>${transferNotification.from.name.capitalize()}</b>", transferNotification.amount).toHtml()

                        if (transferNotification.from.avatar == null) builder.setContentText(text)
                                .setLargeIcon(getVectorAsBitmap(R.drawable.ic_person_gray_32dp))
                        else {
                            isBuilt = true
                            loadImageThenShowNotification(builder, text, transferNotification.from.avatar)
                        }
                    }
                    is GolosCommentNotification -> {
                        builder.setContentTitle(getString(R.string.new_comment))
                        val commentNotification = notification.commentNotification
                        val textId = if (notification.isCommentToPost()) R.string.user_answered_on_post else R.string.user_answered_on_comment
                        val text = getString(textId,
                                "<b>${commentNotification.author.name.capitalize()}</b>",
                                "$siteUrl${commentNotification.parentUrl}").toHtml()

                        if (commentNotification.author.avatar == null) builder.setContentText(text)
                                .setLargeIcon(getVectorAsBitmap(R.drawable.ic_person_gray_32dp))
                        else {
                            isBuilt = true
                            loadImageThenShowNotification(builder, text, commentNotification.author.avatar)
                        }
                    }

                }
                if (!isBuilt) {
                    notificationManager.notify(notifId, builder.build())
                }
            } else if (getLifeCycleLiveData().value != LifeCycleEvent.APP_IN_BACKGROUND) {
                notificationManager.cancelAll()
            }
            return Unit
        }
    }

    override fun getLifeCycleLiveData() = mLiveData

    private fun loadImageThenShowNotification(builder: NotificationCompat.Builder, text: CharSequence, avatarPath: String) {

        val wantedSize = resources.getDimension(R.dimen.notification_big_icon_size).toInt()
        Glide.with(this)
                .asBitmap()
                .load(ImageUriResolver
                        .resolveImageWithSize(avatarPath, wantedwidth = wantedSize))

                .apply(RequestOptions.circleCropTransform())
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Bitmap>?, p3: Boolean): Boolean {
                        return true
                    }


                    override fun onResourceReady(p0: Bitmap?, p1: Any?, p2: Target<Bitmap>?, p3: DataSource?, p4: Boolean): Boolean {
                        Handler(Looper.getMainLooper()).post {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            builder
                                    .setLargeIcon(p0)
                                    .setContentText(text)
                            notificationManager.notify(notifId, builder.build())
                        }

                        return true
                    }
                })
                .submit(wantedSize, wantedSize)
    }

    companion object get {
        lateinit var context: Context
        val isMocked = false
        fun isAppOnline(): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return cm?.activeNetworkInfo != null && cm.activeNetworkInfo.isConnected
        }

        fun getLifeCycleRespository(): AppLifecycleRepository = this as App
    }
}