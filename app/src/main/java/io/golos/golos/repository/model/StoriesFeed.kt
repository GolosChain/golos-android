package io.golos.golos.repository.model

import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.GolosError

data class StoriesFeed(val items: List<StoryWithComments>,
                       val type: FeedType,
                       val filter: StoryFilter?,
                       var isFeedActual: Boolean = true)

