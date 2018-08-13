package io.golos.golos.notifications

import android.os.Handler
import android.os.Looper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.golos.golos.model.Notification
import io.golos.golos.repository.Repository
import io.golos.golos.utils.mapper
import timber.log.Timber


/**
 * Created by yuri on 05.03.18.

 */
class MyFireBaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        try {
            Timber.e("on new notification $p0")
            Timber.e("data is ${p0?.data}")
            Timber.e("notification is   ${p0?.notification}")
            Timber.e("notification title is   ${p0?.notification?.title}")
            Timber.e("notification title is   ${p0?.notification?.body}")

            val notification = mapper.readValue<Notification>(p0?.data?.get("message")
                    ?: return, Notification::class.java)
            Handler(Looper.getMainLooper()).post {
                Repository.get.notificationsRepository.onReceiveNotifications(listOf(notification))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e)
            try {
                val notifsListType = mapper.typeFactory.constructCollectionType(List::class.java, NotificationNew::class.java)
                val notification = mapper.readValue<List<NotificationNew>>(p0?.data?.get("message")
                        ?: return, notifsListType)
                Handler(Looper.getMainLooper()).post {
                    Repository.get.notificationsRepository.onReceiveNotificationsNew(notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e(e)
            }
        }

    }

    override fun onMessageSent(p0: String?) {
        super.onMessageSent(p0)
        Timber.e("onMessageSent")
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Timber.e("onDeletedMessages")

    }
}
