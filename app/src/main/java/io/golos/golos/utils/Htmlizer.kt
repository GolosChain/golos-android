package io.golos.golos.utils

/**
 * Created by yuri on 14.03.18.
 */
@FunctionalInterface
interface Htmlizer {
    fun toHtml(input: String): CharSequence
}