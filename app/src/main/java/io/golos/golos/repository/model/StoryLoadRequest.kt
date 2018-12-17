package io.golos.golos.repository.model

data class StoryLoadRequest(val author: String, val permlink: String, val blog: String?, val loadVotes: Boolean, val loadComents: Boolean)