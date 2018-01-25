package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.golos.golos.screens.stories.model.FeedType

data class StoryRequest(
        @JsonProperty("feedType")
        val feedType: FeedType,
        @JsonProperty("filter")
        val filter: StoryFilter?)