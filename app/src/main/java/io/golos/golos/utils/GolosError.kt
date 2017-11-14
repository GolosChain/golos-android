package io.golos.golos.utils

/**
 * Created by yuri on 14.11.17.
 */
data class GolosError(val errorCode: ErrorCode,
                      val nativeMessage:String?,
                      val localizedMessage: Int?)