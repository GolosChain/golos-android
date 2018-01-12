package io.golos.golos.repository.model

import io.golos.golos.screens.stories.model.FeedType

/**
 * Created by yuri on 15.11.17.
 */
data class StoriesRequest(val limit: Int,
                          val type: FeedType,
                          val startAuthor: String?,
                          val startPermlink: String?,
                          val filter: StoryFilter? = null) : RepositoryRequests

interface RepositoryRequests