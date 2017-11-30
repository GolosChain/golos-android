package io.golos.golos.repository.model

/**
 * Created by yuri on 09.11.17.
 */
data class UserAuthResponse(val isKeyValid: Boolean,
                            val userName: String?,
                            val postingAuth: Pair<String, String?>?,
                            val activeAuth: Pair<String, String?>?,
                            val avatarPath: String?,
                            val postsCount: Long,
                            val accountWorth: Double)