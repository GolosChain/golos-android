package io.golos.golos.utils

import android.arch.lifecycle.LiveData

interface ReselectionEmitter {
    val reselectLiveData: LiveData<Int>
}