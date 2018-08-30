package io.golos.golos.repository.persistence.model

/**
 * Created by yuri on 13.11.17.
 */
data class GolosUserWithAvatar(val userName: String,
                               val avatarPath: String? = null) {
    fun setAvatar(avatarPath: String?): GolosUserWithAvatar {
        return if (this.avatarPath == avatarPath) return this
        else GolosUserWithAvatar(userName, avatarPath)
    }
}

data class GolosUserAccountInfo(val userName: String,
                                val avatarPath: String? = null,
                                val userMotto: String? = null,
                                val postsCount: Long = 0,
                                val accountWorth: Double = 0.0,
                                val gbgAmount: Double = 0.0,
                                val golosAmount: Double = 0.0,
                                val golosPower: Double = 0.0,
                                val safeGbg: Double = 0.0,
                                val safeGolos: Double = 0.0,
                                val subscribersCount: Int = 0,
                                val subscriptionsCount: Int = 0,
                                val postingPublicKey: String = "",
                                val activePublicKey: String = "",
                                val votingPower: Int = 0,
                                val location: String = "",
                                val website: String = "",
                                val registrationDate: Long = 0,
                                val userCover: String? = null,
                                val lastTimeInfoUpdatedAt: Long = 0)