package io.golos.golos.screens.stories.model

/**
 * Created by yuri yurivladdurain@gmail.com on 07/12/2018.
 */
enum class VoteChooserType {
    LIKE, DIZLIKE, CANCEL_VOTE
}

data class VoteChooserDescription(val storyId: Long, val type: VoteChooserType)