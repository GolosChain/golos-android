package io.golos.golos.repository.model

import eu.bittrade.libs.steemj.base.models.error.SteemError
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.Comment
import io.golos.golos.utils.GolosError

data class StoryItems(val items: List<StoryItemState>,
                              val type: FeedType,
                              val error: GolosError? = null)

data class StoryItemState(val comment: Comment,
                          val state: UpdatingState,
                          val error: SteemError? = null,
                          val type: FeedType? = null)

enum class UpdatingState {
    UPDATING, DONE, FAILED
}