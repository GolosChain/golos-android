package io.golos.golos.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Created by yuri on 05.03.18.
 * AIzaSyAY0spUFCuG2oTYlyTjJrrUKg93A6B62aQ
 */
class MyFireBaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        Timber.e("onMessageReceived $p0")
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
