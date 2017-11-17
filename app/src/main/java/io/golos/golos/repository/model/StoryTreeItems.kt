package io.golos.golos.repository.model

import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosError

data class StoryTreeItems(val items: List<StoryTree>,
                          val type: FeedType,
                          val error: GolosError? = null)

