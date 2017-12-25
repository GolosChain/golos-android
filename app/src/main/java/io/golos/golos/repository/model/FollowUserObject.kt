package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.SubscribeStatus

/**
 * Created by yuri on 22.12.17.
 */
data class FollowUserObject(
        val name: String,
        var avatar: String?,
        var subscribeStatus: SubscribeStatus
)