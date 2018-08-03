package io.golos.golos.repository.model

interface NotificationsPersister {
    fun saveSubscribedOnTopic(topic: String?)
    fun isUserSubscribedOnNotificationsThroughServices(): Boolean
    fun setUserSubscribedOnNotificationsThroughServices(isSubscribed: Boolean)
    fun getSubscribeOnTopic(): String?
}