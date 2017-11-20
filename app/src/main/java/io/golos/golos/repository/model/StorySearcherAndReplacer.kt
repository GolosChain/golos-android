package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryWrapper

/**
 * Created by yuri on 17.11.17.
 */
class StorySearcherAndReplacer {
    fun findAndReplace(replacer: StoryWrapper, inList: ArrayList<StoryTree>): Boolean {
        var isChanged = false
        if (inList.any({ it.rootStory()?.id == replacer.story.id })) {
            isChanged = true
            (0..inList.lastIndex)
                    .filter { inList[it].rootStory()?.id == replacer.story.id }
                    .forEach {
                        inList[it] = StoryTree(replacer, inList[it].commentsWithState())
                    }

        }
        inList.forEach {
            isChanged = it.replaceComment(replacer) || isChanged
        }
        return isChanged
    }

    fun findAndReplace(newState: StoryTree, source: ArrayList<StoryTree>): Boolean {
        var isChanged = false
        (0..source.lastIndex)
                .filter { i -> source[i].rootStory()?.id == newState.rootStory()?.id }
                .forEach { i ->
                    source[i] = newState
                    isChanged = true
                }

        return isChanged
    }

    private fun findAndReplace(newState: GolosDiscussionItem, source: ArrayList<GolosDiscussionItem>): ArrayList<GolosDiscussionItem> {
        (0..source.lastIndex)
                .filter { i -> source[i].id == newState.id }
                .forEach { i -> source[i] = newState }

        return ArrayList(source)
    }
}