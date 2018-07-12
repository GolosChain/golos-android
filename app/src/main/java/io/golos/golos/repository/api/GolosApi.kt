package io.golos.golos.repository.api

import eu.bittrade.libs.golosj.apis.follow.model.FollowApiObject
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.GolosUser
import io.golos.golos.repository.persistence.model.GolosUserAccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import java.io.File


abstract class GolosApi {

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


    abstract fun getUserAvatars(names: List<String>): Map<String, String?>

    abstract fun getStory(blog: String,
                          author: String,
                          permlink: String, accountDataHandler: (List<GolosUserAccountInfo>) -> Unit = { _ -> }): StoryWithComments

    abstract fun getStoryWithoutComments(author: String, permlink: String): StoryWithComments

    abstract fun getStories(limit: Int, type: FeedType, truncateBody: Int,
                            filter: StoryFilter? = null, startAuthor: String?, startPermlink: String?): List<StoryWithComments>

    abstract fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse

    abstract fun getAccountInfo(of: String): GolosUserAccountInfo

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

    abstract fun getSubscriptions(forUser: String, startFrom: String?): List<FollowApiObject>

    abstract fun getSubscribers(forUser: String, startFrom: String?): List<FollowApiObject>

    abstract fun follow(user: String)

    abstract fun unfollow(user: String)

    // empty startForSubscribersOrSubscriptions from to startForSubscribersOrSubscriptions from beginning, maxCount 1 - 1000
    abstract fun getTrendingTag(startFrom: String, maxCount: Int): List<Tag>

    abstract fun getGolosUsers(nick: String): List<GolosUser>
}