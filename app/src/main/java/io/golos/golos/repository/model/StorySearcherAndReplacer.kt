package io.golos.golos.repository.model

import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 17.11.17.
 */


data class ReplaceResult(val isChanged: Boolean, val resultingFeed: StoriesFeed)

class StorySearcherAndReplacer {
    fun findAndReplaceStory(replacer: GolosDiscussionItem, inList: StoriesFeed): ReplaceResult {
        var isChanged = false
        val itemsCopy = inList.items.toArrayList()

        if (replacer.isRootStory) {

            (0..itemsCopy.lastIndex)
                    .filter { itemsCopy[it].rootStory.id == replacer.id }
                    .forEach {

                        isChanged = true
                        itemsCopy[it] = StoryWithComments(replacer, itemsCopy[it].comments)
                    }


        } else {
            itemsCopy.forEach {
                isChanged = it.replaceComment(replacer)
            }
        }

        return ReplaceResult(isChanged, if (isChanged) inList.copy(items = itemsCopy) else inList)
    }

    fun findAndReplaceStory(newState: StoryWithComments, source: ArrayList<StoryWithComments>): Boolean {
        if (source.size == 0) return false
        var isChanged = false

        (0..source.lastIndex)
                .filter { i -> source[i].rootStory.id == newState.rootStory.id }
                .forEach { i ->
                    source[i] = newState
                    isChanged = true
                }
        if (!isChanged) {

            val root = newState.rootStory

            root.children.clear()
            root.children.addAll(newState.comments)
            source.forEachIndexed { index, storyWithComments ->
                val comments = storyWithComments.getFlataned()
                if (comments.find { it.id == root.id } != null) {
                    isChanged = true
                    storyWithComments.replaceComment(root)
                }
            }
        }
        return isChanged
    }
}