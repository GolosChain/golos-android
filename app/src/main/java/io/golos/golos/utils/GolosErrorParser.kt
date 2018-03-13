package io.golos.golos.utils

import eu.bittrade.libs.steemj.exceptions.*
import io.golos.golos.R
import timber.log.Timber
import java.security.InvalidParameterException

/**
 * Created by yuri on 13.11.17.
 */
object GolosErrorParser {
    fun parse(e: Exception): GolosError {
        e.printStackTrace()
        return when (e) {
            is SteemResponseError -> GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, GolosErrorParser.getLocalizedError(e))
            is SteemInvalidTransactionException -> GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, e.message, null)
            is SteemTimeoutException -> GolosError(ErrorCode.ERROR_SLOW_CONNECTION, null, R.string.slow_internet_connection)
            is SteemCommunicationException -> GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection)
            is InvalidParameterException -> {
                if (e.message?.contains("method without providing an account") == true) {
                    GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, R.string.reenter_acc)
                } else {
                    GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, R.string.wrong_args)
                }

            }

            is SteemConnectionException -> GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection)
            else -> GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.unknown_error)
        }
    }

    fun getLocalizedError(error: SteemResponseError): Int {
        Timber.e("error $error")
        if (error.message == null
                && error.error == null
                && error.error?.steemErrorDetails == null
                && error.error?.steemErrorDetails?.data == null) {
            return R.string.unknown_error
        }

        val message = error.error.steemErrorDetails.data.toString()
        if (error.message?.contains("Your reputation must be at least") == true)
            return R.string.you_must_have_more_repo_for_action
        if (message.contains("Cannot vote again on a comment after payout"))
            return R.string.cant_vote_after_payout
        if (message.contains(" Voter has used the maximum number of vote changes on this commen"))
            return R.string.user_used_max_comments_chances
        if (message.contains(" You have already voted in a similar way"))
            return R.string.you_voted_same_way
        if (message.contains("You may only comment once every 20 seconds"))
            return R.string.you_may_comment_only_tw_sec
        else if (error.error.steemErrorDetails.message.contains("You may only post once every 5 minutes"))
            return R.string.you_can_post_only_every_five_minutes
        else if (error.error.steemErrorDetails.message.contains(" <= now + fc::seconds(STEEMIT_MAX_TIME_UNTIL_EXPIRATION): ")
        ||error.error.steemErrorDetails.message.contains("now < trx.expiration"))
            return R.string.wrong_time
        else if (error.error.steemErrorDetails.message.contains("= STEEMIT_MIN_VOTE_INTERVAL_SEC: Can only vote once every "))
            return R.string.can_vote_once_per_three_second
        else return R.string.unknown_error
    }
}