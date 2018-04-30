package io.golos.golos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.golos.golos.repository.Repository
import timber.log.Timber

const val NOTIFICATION_KEY = "NOTIFICATION_KEY"

class NotificationsBroadCastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(NOTIFICATION_KEY)) {
            val hashCode = intent.getIntExtra(NOTIFICATION_KEY, 0)
            val notification = Repository.get.notificationsRepository.notifications.value?.notifications?.find { it.hashCode() == hashCode }
            Repository.get.notificationsRepository.dismissNotification(notification ?: return)
        }
    }
}
