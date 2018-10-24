package io.golos.golos.screens.story.model

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
                          val error: GolosError? = null,
                          val discussionType: DiscussionType = DiscussionType.STORY,
                          val tags: List<String> = arrayListOf(),
                          val storyTree: StoryWrapperWithComment)

data class StorySubscriptionBlockState(val canUserMakeBlogSubscriptionActions: Boolean,
                                       val subscribeOnStoryAuthorStatus: SubscribeStatus,
                                       val subscribeOnTagStatus: SubscribeStatus)




