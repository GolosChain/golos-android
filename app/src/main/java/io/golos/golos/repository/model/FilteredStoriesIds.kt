package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by yuri on 25.01.18.
 */
data class FilteredStoriesIds(
        @JsonProperty("request")
        val request: StoryRequest,
        @JsonProperty("storyIds")
        val storyIds: List<Long>)