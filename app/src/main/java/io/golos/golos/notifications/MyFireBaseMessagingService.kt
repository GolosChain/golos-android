package io.golos.golos.notifications

import android.os.Handler
import android.os.Looper
import com.google.firebase.messaging.FirebaseMessagingService1
import com.google.firebase.messaging.RemoteMessage
import io.golos.golos.repository.Repository
import io.golos.golos.utils.mapper
import timber.log.Timber


/**
 * Created by yuri on 05.03.18.

 */
class MyFireBaseMessagingService : FirebaseMessagingService1() {
    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        try {

            Timber.e("notification title is   ${p0?.notification?.title}")
            Timber.e("notification body is   ${p0?.notification?.body}")
            Timber.e("data is ${p0?.data}")


            val notification = mapper.readValue<NotificationNew>(p0?.data?.get("body")
                    ?: return, NotificationNew::class.java)

            Timber.e("notification = $notification")
            Handler(Looper.getMainLooper()).post {
                Repository.get.notificationsRepository.onReceiveNotifications(listOf(notification))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e)
        }
    }
}
