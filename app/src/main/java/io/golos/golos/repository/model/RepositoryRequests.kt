package io.golos.golos.repository.model

import io.golos.golos.screens.main_stripes.model.FeedType

/**
 * Created by yuri on 15.11.17.
 */
data class StoriesRequest(val limit: Int,
                          val type: FeedType,
                          val startAuthor: String?,
                          val startPermlink: String?) : RepositoryRequests

interface RepositoryRequests