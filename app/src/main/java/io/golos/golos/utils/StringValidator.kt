package io.golos.golos.utils

/**
 * Created by yuri yurivladdurain@gmail.com on 27/10/2017.
 *
 */
interface StringValidator {
    /***
     * @return tuple, where bool - is validation succesfull, or not, ans string - error message
     * */
    fun validate(input: String): Pair<Boolean, String>
}