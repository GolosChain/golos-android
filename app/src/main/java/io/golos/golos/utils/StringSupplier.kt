package io.golos.golos.utils

import android.support.annotation.StringRes

/**
 * Created by yuri yurivladdurain@gmail.com on 28/10/2017.
 */
interface StringSupplier {
    fun get(@StringRes id: Int): String
}