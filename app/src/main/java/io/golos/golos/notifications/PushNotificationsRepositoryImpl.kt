package io.golos.golos.notifications

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.MainThread
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.utils.toArrayList

interface PushNotificationsRepository {
    val notifications: LiveData<GolosNotifications>
    @MainThread
    fun setUp()

    @MainThread
    fun onReceivePushNotifications(notifications: List<Notification>)

    @MainThread
    fun dismissNotification(notification: GolosNotification)


    @MainThread
    fun dismissAllNotifications()

}

/**
 * Created by yuri on 05.03.18.
 */


internal class PushNotificationsRepositoryImpl(private val settingsRepository: UserSettingsRepository
                                               = Repository.get.userSettingsRepository) : PushNotificationsRepository {
    private val mNotifications = MutableLiveData<GolosNotifications>()
    private val mFilteredNotifications = MutableLiveData<GolosNotifications>()


    @MainThread
    override fun setUp() {
        mNotifications.observeForever {
            val new = GolosNotifications(getFiteredNotificaitons())
            if (new != mFilteredNotifications.value) mFilteredNotifications.value = new

        }
        settingsRepository.getNotificationsSettings().observeForever {
            val new = GolosNotifications(getFiteredNotificaitons())
            if (new != mFilteredNotifications.value) mFilteredNotifications.value = new
        }
    }

    @MainThread
    override fun onReceivePushNotifications(notifications: List<Notification>) {
        val newNotifications = ArrayList(notifications.map { GolosNotification.fromNotification(it) }) + (mNotifications.value?.notifications
                ?: listOf()).toArrayList()
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
        return mNotifications.value?.notifications?.filter {
            val settings = settingsRepository.getNotificationsSettings().value
            when (it) {
                is GolosUpVoteNotification -> settings?.showUpvoteNotifs ?: true
                is GolosTransferNotification -> settings?.showTransferNotifs ?: true
                is GolosCommentNotification -> settings?.showNewCommentNotifs ?: true
                is GolosDownVoteNotification -> settings?.showFlagNotifs ?: true
                is GolosSubscribeNotification -> settings?.showSubscribeNotifs ?: true
                is GolosUnSubscribeNotification -> settings?.showUnSubscribeNotifs ?: true
                is GolosMentionNotification -> settings?.showMentions ?: true
                is GolosWitnessVoteNotification -> settings?.showWitnessVote ?: true
                is WitnessCancelVoteGolosNotification -> settings?.showWitnessCancelVote ?: true
                is GolosRepostNotification -> settings?.showReblog ?: true
            }

        } ?: listOf()
    }

}