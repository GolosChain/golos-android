package io.golos.golos.screens.stories.model

import io.golos.golos.screens.story.model.StoryWithComments

/**
 * Created by yuri on 20.02.18.
 */
@FunctionalInterface
interface StoryWithCommentsClickListener {
    fun onClick(story: StoryWithComments)

}