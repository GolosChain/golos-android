package io.golos.golos.screens.story.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.stories.model.FeedType

/**
 * Created by yuri on 16.01.18.
 */
data class StoryDescriptor(
        @JsonProperty("author")
        val author: String,
        @JsonProperty("blog")
        val blog: String?,
        @JsonProperty("permlink")
        val permlink: String,
        @JsonProperty("feedType")
        val feedType: FeedType,
        @JsonProperty("filter")
        val filter: StoryFilter?)