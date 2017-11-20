package io.golos.golos.repository.api

import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.StoryTree

/**
 * Created by yuri on 20.11.17.
 */
abstract class GolosApi {

    companion object {
        private var instance: GolosApi? = null
        val get: GolosApi
            @Synchronized
            get() {
                if (instance == null) instance = MockApiImpl()
                return instance!!
            }
    }

    abstract fun getUserAvatar(username: String, permlink: String?, blog: String?): String?

    abstract fun getUserFeed(userName: String, limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<StoryTree>

    abstract fun getStory(blog: String, author: String, permlink: String): StoryTree

    abstract fun authWithMasterKey(userName: String, masterKey: String): UserAuthResponse

    abstract fun  getStories(limit: Int, type: FeedType, truncateBody: Int,
                            startAuthor: String?, startPermlink: String?): List<StoryTree>
}