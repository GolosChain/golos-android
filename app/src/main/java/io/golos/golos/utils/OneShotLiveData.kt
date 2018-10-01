package io.golos.golos.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

public class OneShotLiveData<T> : MutableLiveData<T>() {

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, Observer {
            if (it == null) return@Observer
            observer.onChanged(it)
            value = null
        })
    }
}