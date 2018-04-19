package io.golos.golos.repository.model

import com.google.firebase.messaging.FirebaseMessaging

object FCMTopicSubscriber: NotificationTopicSubscription {
    override fun subscribeOn(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
    }

    override fun unsubscribeOf(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
    }
}