package io.golos.golos.repository.model

interface NotificationTopicSubscription {
    fun subscribeOn(topic: String)

    fun unsubscribeOf(topic: String)
}