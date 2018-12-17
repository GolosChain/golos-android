package io.golos.golos.repository.api

/**
 * Created by yuri yurivladdurain@gmail.com on 14/12/2018.
 */
data class DiscussionQueryData (val author: String,
                                val permlink: String,
                                val voteLimit: Int? = null)