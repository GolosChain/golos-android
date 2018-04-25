package io.golos.golos.repository

import android.arch.lifecycle.LiveData

enum class LifeCycleEvent {
    APP_CREATE, APP_DESTROY, APP_IN_FOREGROUND, APP_IN_BACKGROUND
}

interface AppLifecycleRepository {
    fun getLifeCycleLiveData(): LiveData<LifeCycleEvent>
}