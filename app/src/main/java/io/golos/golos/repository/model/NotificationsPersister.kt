package io.golos.golos.repository.model

interface NotificationsPersister {
    fun isUserSubscribedOnNotificationsThroughServices(): Boolean
    fun setUserSubscribedOnNotificationsThroughServices(isSubscribed: Boolean)
}