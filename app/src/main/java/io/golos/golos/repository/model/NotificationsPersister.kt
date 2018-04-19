package io.golos.golos.repository.model

interface NotificationsPersister {
    fun saveSubscribedOnTopic(topic: String?)
    fun getSubscribeOnTopic(): String?
}