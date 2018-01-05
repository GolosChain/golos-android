package io.golos.golos.repository.model

import eu.bittrade.libs.steemj.base.models.TrendingTag

/**
 * Created by yuri on 05.01.18.
 */
data class Tag(val name: String,
               val payoutInGbg: Double,
               val votes: Long,
               val topPostsCount: Long) {
    constructor(tag: TrendingTag) : this(
            tag.name,
            tag.totalPayouts?.amount ?: 0.0,
            tag.netVotes,
            tag.topPosts)
}