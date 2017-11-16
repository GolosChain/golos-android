package io.golos.golos.screens.main_stripes.viewmodel

import io.golos.golos.repository.Repository
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.RootStory

/**
 * Created by yuri on 14.11.17.
 */
interface StoriesRequestRouter {
    fun getStories(limit: Int, truncateBody: Int,
                   startAuthor: String? = null, startPermlink: String? = null): List<RootStory>
}

class StripeRouter(private val repository: Repository, private val stripeType: FeedType) : StoriesRequestRouter {
    override fun getStories(limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory> {
        return repository.getStripeItems(limit, stripeType, truncateBody, startAuthor, startPermlink)
    }
}


class FeedRouter(private val repository: Repository, private val userName: String) : StoriesRequestRouter {
    override fun getStories(limit: Int, truncateBody: Int, startAuthor: String?, startPermlink: String?): List<RootStory> {
        return repository.getUserFeed(userName, limit, truncateBody, startAuthor, startPermlink)
    }
}
