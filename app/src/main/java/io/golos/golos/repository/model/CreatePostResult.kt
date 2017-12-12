package io.golos.golos.repository.model

/**
 * Created by yuri on 12.12.17.
 */
data class CreatePostResult(var isPost: Boolean,
                       val author: String,
                       val blog: String,
                       val permlink: String)
