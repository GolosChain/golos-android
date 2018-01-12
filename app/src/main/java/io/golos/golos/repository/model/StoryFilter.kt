package io.golos.golos.repository.model

import com.fasterxml.jackson.annotation.JsonProperty

data class StoryFilter(
        @JsonProperty("tagFilter")
        val tagFilter: List<String> = arrayListOf(),
        @JsonProperty("userNameFilter")
        val userNameFilter: List<String> = arrayListOf()) {

    constructor(tagFilter: String? = null, userNameFilter: String? = null) : this(
            if (tagFilter != null) arrayListOf(tagFilter) else arrayListOf(),
            if (userNameFilter != null) arrayListOf(userNameFilter) else arrayListOf()
    )
}