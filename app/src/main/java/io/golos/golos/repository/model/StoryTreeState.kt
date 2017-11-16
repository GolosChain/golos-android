package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosError

/**
 * Created by yuri on 16.11.17.
 */
data class StoryTreeState(val treeState: StoryTree,
                          val error: GolosError? = null)
