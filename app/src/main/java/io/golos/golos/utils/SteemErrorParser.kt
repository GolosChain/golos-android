package io.golos.golos.utils

import eu.bittrade.libs.steemj.exceptions.SteemResponseError
import io.golos.golos.R

/**
 * Created by yuri on 13.11.17.
 */
object SteemErrorParser {
    fun getLocalizedError(error: SteemResponseError): Int {
        if (error.error.steemErrorDetails.data.toString().contains(" Voter has used the maximum number of vote changes on this commen"))
            return R.string.user_used_max_comments_chances
        if (error.error.steemErrorDetails.data.toString().contains(" You have already voted in a similar way"))
            return R.string.you_voted_same_way
        else return R.string.unknown_error
    }
}