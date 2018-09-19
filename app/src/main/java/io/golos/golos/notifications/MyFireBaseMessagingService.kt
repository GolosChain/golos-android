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

            Timber.i("data is ${p0?.data}")

            val notification = mapper.readValue<Notification>(p0?.data?.get("body")
                    ?: return, Notification::class.java)

            Handler(Looper.getMainLooper()).post {
                Repository.get.notificationsRepository.onReceivePushNotifications(listOf(notification))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e)
        }
    }
}
