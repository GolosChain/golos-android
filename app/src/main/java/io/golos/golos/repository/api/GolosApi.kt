package io.golos.golos.repository.api

import eu.bittrade.libs.steemj.apis.follow.model.FollowApiObject
import io.golos.golos.App
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.model.AccountInfo
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import java.io.File

/**
 * Created by yuri on 20.11.17.
 */
abstract class GolosApi {

    companion object {
        private var instance: GolosApi? = null
        val get: GolosApi
            @Synchronized
            get() {
                if (instance == null) {
                    if (App.isMocked) instance = MockApiImpl()
                    else instance = ApiImpl()
                }
                return instance!!
            }
    }

    abstract fun getUserAvatar(username: String, permlink: String?, blog: String?): String?

    abstract fun getUserAvatars(names: List<String>): Map<String, String?>

    abstract fun getUserFeed(userName: String, type: FeedType, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<StoryTree>

    abstract fun getStory(blog: String,
                          author: String,
                          permlink: String, accountDataHandler: (List<AccountInfo>) -> Unit = { _->}): StoryTree

    abstract fun getStoryWithoutComments(author: String, permlink: String): StoryTree

    abstract fun getStories(limit: Int, type: FeedType, truncateBody: Int,
                            filter: StoryFilter? = null, startAuthor: String?, startPermlink: String?): List<StoryTree>

    abstract fun auth(userName: String, masterKey: String?, activeWif: String?, postingWif: String?): UserAuthResponse

    abstract fun getAccountData(of: String): AccountInfo

    abstract fun cancelVote(author: String, permlink: String): GolosDiscussionItem

    abstract fun upVote(author: String, permlink: String, percents: Short): GolosDiscussionItem

    abstract fun uploadImage(sendFromAccount: String, file: File): String

    abstract fun sendPost(sendFromAccount: String, title: String, content: String,
                          tags: Array<String>): CreatePostResult

    abstract fun sendComment(sendFromAccount: String,
                             authorOfItemToReply: String,
                             permlinkOfItemToReply: String,
                             content: String,
                             categoryName: String): CreatePostResult

    abstract fun getSubscriptions(forUser: String, startFrom: String?): List<FollowApiObject>

    abstract fun follow(user:String)

    abstract fun unfollow(user:String)
}