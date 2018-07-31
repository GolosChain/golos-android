package io.golos.golos.repository

import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.utils.GolosError

fun StoriesFeed.setNewError(error: GolosError?): StoriesFeed {
    return StoriesFeed(items, type, filter, null, isFeedActual)
}