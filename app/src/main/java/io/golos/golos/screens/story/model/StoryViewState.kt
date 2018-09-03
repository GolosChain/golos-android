package io.golos.golos.screens.story.model

import eu.bittrade.libs.golosj.base.models.DiscussionWithComments
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 06.11.17.
 */

data class StoryViewState(val isLoading: Boolean = false,
                          val storyTitle: String = "",
                          val isStoryCommentButtonShown: Boolean = false,
                          val errorCode: GolosError? = null,
                          val tags: MutableList<String> = arrayListOf(),
                          val storyTree: StoryWithComments = StoryWithComments(discussionWithComments = DiscussionWithComments()),
                          val canUserMakeBlogSubscriptionActions: Boolean = false,
                          val subscribeOnStoryAuthorStatus: SubscribeStatus,
                          val subscribeOnTagStatus: SubscribeStatus) {

    fun changeField(isLoading: Boolean = this.isLoading,
                    storyTitle: String = this.storyTitle,
                    isStoryCommentButtonShown: Boolean = this.isStoryCommentButtonShown,
                    errorCode: GolosError? = this.errorCode,
                    tags: MutableList<String> = this.tags,
                    storyTree: StoryWithComments = this.storyTree,
                    canUserMakeBlogSubscriptionActions: Boolean = this.canUserMakeBlogSubscriptionActions,
                    subscribeOnStoryAuthorStatus: SubscribeStatus = this.subscribeOnStoryAuthorStatus,
                    subscribeOnTagStatus: SubscribeStatus = this.subscribeOnTagStatus): StoryViewState {
        return StoryViewState(isLoading, storyTitle, isStoryCommentButtonShown, errorCode, tags, storyTree,
                canUserMakeBlogSubscriptionActions, subscribeOnStoryAuthorStatus, subscribeOnTagStatus)
    }
}


