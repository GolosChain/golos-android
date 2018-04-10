package io.golos.golos.repository.persistence.model

/**
 * Created by yuri on 06.11.17.
 */
data class UserAvatar(val userName: String,
                      val avatarPath: String?,
                      val dateUpdated: Long)