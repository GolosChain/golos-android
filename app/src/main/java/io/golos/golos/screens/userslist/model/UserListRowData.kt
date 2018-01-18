package io.golos.golos.screens.userslist.model

import io.golos.golos.screens.story.model.SubscribeStatus

data class UserListRowData(val name: String,
                           var avatar: String?,
                           var shownValue: String?,
                           var subscribeStatus: SubscribeStatus)