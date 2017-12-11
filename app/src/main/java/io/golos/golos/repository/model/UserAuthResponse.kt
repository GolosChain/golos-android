package io.golos.golos.repository.model

import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 09.11.17.
 */
data class UserAuthResponse(val isKeyValid: Boolean,
                            val userName: String?,
                            val userMotto: String? = null,
                            val postingAuth: Pair<String, String?>? = null,
                            val activeAuth: Pair<String, String?>? = null,
                            val avatarPath: String? = null,
                            val postsCount: Long = 0,
                            val accountWorth: Double = 0.0,
                            val subscibesCount: Long = 0,
                            val subscribersCount: Long = 0,
                            val gbgAmount: Double = 0.0,
                            val golosAmount: Double = 0.0,
                            val golosPower: Double = 0.0,
                            val safeGbg:Double = 0.0,
                            val safeGolos:Double = 0.0,
                            val error: GolosError? = null)