package io.golos.golos.screens.story.model

import eu.bittrade.libs.steemj.base.models.DiscussionWithComments
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 06.11.17.
 */

data class StoryViewState(val isLoading: Boolean = false,
                          val storyTitle: String = "",
                          val isStoryCommentButtonShown: Boolean = false,
                          val errorCode: GolosError? = null,
                          val tags: List<String> = emptyList(),
                          val storyTree: StoryTree = StoryTree(discussionWithComments = DiscussionWithComments()),
                          val canUserMakeBlogSubscriptionActions: Boolean = false,
                          val subscribeOnStoryAuthorStatus: SubscribeStatus,
                          val subscribeOnTagStatus: SubscribeStatus)
