package io.golos.golos.notifications

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.model.DeviceIdProvider
import io.golos.golos.repository.model.PreparingState
import io.golos.golos.repository.services.model.Event
import io.golos.golos.repository.services.GolosPushService
import io.golos.golos.repository.services.ServiceLogoutListener
import timber.log.Timber

interface PushNotificationsRepository {
    val notifications: LiveData<GolosNotifications>
    @MainThread
    fun setUp()

    @MainThread
    fun onReceivePushNotifications(notifications: Event)

    @MainThread
    fun dismissNotification(notification: GolosNotification)


    @MainThread
    fun dismissAllNotifications()
}

/**
 * Created by yuri on 05.03.18.
 */
var isPushesSubscribed = false

internal class PushNotificationsRepositoryImpl(private val appUserDataProvider: UserDataProvider,
                                               private val pushService: GolosPushService,
                                               private val deviceIdProvider: DeviceIdProvider,
                                               private val fcmTokenProvider: FCMTokenProvider) : PushNotificationsRepository {
    private val mNotifications = MutableLiveData<GolosNotifications>()
    private val mFilteredNotifications = MutableLiveData<GolosNotifications>()
    private var mLastSubscribedToken: String? = null


    @MainThread
    override fun setUp() {
        pushService.addLogoutListener(object : ServiceLogoutListener {
            override fun onLogout() {
                try {
                    pushService.unsubscribeFromPushNotificationsSync(mLastSubscribedToken
                            ?: return, deviceIdProvider.getDeviceId())
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        })
        mNotifications.observeForever {
            val new = GolosNotifications(getFiteredNotificaitons())
            if (new != mFilteredNotifications.value) mFilteredNotifications.value = new
        }

        pushService.loadingState.observeForever {
            if (it == PreparingState.DONE) onAuthOrTokenChange()
        }

        appUserDataProvider.appUserData.observeForever {
            onAuthOrTokenChange()

        }
        fcmTokenProvider.tokenLiveData.observeForever {
            onAuthOrTokenChange()
        }
    }

    fun onAuthOrTokenChange() {
        val currentUserData = appUserDataProvider.appUserData.value
        val currentFcmToken = fcmTokenProvider.tokenLiveData.value ?: return
        if (pushService.loadingState.value != PreparingState.DONE) return
        if (currentUserData != null) {
            if (currentFcmToken.newToken == mLastSubscribedToken) return
            pushService.subscribeOnPushNotifications(currentFcmToken.newToken, deviceIdProvider.getDeviceId())
            isPushesSubscribed = true
            mLastSubscribedToken = currentFcmToken.newToken
        }
    }

    @MainThread
    override fun onReceivePushNotifications(notifications: Event) {
        val new = GolosNotification.fromEvent(notifications)
        if (mNotifications.value?.notifications.orEmpty().contains(new)) return
        if (!Repository.get.isUserLoggedIn()) return
        val newNotifications = listOf(new) + mNotifications.value?.notifications.orEmpty()
        mNotifications.value = GolosNotifications(newNotifications)
    }

    @MainThread
    override fun dismissNotification(notification: GolosNotification) {
        mNotifications.value = GolosNotifications(mNotifications.value?.notifications?.filter { it != notification }
                ?: listOf())
    }


    override fun dismissAllNotifications() {
        mNotifications.value = GolosNotifications(listOf())
    }

    override val notifications: LiveData<GolosNotifications> = mFilteredNotifications

    private fun getFiteredNotificaitons(): List<GolosNotification> {
        return mNotifications.value?.notifications.orEmpty()
//                ?.filter {
//            val settings = settingsRepository.getNotificationsSettings().value
//            when (it) {
//                is GolosUpVoteNotification -> settings?.vote ?: true
//                is GolosTransferNotification -> settings?.transfer ?: true
//                is GolosCommentNotification -> settings?.reply ?: true
//                is GolosDownVoteNotification -> settings?.flag ?: true
//                is GolosSubscribeNotification -> settings?.subscribe ?: true
//                is GolosUnSubscribeNotification -> settings?.unsubscribe ?: true
//                is GolosMentionNotification -> settings?.mention ?: true
//                is GolosWitnessVoteNotification -> settings?.witnessVote ?: true
//                is WitnessCancelVoteGolosNotification -> settings?.witnessCancelVote ?: true
//                is GolosRepostNotification -> settings?.repost ?: true
//                is GolosRewardNotification -> settings?.reward ?: true
//                is GolosCuratorRewardNotification -> settings?.curatorReward ?: true
//            }
//
//        } ?: listOf()
    }

}