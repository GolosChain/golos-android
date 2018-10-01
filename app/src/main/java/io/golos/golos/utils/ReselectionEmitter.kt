package io.golos.golos.utils

import androidx.lifecycle.LiveData

interface ReselectionEmitter {
    val reselectLiveData: LiveData<Int>
}