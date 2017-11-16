package io.golos.golos.utils

import eu.bittrade.libs.steemj.exceptions.*
import io.golos.golos.R
import java.security.InvalidParameterException

/**
 * Created by yuri on 13.11.17.
 */
object GolosErrorParser {
    fun parse(e: Exception): GolosError {
        return when (e) {
            is SteemResponseError -> GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, GolosErrorParser.getLocalizedError(e))
            is SteemInvalidTransactionException -> GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, e.message, null)
            is SteemTimeoutException -> GolosError(ErrorCode.ERROR_SLOW_CONNECTION, null, R.string.slow_internet_connection)
            is SteemCommunicationException -> GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection)
            is InvalidParameterException -> GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, R.string.wrong_args)
            is SteemConnectionException -> GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection)
            else -> GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.unknown_error)
        }
    }

    fun getLocalizedError(error: SteemResponseError): Int {
        if (error.error.steemErrorDetails.data.toString().contains(" Voter has used the maximum number of vote changes on this commen"))
            return R.string.user_used_max_comments_chances
        if (error.error.steemErrorDetails.data.toString().contains(" You have already voted in a similar way"))
            return R.string.you_voted_same_way
        else return R.string.unknown_error
    }
}