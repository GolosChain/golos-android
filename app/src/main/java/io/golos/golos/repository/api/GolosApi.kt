package io.golos.golos.repository.api

import io.golos.golos.repository.GolosUsersApi
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import java.io.File


abstract class GolosApi : GolosUsersApi {

    companion object {
        private var instance: GolosApi? = null
        val get: GolosApi
            @Synchronized
            get() {
                if (instance == null) {
                    instance = ApiImpl()
                }
                return instance!!
            }
    }


    abstract fun getStory(blog: String,
                          author: String,
                          permlink: String,
                          voteLimit: Int? = null,
                          accountDataHandler: (List<GolosUserAccountInfo>) -> Unit = { _ -> }): StoryWithComments

    abstract fun getStoryWithoutComments(author: String, permlink: String, voteLimit: Int? = null): StoryWithComments

    abstract fun getStories(limit: Int, type: FeedType, truncateBody: Int,
                            filter: StoryFilter? = null, startAuthor: String?, startPermlink: String?): List<StoryWithComments>

    abstract fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse

    abstract fun cancelVote(author: String, permlink: String): GolosDiscussionItem

    abstract fun vote(author: String, permlink: String, percents: Short): GolosDiscussionItem

    abstract fun uploadImage(sendFromAccount: String, file: File): String

    abstract fun sendPost(sendFromAccount: String, title: String, content: String,
                          tags: Array<String>): CreatePostResult

    abstract fun editPost(originalComentPermlink: String, sendFromAccount: String, title: String,
                          content: String,
                          tags: Array<String>): CreatePostResult

    abstract fun sendComment(sendFromAccount: String,
                             authorOfItemToReply: String,
                             permlinkOfItemToReply: String,
                             content: String,
                             categoryName: String): CreatePostResult

    abstract fun editComment(sendFromAccount: String,
                             authorOfItemToReply: String,
                             permlinkOfItemToReply: String,
                             originalComentPermlink: String,
                             content: String,
                             categoryName: String): CreatePostResult

    // empty startForSubscribersOrSubscriptions from to startForSubscribersOrSubscriptions from beginning, maxCount 1 - 1000
    abstract fun getTrendingTag(startFrom: String, maxCount: Int): List<Tag>

}