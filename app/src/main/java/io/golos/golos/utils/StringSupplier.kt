package io.golos.golos.utils;

/**
 * Created by yuri yurivladdurain@gmail.com on 28/10/2017.
 */
interface StringSupplier {
    fun get(resId: Int, args: String? = null): String
}