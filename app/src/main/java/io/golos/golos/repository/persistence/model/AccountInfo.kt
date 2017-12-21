package io.golos.golos.repository.persistence.model

/**
 * Created by yuri on 13.11.17.
 */
data class AccountInfo(val userName: String?,
                       val userMotto: String? = null,
                       val avatarPath: String? = null,
                       val postsCount: Long = 0,
                       val accountWorth: Double = 0.0,
                       val subscibesCount: Long = 0,
                       var subscribersCount: Long = 0,
                       val gbgAmount: Double = 0.0,
                       val golosAmount: Double = 0.0,
                       val golosPower: Double = 0.0,
                       val safeGbg: Double = 0.0,
                       val safeGolos: Double = 0.0,
                       val postingPublicKey: String = "",
                       val activePublicKey: String = "",
                       var isCurrentUserSubscribed: Boolean = false)