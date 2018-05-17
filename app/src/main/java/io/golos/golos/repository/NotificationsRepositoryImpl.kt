package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.support.annotation.MainThread
import io.golos.golos.model.Notification
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.utils.toArrayList

interface NotificationsRepository {
    val notifications: LiveData<GolosNotifications>
    @MainThread
    fun setUp(context: Context)

    @MainThread
    fun onReceiveNotifications(notifications: List<Notification>)

    @MainThread
    fun dismissNotification(notification: GolosNotification)
}

/**
 * Created by yuri on 05.03.18.
 */


internal class NotificationsRepositoryImpl(private val mRepository: Repository,
                                           private val mNotificationTopicSubscription: NotificationTopicSubscription,
                                           private val mNotificationsPersister: NotificationsPersister) : NotificationsRepository {
    private val mNotifications = MutableLiveData<GolosNotifications>()
    private val mFilteredNotifications = MutableLiveData<GolosNotifications>()


    @MainThread
    override fun setUp(context: Context) {
        mRepository.getCurrentUserDataAsLiveData().observeForever(object : Observer<AppUserData> {
            override fun onChanged(t: AppUserData?) {
                val userName = mNotificationsPersister.getSubscribeOnTopic()
                if (t?.isUserLoggedIn == true) {
                    if (userName != null) return
                    mNotificationTopicSubscription.subscribeOn(t.userName ?: return)
                    mNotificationsPersister.saveSubscribedOnTopic(t.userName)
                } else if (t?.isUserLoggedIn == false) {
                    if (userName != null) {
                        mNotificationTopicSubscription.unsubscribeOf(userName)
                        mNotificationsPersister.saveSubscribedOnTopic(null)
                        mFilteredNotifications.value = null
                        mNotifications.value = null
                    }
                }
            }
        })
        mNotifications.observeForever {
            mFilteredNotifications.value = GolosNotifications(getFiteredNotificaitons())
        }
        Repository.get.userSettingsRepository.getNotificationsSettings().observeForever {
            mFilteredNotifications.value = GolosNotifications(getFiteredNotificaitons())
        }
    }


    @MainThread
    override fun onReceiveNotifications(notifications: List<Notification>) {
        if (!mRepository.isUserLoggedIn()) return
        val newNotifications = ArrayList(notifications.map { GolosNotification.fromNotification(it) }) + (mNotifications.value?.notifications
                ?: listOf()).toArrayList()
        mNotifications.value = GolosNotifications(newNotifications)
    }

    @MainThread
    override fun dismissNotification(notification: GolosNotification) {
        mNotifications.value = GolosNotifications(mNotifications.value?.notifications?.filter { it != notification }
                ?: listOf())
    }

    override val notifications: LiveData<GolosNotifications> = mFilteredNotifications

    private fun getFiteredNotificaitons(): List<GolosNotification> {
        return mNotifications.value?.notifications?.filter {
            val settings = mRepository.userSettingsRepository.getNotificationsSettings().value
            when (it) {
                is GolosUpVoteNotification -> settings?.showUpvoteNotifs ?: true
                is GolosTransferNotification -> settings?.showTransferNotifs ?: true
                is GolosCommentNotification -> settings?.showNewCommentNotifs ?: true
                is GolosDownVoteNotification -> settings?.showUpvoteNotifs ?: true
            }

        } ?: listOf()
    }

}