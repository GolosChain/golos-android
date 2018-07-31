package io.golos.golos.utils

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import timber.log.Timber

public class OneShotLiveData<T>() : MutableLiveData<T>() {

    override fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        super.observe(owner, Observer {
            Timber.e("new value = $it")
            if (it == null) return@Observer
            observer.onChanged(it)
            value = null
        })
    }
}