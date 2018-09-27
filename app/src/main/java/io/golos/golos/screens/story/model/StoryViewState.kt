package io.golos.golos.screens.story.model

import eu.bittrade.libs.golosj.base.models.DiscussionWithComments
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 06.11.17.
 */
enum class DiscussionType {
    STORY, COMMENT
}

data class StoryViewState(val isLoading: Boolean = false,
                          val storyTitle: String = "",
                          val canUserCommentThis: Boolean = false,
                          val errorCode: GolosError? = null,
                          val discussionType: DiscussionType = DiscussionType.STORY,
                          val tags: MutableList<String> = arrayListOf(),
                          val storyTree: StoryWithComments = StoryWithComments(discussionWithComments = DiscussionWithComments()))

data class StorySubscriptionBlockState(val canUserMakeBlogSubscriptionActions: Boolean,
                                       val subscribeOnStoryAuthorStatus: SubscribeStatus,
                                       val subscribeOnTagStatus: SubscribeStatus)




