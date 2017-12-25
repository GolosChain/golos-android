package io.golos.golos.repository.model

import io.golos.golos.repository.StoryFilter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosError

data class StoryTreeItems(val items: List<StoryTree>,
                          val type: FeedType,
                          val filter: StoryFilter?,
                          val error: GolosError? = null)

