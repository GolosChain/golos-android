package io.golos.golos.repository.persistence.model

/**
 * Created by yuri on 13.11.17.
 */
data class AccountInfo(val userName: String,
                       val avatarPath: String? = null,
                       val accountWorth: Double,
                       val postsCount: Long)