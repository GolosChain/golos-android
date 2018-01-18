package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.SubscribeStatus

/**
 * Created by yuri on 22.12.17.
 */
data class UserBlogSubscription(val user: UserObject,
                                var status: SubscribeStatus)

data class UserObject(
        val name: String,
        var avatar: String?
)